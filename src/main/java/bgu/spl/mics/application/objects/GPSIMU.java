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

    public GPSIMU(ArrayList<Pose> poses) {
        this.currentTick = 0;
        this.status = STATUS.UP;
        this.poses = poses;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(int currentTick) {
        this.currentTick = currentTick;
    }

    public ArrayList<Pose> getPoses() {
        return poses;
    }

    public Pose getPoseAtTick(int tick) {
        return poses.get(tick);
    }

    public void addPose(Pose pose) {
        poses.add(pose);
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public void clearPoses() {
        poses.clear();
    }

    public void increaseTick() {
        currentTick++;
    }
}
