package bgu.spl.mics.application.objects;

/**
 * Represents the robot's pose (position and orientation) in the environment.
 * Includes x, y coordinates and the yaw angle relative to a global coordinate
 * system.
 */
public class Pose {
    private float x; // the robot's x coordinate
    private float y; // the robot's y coordinate
    private float yaw; // the robot's yaw angle
    private int time; // the time at which the pose was reached

    public Pose(float x, float y, float yaw, int time) {
        this.x = x;
        this.y = y;
        this.yaw = yaw;
        this.time = time;
    }
}
