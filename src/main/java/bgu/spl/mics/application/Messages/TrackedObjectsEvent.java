package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackedObjectsEvent implements Event<Boolean> {
    private ConcurrentLinkedQueue<TrackedObject> trackedObjects;
    private int detectionTime;
    private int doneDetectedTick;
    private DetectObjectsEvent myEvent;

    public TrackedObjectsEvent(ConcurrentLinkedQueue<TrackedObject> trackedObjects, int detectionTime, int doneDetectedTick,
            DetectObjectsEvent detectedObjectEvent) {
        this.trackedObjects = trackedObjects;
        this.detectionTime = detectionTime;
        this.doneDetectedTick = doneDetectedTick;
        this.myEvent = detectedObjectEvent;

    }

    public ConcurrentLinkedQueue<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    }

    public int getDetectionTime() {
        return detectionTime;
    }

    public int getDoneDetectedTick() {
        return doneDetectedTick;
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