package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * Represents objects detected by the camera at a specific timestamp.
 * Includes the time of detection and a list of detected objects.
 */
public class StampedDetectedObjects {
    private int time; // the timestamp of the detected objects
    private ArrayList<DetectedObject> detectedObjects; // list of detected objects

    public StampedDetectedObjects(int time) {
        this.time = time;
        this.detectedObjects = new ArrayList<DetectedObject>();
    }

    public int getTime() {
        return time;
    }
}
