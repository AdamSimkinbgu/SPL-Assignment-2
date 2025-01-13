package bgu.spl.mics.application.objects;

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

    public StampedCloudPoints(int time, String id, ArrayList<CloudPoint> points) {
        this.id = id;
        this.time = time;
        this.points = points;
    }

    public String getID() {
        return id;
    }

    public int getTime() {
        return time;
    }

    public ArrayList<CloudPoint> getPoints() {
        return points;
    }

}
