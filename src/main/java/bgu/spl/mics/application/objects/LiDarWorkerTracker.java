package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.mics.application.Messages.DetectObjectsEvent;
import bgu.spl.mics.application.Messages.TrackedObjectsEvent;

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
    private int shouldTerminateAtTick;

    public LiDarWorkerTracker(int id, int frequency, String FilePath) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ConcurrentLinkedQueue<>();
        this.lidarDataBase = LiDarDataBase.getInstance(FilePath);
        this.trackedObjects = new ConcurrentHashMap<>();
        this.errorMsg = null;
        this.shouldTerminateAtTick = -1;
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

    public ConcurrentLinkedQueue<TrackedObject> calculateTrackedObjects(StampedDetectedObjects detectedObjects, int detectedTick) {
        // calculate tracked objects from detected objects
        ConcurrentLinkedQueue<TrackedObject> afterCalculateObjects = new ConcurrentLinkedQueue<>();
        ArrayList<DetectedObject> detectedObject = detectedObjects.getDetectedObjects();
        checkForError(detectedTick);
        if (getStatus() == STATUS.UP) {
            for (DetectedObject detected : detectedObject) {
                String detectedID = detected.getId();
                StampedCloudPoints stampedCloudPoints = lidarDataBase.getStampedCloudPoints(detectedTick, detectedID);
                if (stampedCloudPoints != null) {
                    ArrayList<CloudPoint> cloudPoints = new ArrayList<>();
                    for (List<Double> point : stampedCloudPoints.getPoints()) {
                        cloudPoints.add(new CloudPoint(point.get(0), point.get(1)));
                    }
                    TrackedObject trackedObject = new TrackedObject(detectedID, detectedTick, detected.getDescription(),
                            cloudPoints);
                    afterCalculateObjects.add(trackedObject);
                }
            }
            updateLastTrackedObjects(afterCalculateObjects);
        }
        trackedObjects.put(detectedTick, new ArrayList<>(afterCalculateObjects));
        return afterCalculateObjects;
    }

    private void checkForError(int detectedTick) {
        // check if the there was an error in detections of objects
        // getcloudpoints from base returns list of stampedcloudpoints
        List<StampedCloudPoints> stampedCloudPoints = lidarDataBase.getStampedCloudPoints();
        for (StampedCloudPoints stampedCloudPoint : stampedCloudPoints) {
            if (stampedCloudPoint.getTime() == detectedTick) {
                if (stampedCloudPoint.getID().equals("ERROR")) {
//                    setStatus(STATUS.ERROR);
                    setErrorMsg("Error in detected objects at time " + detectedTick);
                    setTerminateAtTick(detectedTick);
                }
            }
        }
    }

    private void setTerminateAtTick(int detectedTime) {
        // set the time to terminate the worker
        this.shouldTerminateAtTick = detectedTime + getFrequency();
    }

    public int getShouldTerminateAtTick() {
        return shouldTerminateAtTick;
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

    public void updateLastTrackedObjects(ConcurrentLinkedQueue<TrackedObject> trackedObjects) {
        lastTrackedObjects = trackedObjects;
    }

    public void workerCheckBeforeCrash(PriorityQueue<TrackedObjectsEvent> pendingqueue) {
        if (!pendingqueue.isEmpty()) {
            TrackedObjectsEvent toe = pendingqueue.poll();
            if (toe != null) {
                ConcurrentLinkedQueue<TrackedObject> lastTracked = toe.getTrackedObjects();
                updateLastTrackedObjects(lastTracked);
            }
        }
    }

    public List<TrackedObject> getTrackedObjectsByTime(int time) {
        for (int key : trackedObjects.keySet()) {
            System.out.println(trackedObjects.get(key));
        }
        return trackedObjects.get(time);
    }

    // public ConcurrentLinkedQueue<TrackedObject>
    // handleDetectObject(DetectObjectsEvent event, int detectionTime) {
    // ConcurrentLinkedQueue<TrackedObject> trackedObjects = new
    // ConcurrentLinkedQueue<>();
    //
    // // StampedDetectedObjects from the camera event
    // StampedDetectedObjects stampedObjects = event.getStampedDetectedObjects();
    //
    // // For each detected object in the camera’s list:
    // for (DetectedObject obj : stampedObjects.getDetectedObjects()) {
    // String objectId = obj.getId();
    // String objectDescription = obj.getDescription();
    //
    // // 1) Retrieve the matching cloud points from the LiDarDataBase
    // // for the same time + object ID
    // StampedCloudPoints stampedCP =
    // this.lidarDataBase.getStampedCloudPoints(detectionTime, objectId);
    // if (stampedCP == null) {
    // // Possibly the LiDAR DB had no entry for that (time, objectId)
    // continue;
    // }
    //
    // // 2) Convert the raw lists into CloudPoint objects
    // ArrayList<CloudPoint> coordinates = new ArrayList<>();
    // for (List<Double> listCP : stampedCP.getPoints()) {
    // Double x = listCP.get(0);
    // Double y = listCP.get(1);
    // // Possibly ignore z if you only do 2D?
    // coordinates.add(new CloudPoint(x, y));
    // }
    //
    // // 3) Create a new TrackedObject and add it to the queue
    // TrackedObject tracked = new TrackedObject(objectId, detectionTime,
    // objectDescription, coordinates);
    // trackedObjects.add(tracked);
    // }
    //
    // // 4) Update stats (like “number of tracked objects”)
    // //
    // StatisticalFolder.getInstance().increaseNumTrackedObjects(trackedObjects.size());
    //
    // // 5) Optionally store these as “lastTrackedObjects”
    // updateLastTrackedObjects(trackedObjects);
    //
    // return trackedObjects;
    // }
}
