package bgu.spl.mics.application.objects;

/**
 * CloudPoint represents a specific point in a 3D space as detected by the
 * LiDAR.
 * These points are used to generate a point cloud representing objects in the
 * environment.
 */
public class CloudPoint {
    private double x;
    private double y;

    public CloudPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CloudPoint)) {
            return false;
        }
        CloudPoint other = (CloudPoint) o;
        return other.x == x && other.y == y;
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }

}
