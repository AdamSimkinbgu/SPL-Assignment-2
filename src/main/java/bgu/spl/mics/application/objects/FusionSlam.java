package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Map<Integer, Pose> poses;
    private int tick;
    private AtomicInteger activeSensors;
    private AtomicInteger activeCameras;

    // correct implementation of the Singleton pattern according to the practical
    // class
    private FusionSlam() {
        this.landmarks = new ArrayList<LandMark>();
        this.poses = new HashMap<Integer, Pose>();
        this.tick = 0;
        this.activeSensors = new AtomicInteger(0);
        this.activeCameras = new AtomicInteger(0);
    }

    // Singleton instance holder
    private static class FusionSlamHolder {
        private static final FusionSlam instance = new FusionSlam();
    }

    public static FusionSlam getInstance() {
        return FusionSlamHolder.instance;
    }

    public synchronized void analyzeObjects(ArrayList<TrackedObject> trackedObjects) {
        for (TrackedObject trackedObj : trackedObjects) {
            Pose currPose = poses.get(trackedObj.getTime());
            String tObjID = trackedObj.getID();
            if (currPose == null) {
                System.out.println("No pose found for time " + trackedObj.getTime() + " for object " + tObjID);
                continue;
            }
            List<CloudPoint> points = trackedObj.getPoints();
            LandMark landmark = findLankmark(tObjID);
            if (landmark != null) {
                updateLandmarkCoordinates(landmark, points);
            } else {
                LandMark newLandmark = new LandMark(tObjID, trackedObj.getDescription(), points);
                landmarks.add(newLandmark);
                StatisticalFolder.getInstance().increaseNumLandmarks();
            }
        }
    }

    private void updateLandmarkCoordinates(LandMark landmark, List<CloudPoint> points) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateLandmarkCoordinates'");
    }

    private LandMark findLankmark(String tObjID) {
        return landmarks.stream().filter(lm -> lm.getID().equals(tObjID)).findFirst().orElse(null);
    }
}
