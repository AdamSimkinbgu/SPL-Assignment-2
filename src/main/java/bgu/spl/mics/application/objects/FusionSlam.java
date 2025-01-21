package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private ConcurrentHashMap<Integer, Pose> poses;
    private int tick;
    private AtomicInteger activeSensors;
    private AtomicInteger activeCameras;

    // correct implementation of the Singleton pattern according to the practical
    // class
    private FusionSlam() {
        this.landmarks = new ArrayList<LandMark>();
        this.poses = new ConcurrentHashMap<Integer, Pose>();
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

    public synchronized void addPose(Pose pose) {
        poses.putIfAbsent(pose.getTime(), pose);
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

    public Pose getPoseAtTime(int time) {
        Pose pose = poses.get(time);
        return pose;
    }

    public synchronized void analyzeObjects(List<TrackedObject> trackedObjects) {
        for (TrackedObject trackedObj : trackedObjects) {
            Pose currPose = poses.get(trackedObj.getTime());
            String tObjID = trackedObj.getID();
            if (currPose == null) {
                System.out.println(
                        "FusionSlam: No pose found for time " + trackedObj.getTime() + " for object " + tObjID);
                continue;
            }
            List<List<Double>> points = convertToGlobalCoordinates(trackedObj.getPoints(), currPose);
            System.out.println(
                    "FusionSlam: Converted " + points.size() + " local cloud points to global coordinates for object "
                            + tObjID + " at tick " + trackedObj.getTime());
            LandMark landmarkToUpdate = findLankmark(tObjID); // findLandmark(tObjID);
            if (landmarkToUpdate != null) {
                System.out.println("FusionSlam: Updating existing landmark " + tObjID);
                updateLandmarkCoordinates(landmarkToUpdate, points);
            } else {
                System.out.println("FusionSlam: Creating new landmark " + tObjID);
                ConcurrentLinkedQueue<CloudPoint> cloudPoints = new ConcurrentLinkedQueue<>();
                for (List<Double> pt : points) {
                    Double x = pt.get(0);
                    Double y = pt.get(1);
                    cloudPoints.add(new CloudPoint(x, y));
                }
                LandMark newLandmark = new LandMark(tObjID, trackedObj.getDescription(), cloudPoints);
                landmarks.add(newLandmark);
                StatisticalFolder.getInstance().increaseNumLandmarks();
                StatisticalFolder.getInstance().updateLandmarks(landmarks);
            }
        }
    }

    private synchronized void updateLandmarkCoordinates(LandMark landmark, List<List<Double>> points) {
        List<List<Double>> currentPts = landmark.getPoints();
        if (currentPts.size() == points.size()) {
            List<List<Double>> averagedPts = new ArrayList<>();
            for (int i = 0; i < currentPts.size(); i++) {
                List<Double> currentPt = currentPts.get(i);
                List<Double> incomingPt = points.get(i);
                double avgX = (currentPt.get(0) + incomingPt.get(0)) / 2.0;
                double avgY = (currentPt.get(1) + incomingPt.get(1)) / 2.0;
                List<Double> avgPt = new ArrayList<>();
                avgPt.add(avgX);
                avgPt.add(avgY);
                averagedPts.add(avgPt);
            }
            System.out.println(
                    "FusionSlam: Averaged " + points.size() + " cloud points for landmark " + landmark.getID());
        } else {
            System.out.println(
                    "FusionSlam: Integrating " + points.size() + " cloud points for landmark " + landmark.getID());
            integrateLandmarkCoordinates(landmark, points);
        }
    }

    private synchronized void integrateLandmarkCoordinates(LandMark landmark, List<List<Double>> additionalPts) {
        List<List<Double>> currentPts = landmark.getPoints();
        int minSize = Math.min(currentPts.size(), additionalPts.size());

        List<List<Double>> integratedPts = IntStream.range(0, minSize).mapToObj(i -> {
            List<Double> currentPt = currentPts.get(i);
            List<Double> incomingPt = additionalPts.get(i);
            double avgX = (currentPt.get(0) + incomingPt.get(0)) / 2.0;
            double avgY = (currentPt.get(1) + incomingPt.get(1)) / 2.0;
            List<Double> avgPt = new ArrayList<>();
            avgPt.add(avgX);
            avgPt.add(avgY);
            return avgPt;
        }).collect(Collectors.toList());

        if (additionalPts.size() > currentPts.size()) {
            integratedPts.addAll(additionalPts.subList(minSize, additionalPts.size()));
        } else if (currentPts.size() > additionalPts.size()) {
            integratedPts.addAll(currentPts.subList(minSize, currentPts.size()));
        }
        ConcurrentLinkedQueue<CloudPoint> ConvertedIntegratedPoints = new ConcurrentLinkedQueue<>();
        for (List<Double> pt : integratedPts) {
            ConvertedIntegratedPoints.add(new CloudPoint(pt.get(0), pt.get(1));
        }
        landmark.setCoordinates(ConvertedIntegratedPoints);
        System.out.println("FusionSlam: Integrated cloud points for landmark " + landmark.getID());
    }

    public synchronized List<List<Double>> convertToGlobalCoordinates(ArrayList<CloudPoint> localPts,
            Pose robotPose) {
        List<List<Double>> transformedPoints = new ArrayList<>();

        double angleRad = Math.toRadians(robotPose.getYaw());
        double cosine = Math.cos(angleRad);
        double sine = Math.sin(angleRad);

        for (CloudPoint pt : localPts) {
            double x = pt.getX();
            double y = pt.getY();
            double globalX = cosine * x - sine * y + robotPose.getX();
            double globalY = sine * x + cosine * y + robotPose.getY();
            List<Double> globalPt = new ArrayList<>();
            globalPt.add(globalX);
            globalPt.add(globalY);
            transformedPoints.add(globalPt);
        }

        System.out.println("FusionSlam: Converted " + localPts.size() + " local cloud points to global coordinates\n"
                + "FusionSlam: Robot pose: " + robotPose.getX() + ", " + robotPose.getY() + ", " + robotPose.getYaw()
                + "\n" + "FusionSlam: Transformation matrix: [" + cosine + ", " + -sine + ", " + robotPose.getX()
                + "], [" + sine + ", " + cosine + ", " + robotPose.getY() + "]\n" + "FusionSlam: Local points: "
                + localPts.size() + "\n" + "FusionSlam: Global points: " + transformedPoints.size());

        return transformedPoints;
    }

    public void setActiveCameras(int numActiveCameras) {
        activeCameras.set(numActiveCameras);
    }

    public void clearForDebug() {
        landmarks.clear();
        poses.clear();
        tick = 0;
        activeSensors.set(0);
        activeCameras.set(0);
    }

    public int getNumberOfActiveCameras() {
        return activeCameras.get();
    }
}
