package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of cloud points corresponding to a specific timestamp.
 * Used by the LiDAR system to store and process point cloud data for tracked
 * objects.
 */
public class StampedCloudPoints {
    private String id; // the ID of the object
    private int time; // the timestamp of the cloud points
    private List<List<Double>> cloudPoints;

    // Constructors
    public StampedCloudPoints() {
    }

    public StampedCloudPoints(int time, String id, List<List<Double>> cloudPoints) {
        this.time = time;
        this.id = id;
        this.cloudPoints = cloudPoints;
    }

    public String getID() {
        return id;
    }

    public int getTime() {
        return time;
    }

    public List<List<Double>> getPoints() {
        return cloudPoints;
    }

    @Override
    public String toString() {
        return "StampedCloudPoints{" +
                "id='" + id + '\'' +
                ", time=" + time +
                ", points=" + cloudPoints +
                '}';
    }
}
