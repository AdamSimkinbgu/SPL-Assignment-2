package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackedObjectsEvent implements Event<Boolean> {
    private ConcurrentLinkedQueue<TrackedObject> trackedObjects;
    private int detectionTime;
    private int doneDetectedAndTrackedTick;
    private DetectObjectsEvent myEvent;
    private int currTick;

    public TrackedObjectsEvent(ConcurrentLinkedQueue<TrackedObject> trackedObjects, int detectionTime, int doneDetectedAndTrackedTick,
            int currTick, DetectObjectsEvent detectedObjectEvent) {
        this.trackedObjects = trackedObjects;
        this.detectionTime = detectionTime;
        this.doneDetectedAndTrackedTick = doneDetectedAndTrackedTick;
        this.myEvent = detectedObjectEvent;
        this.currTick = currTick;
    }

    public ConcurrentLinkedQueue<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    }

    public int getCurrTick() {
        return currTick;
    }

    public int getDetectionTime() {
        return detectionTime;
    }

    public int getDoneDetectedAndTrackedTick() {
        return doneDetectedAndTrackedTick;
    }

    public DetectObjectsEvent getMyEvent() {
        return myEvent;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TrackedObject trackedObject : trackedObjects) {
            sb.append(trackedObject.getID() + " ");
        }
        return sb.toString();
    }

    public long getDetectedTick() {
        return detectionTime;
    }
}