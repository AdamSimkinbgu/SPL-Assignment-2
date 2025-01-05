package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * Represents an object tracked by the LiDAR.
 * This object includes information about the tracked object's ID, description,
 * time of tracking, and coordinates in the environment.
 */
public class TrackedObject {
    private String id; // the ID of the object
    private int time; // the time at which the object was tracked
    private String description; // the description of the object
    private ArrayList<CloudPoint> points; // list of coordinates of the object according to the charging station's
                                          // coordinate system

    public TrackedObject(String id, int time, String description) {
        this.id = id;
        this.time = time;
        this.description = description;
        this.points = new ArrayList<>();
    }
}
