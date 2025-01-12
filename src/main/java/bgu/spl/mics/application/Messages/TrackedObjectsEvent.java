package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.List;

public class TrackedObjectsEvent implements Event<Boolean> {
    private List<TrackedObject> trackedObjects;
    private int detectionTime;
    private int dueTime;



    public TrackedObjectsEvent(List<TrackedObject> trackedObjects, int detectionTime, int dueTime) {
        this.trackedObjects = trackedObjects;
        this.detectionTime = detectionTime;
        this.dueTime = dueTime;

    }

    public List<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    }

    public int getDetectionTime() {
        return detectionTime;
    }

    public int getDueTime() {
        return dueTime;
    }


}