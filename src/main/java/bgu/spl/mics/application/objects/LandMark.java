package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {
    private String id; // the internal of the object
    private String Description; // the description of the landmark
    private List<CloudPoint> points; // list of coordinates of the object according to the charging station's
                                     // coordinate system

    public LandMark(String id, String description) {
        this.id = id;
        Description = description;
        points = new ArrayList<CloudPoint>(); // why this cunsturctor made???
    }

    public LandMark(String id, String description, List<CloudPoint> points) {
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

    public List<CloudPoint> getPoints() {
        return points;
    }

    public void addPoint(CloudPoint point) {
        points.add(point);
    }

    public void clearPoints() {
        points.clear();
    }

    public void setCoordinates(List<CloudPoint> mergedPoints) {
        points = mergedPoints;
    }
}
