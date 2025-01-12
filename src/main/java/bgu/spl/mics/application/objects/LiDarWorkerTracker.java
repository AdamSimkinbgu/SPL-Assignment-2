package bgu.spl.mics.application.objects;

import java.util.ArrayList;

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

    public LiDarWorkerTracker(int id, int frequency) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ArrayList<TrackedObject>();
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
        int detectedtime = detectedObjects.getTime();
        for (DetectedObject detected : detectedObject) {
            String id = detected.getId();
            String description = detected.getDescription();
            TrackedObject trackedObject = new TrackedObject(id, detectedtime, description);
            afterCalculateObjects.add(trackedObject);
            // TODO not finished.

        }
        return afterCalculateObjects;

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

    public TrackedObject getLastTrackedObject() {
        return lastTrackedObjects.get(lastTrackedObjects.size() - 1);
    }

}
