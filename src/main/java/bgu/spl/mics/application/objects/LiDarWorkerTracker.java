package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

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
    private ArrayList<TrackedObject> lastTrackedObjects; // list of the last tracked objects
    private LiDarDataBase lidarDataBase; // the LiDAR database

    public LiDarWorkerTracker(int id, int frequency, String FilePath) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ArrayList<TrackedObject>();
        this.lidarDataBase = LiDarDataBase.getInstance(FilePath);
    }

    public int getID() {
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public ArrayList<TrackedObject> calculateTrackedObjects(StampedDetectedObjects detectedObjects) {
        // calculate tracked objects from detected objects
        ArrayList<TrackedObject> afterCalculateObjects = new ArrayList<>();
        ArrayList<DetectedObject> detectedObject = detectedObjects.getDetectedObjects();
        int detectedTime = detectedObjects.getTime();
        checkForError(detectedTime);
        if (getStatus() == STATUS.UP) {
            for (DetectedObject detected : detectedObject) {
                // detectobject event has detectorname(which camera detected), at what time it
                // was send and
                // object of stampeddetectedobjects
                // stampeddetectedobjects has time and arraylist of detectedobjects
                // detected object has id, description.
                String id = detected.getId();
                String description = detected.getDescription();
                TrackedObject trackedObject = new TrackedObject(id, detectedTime, description);
                afterCalculateObjects.add(trackedObject);
            }
        }
        return afterCalculateObjects;
    }

    private void checkForError(int detectedTime) {
        // check if the there was an error in detections of objects
        // getcloudpoints from base returns list of stampedcloudpoints
        List<StampedCloudPoints> stampedCloudPoints = lidarDataBase.getStampedCloudPoints();
        for (StampedCloudPoints stampedCloudPoint : stampedCloudPoints) {
            if (stampedCloudPoint.getTime() == detectedTime) {
                if (stampedCloudPoint.getID().equals("ERROR")) {
                    setStatus(STATUS.DOWN);
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

    public ArrayList<TrackedObject> getLastTrackedObject() {
        return lastTrackedObjects;
    }

    public void updateLastTrackedObjects(ArrayList<TrackedObject> trackedObjects) {
        lastTrackedObjects = trackedObjects;
    }
}
