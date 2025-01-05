package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {
    private String id; // the internal of the object
    private String Description; // the description of the landmark
    private ArrayList<CloudPoint> points; // list of coordinates of the object according to the charging station's
                                          // coordinate system

    public LandMark(String id, String description) {
        this.id = id;
        Description = description;
        points = new ArrayList<CloudPoint>();
    }
}
