package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {
    private int currentTick; // the current time tick
    private ArrayList<Pose> poses; // represents a list of time-stamped poses
    STATUS status; // the status of the GPSIMU system

    public GPSIMU() {
        this.currentTick = 0;
        this.status = STATUS.UP;
        this.poses = new ArrayList<Pose>();
    }
}
