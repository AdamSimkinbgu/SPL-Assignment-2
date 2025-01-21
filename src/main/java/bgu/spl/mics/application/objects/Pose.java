package bgu.spl.mics.application.objects;

import java.util.Objects;

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

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getYaw() {
        return yaw;
    }

    public int getTime() {
        return time;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Pose pose = (Pose) obj;
        return Float.compare(pose.x, x) == 0 &&
                Float.compare(pose.y, y) == 0 &&
                Float.compare(pose.yaw, yaw) == 0 &&
                time == pose.time;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, yaw, time);
    }

}
