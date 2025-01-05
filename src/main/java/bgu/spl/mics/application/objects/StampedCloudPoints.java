package bgu.spl.mics.application.objects;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Represents a group of cloud points corresponding to a specific timestamp.
 * Used by the LiDAR system to store and process point cloud data for tracked
 * objects.
 */
public class StampedCloudPoints {
    private String id; // the ID of the object
    private int time; // the timestamp of the cloud points
    private ArrayList<CloudPoint> points; // list of cloud points

    public StampedCloudPoints(String id, int time) {
        this.id = id;
        this.time = time;
        this.points = new ArrayList<CloudPoint>();
    }

}
