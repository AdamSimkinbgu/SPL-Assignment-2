package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping
 * (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update
 * a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam
 * exists.
 */
public class FusionSlam {
    private ArrayList<LandMark> landmarks;
    private ArrayList<Pose> poses;

    private FusionSlam() {
        this.landmarks = new ArrayList<LandMark>();
        this.poses = new ArrayList<Pose>();
    }

    // Singleton instance holder
    private static class FusionSlamHolder {
        private static final FusionSlam instance = new FusionSlam();
    }

    public static FusionSlam getInstance() {
        return FusionSlamHolder.instance;
    }
}
