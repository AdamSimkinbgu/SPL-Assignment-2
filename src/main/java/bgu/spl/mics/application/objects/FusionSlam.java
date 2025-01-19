package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private LandMark findLankmark(String tObjID) {
        return landmarks.stream().filter(lm -> lm.getID().equals(tObjID)).findFirst().orElse(null);
    }

    public ArrayList<LandMark> getLandmarks() {
        return landmarks;
    }

    public Map<Integer, Pose> getPoses() {
        return poses;
    }

    public void addPose(Pose pose) {
        poses.put(pose.getTime(), pose);
    }

    public int getTick() {
        return tick;
    }

    public void increaseSensor() {
        activeSensors.incrementAndGet();
    }

    public void decreaseSensor() {
        activeSensors.decrementAndGet();
    }

    public void setNumberOfActiveSensors(int numActiveSensors) {
        activeSensors.set(numActiveSensors);
    }

    public void increaseCameras() {
        activeCameras.incrementAndGet();
    }

    public void decreaseCameras() {
        activeCameras.decrementAndGet();
    }

    public void setNumberOfActiveCameras(int numActiveCameras) {
        activeCameras.set(numActiveCameras);
    }

    public void setTick(int tick) {
        this.tick = tick;
    }

    public int getNumberOfActiveSensors() {
        return activeSensors.get();
    }

    public void analyzeObjects(List<TrackedObject> trackedObjects) {
        for (TrackedObject trackedObj : trackedObjects) {
            Pose currPose = poses.get(trackedObj.getTime());
            String tObjID = trackedObj.getID();
            if (currPose == null) {
                System.out.println("No pose found for time " + trackedObj.getTime() + " for object " + tObjID);
                continue;
            }
            List<CloudPoint> points = convertToGlobalCoordinates(trackedObj.getPoints(), currPose);
            LandMark landmarkToUpdate = findLankmark(tObjID);
            if (landmarkToUpdate != null) {
                updateLandmarkCoordinates(landmarkToUpdate, points);
            } else {
                LandMark newLandmark = new LandMark(tObjID, trackedObj.getDescription(), points);
                landmarks.add(newLandmark);
                StatisticalFolder.getInstance().increaseNumLandmarks();
                StatisticalFolder.getInstance().updateLandmarks(landmarks);
            }
        }
    }

    private void updateLandmarkCoordinates(LandMark landmark, List<CloudPoint> points) {
        List<CloudPoint> currentPts = landmark.getPoints();
        if (currentPts.size() == points.size()) {
            List<CloudPoint> updatedPts = IntStream.range(0, currentPts.size())
                    .mapToObj(i -> new CloudPoint((currentPts.get(i).getX() + points.get(i).getX()) / 2.0,
                            (currentPts.get(i).getY() + points.get(i).getY()) / 2.0))
                    .collect(Collectors.toList());
            landmark.setCoordinates(updatedPts);
        } else {
            // if the sizes differe, handle integration of the new points
            integrateLandmarkCoordinates(landmark, points);
        }
    }

    private void integrateLandmarkCoordinates(LandMark landmark, List<CloudPoint> additionalPts) {
        List<CloudPoint> currentPts = landmark.getPoints();
        int minSize = Math.min(currentPts.size(), additionalPts.size());

        List<CloudPoint> integratedPts = IntStream.range(0, minSize)
                .mapToObj(i -> {
                    CloudPoint basePt = currentPts.get(i);
                    CloudPoint incomingPt = additionalPts.get(i);
                    return new CloudPoint(
                            basePt.getX() + (incomingPt.getX() - basePt.getX()) / 2.0,
                            basePt.getY() + (incomingPt.getY() - basePt.getY()) / 2.0);
                })
                .collect(Collectors.toList());

        if (additionalPts.size() > currentPts.size()) {
            integratedPts.addAll(additionalPts.subList(minSize, additionalPts.size()));
        } else if (currentPts.size() > additionalPts.size()) {
            integratedPts.addAll(currentPts.subList(minSize, currentPts.size()));
        }

        landmark.setCoordinates(integratedPts);
    }

    public List<CloudPoint> convertToGlobalCoordinates(List<CloudPoint> localPts, Pose robotPose) {
        List<CloudPoint> transformedPoints = new ArrayList<>();

        double angleRad = Math.toRadians(robotPose.getYaw());
        double cosine = Math.cos(angleRad);
        double sine = Math.sin(angleRad);

        for (CloudPoint localPt : localPts) {
            double xGlobal = (localPt.getX() * cosine) - (localPt.getY() * sine) + robotPose.getX();
            double yGlobal = (localPt.getX() * sine) + (localPt.getY() * cosine) + robotPose.getY();
            transformedPoints.add(new CloudPoint(xGlobal, yGlobal));
        }

        return transformedPoints;
    }

    public void setActiveCameras(int numActiveCameras) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setActiveCameras'");
    }
}
