package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {
    private String id; // the internal of the object
    private String Description; // the description of the landmark
    private ConcurrentLinkedQueue<CloudPoint> points; // list of coordinates of the object according to the charging
                                                      // station's
    // coordinate system

    public LandMark(String id, String description) {
        this.id = id;
        Description = description;
        points = new ConcurrentLinkedQueue<CloudPoint>(); // why this cunsturctor made???
    }

    public LandMark(String id, String description, ConcurrentLinkedQueue<CloudPoint> points) {
        this.id = id;
        Description = description;
        this.points = points;
    }

    public String getID() {
        return id;
    }

    public String getDescription() {
        return Description;
    }

    public List<List<Double>> getPoints() {
        List<List<Double>> pointsList = new ArrayList<>();
        for (CloudPoint point : points) {
            List<Double> pointList = new ArrayList<>();
            pointList.add(point.getX());
            pointList.add(point.getY());
            pointsList.add(pointList);
        }
        return pointsList;
    }

    public void addPoint(CloudPoint point) {
        points.add(point);
    }

    public void clearPoints() {
        points.clear();
    }

    public void setCoordinates(ConcurrentLinkedQueue<CloudPoint> mergedPoints) {
        points = mergedPoints;
    }

    public void addCoordinates(ConcurrentLinkedQueue<CloudPoint> convertedIntegratedPoints) {
        points.addAll(convertedIntegratedPoints);
    }
}
