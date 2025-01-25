package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.mics.application.Messages.DetectObjectsEvent;

/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using
 * data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSlam service.
 */
public class LiDarWorkerTracker {
    private int id; // the ID of the LiDAR worker
    private int frequency; // the frequency of the LiDAR worker
    private STATUS status;
    private ConcurrentLinkedQueue<TrackedObject> lastTrackedObjects; // list of the last tracked objects
    private volatile ConcurrentHashMap<Integer, List<TrackedObject>> trackedObjects; // list of tracked objects
    private LiDarDataBase lidarDataBase; // the LiDAR database
    private String errorMsg;


    public LiDarWorkerTracker(int id, int frequency, String FilePath) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ConcurrentLinkedQueue<>();
        this.lidarDataBase = LiDarDataBase.getInstance(FilePath);
        this.trackedObjects = new ConcurrentHashMap<>();
        this.errorMsg = null;
    }

    public int getID() {
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public ConcurrentLinkedQueue<TrackedObject> calculateTrackedObjects(StampedDetectedObjects detectedObjects) {
        // calculate tracked objects from detected objects
        ConcurrentLinkedQueue<TrackedObject> afterCalculateObjects = new ConcurrentLinkedQueue<>();
        ArrayList<DetectedObject> detectedObject = detectedObjects.getDetectedObjects();
        int detectedTime = detectedObjects.getTime();
        checkForError(detectedTime);
        if (getStatus() == STATUS.UP) {
            for (DetectedObject detected : detectedObject) {
                String detectedID = detected.getId();
                StampedCloudPoints stampedCloudPoints = lidarDataBase.getStampedCloudPoints(detectedTime, detectedID);
                if (stampedCloudPoints != null) {
                    ArrayList<CloudPoint> cloudPoints = new ArrayList<>();
                    for (List<Double> point : stampedCloudPoints.getPoints()) {
                        cloudPoints.add(new CloudPoint(point.get(0), point.get(1)));
                    }
                    TrackedObject trackedObject = new TrackedObject(detectedID, detectedTime, detected.getDescription(),
                            cloudPoints);
                    afterCalculateObjects.add(trackedObject);
                }
            }
        }
        trackedObjects.put(detectedTime, new ArrayList<>(afterCalculateObjects));
        return afterCalculateObjects;
    }

    private void checkForError(int detectedTime) {
        // check if the there was an error in detections of objects
        // getcloudpoints from base returns list of stampedcloudpoints
        List<StampedCloudPoints> stampedCloudPoints = lidarDataBase.getStampedCloudPoints();
        for (StampedCloudPoints stampedCloudPoint : stampedCloudPoints) {
            if (stampedCloudPoint.getTime() == detectedTime) {
                if (stampedCloudPoint.getID().equals("ERROR")) {
                    setStatus(STATUS.ERROR);
                    setErrorMsg("Error in detected objects at time " + detectedTime);
                }
            }
        }

    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public boolean didDetectAny() {
        return !lastTrackedObjects.isEmpty();
    }

    public ConcurrentLinkedQueue<TrackedObject> getLastTrackedObject() {
        return lastTrackedObjects;
    }

    public ConcurrentLinkedQueue<TrackedObject> processDetectedObjects(DetectObjectsEvent event, int detectionTime) {
        ConcurrentLinkedQueue<TrackedObject> trackedObjects = new ConcurrentLinkedQueue<>();

        // Retrieve detected objects from the event
        StampedDetectedObjects stampedObjects = event.getStampedDetectedObjects();

        // Check for errors before processing
        checkForError(detectionTime);

        if (getStatus() == STATUS.UP) {
            for (DetectedObject obj : stampedObjects.getDetectedObjects()) {
                String objectId = obj.getId();
                String objectDescription = obj.getDescription();

                // Retrieve cloud points for the detected object
                StampedCloudPoints stampedCP = this.lidarDataBase.getStampedCloudPoints(detectionTime, objectId);
                if (stampedCP == null) {
                    continue; // Skip if no cloud points are found
                }

                // Convert raw cloud points to CloudPoint objects
                ArrayList<CloudPoint> coordinates = new ArrayList<>();
                for (List<Double> point : stampedCP.getPoints()) {
                    Double x = point.get(0);
                    Double y = point.get(1);
                    coordinates.add(new CloudPoint(x, y));
                }

                // Create and add the TrackedObject to the queue
                TrackedObject tracked = new TrackedObject(objectId, detectionTime, objectDescription, coordinates);
                trackedObjects.add(tracked);
            }
        }

        // Update the last tracked objects
        updateLastTrackedObjects(trackedObjects);

        // Update StatisticalFolder with the new tracked objects
        StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(detectionTime, this);

        return trackedObjects;
    }

    public void updateLastTrackedObjects(ConcurrentLinkedQueue<TrackedObject> incomingTrackedObjects) {
        lastTrackedObjects = incomingTrackedObjects;
        if (incomingTrackedObjects.isEmpty()) {
            return;
        }
        ArrayList<TrackedObject> trackedObjectsList = incomingTrackedObjects.stream().collect(ArrayList::new,
                ArrayList::add,
                ArrayList::addAll);
        trackedObjects.put(trackedObjectsList.get(0).getTime(), trackedObjectsList);
    }

    public List<TrackedObject> getTrackedObjectsByTime(int time) {
        if (trackedObjects.containsKey(time)) {
            return trackedObjects.get(time);
        }
        return new ArrayList<>();
    }

    public void updateTrackedObjectsByTime(ConcurrentLinkedQueue<TrackedObject> trackedObjects2) {
        if (trackedObjects2.isEmpty()) {
            return;
        }
        List<TrackedObject> trackedObjectsList = new ArrayList<>(trackedObjects2);
        trackedObjects.put(trackedObjectsList.get(0).getTime(), trackedObjectsList);
    }
}
