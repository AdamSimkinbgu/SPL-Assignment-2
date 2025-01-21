package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {
    private int currentTick; // the current time tick
    private ConcurrentHashMap<Integer, Pose> poses; // represents a list of time-stamped poses
    STATUS status; // the status of the GPSIMU system

    public GPSIMU() {
        this.currentTick = 0;
        this.status = STATUS.UP;
        this.poses = new ConcurrentHashMap<Integer, Pose>();
    }

    public GPSIMU(ConcurrentHashMap<Integer, Pose> poses) {
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

    public ConcurrentHashMap<Integer, Pose> getPoses() {
        return poses;
    }

    public Pose getPoseAtTick(int tick) {
        return poses.get(tick);
    }

    public void addPose(Pose pose) {
        poses.put(pose.getTime(), pose);
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
