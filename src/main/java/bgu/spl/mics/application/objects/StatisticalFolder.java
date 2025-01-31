
package bgu.spl.mics.application.objects;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks
 * identified.
 */
public class StatisticalFolder {
    private AtomicInteger systemRuntime;
    private AtomicInteger numDetectedObjects;
    private AtomicInteger numTrackedObjects;
    private AtomicInteger numLandmarks;
    private String outputFilePath;
    private JsonObject camerasLastFrame;
    private JsonObject lidarWorkerTrackersLastFrame = new JsonObject();
    private volatile ConcurrentHashMap<Integer, ConcurrentHashMap<String, List<DetectedObject>>> mapOfDetectedObjectsByTime = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<Integer, ConcurrentHashMap<String, List<TrackedObject>>> mapOfTrackedObjectsByTime = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> camerasLastTick = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> lidarWorkerTrackersLastTick = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, JsonObject> poses = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, JsonObject> landmarks = new ConcurrentHashMap<>();
    private String errorMsg;
    private String faultySensor;
    private volatile boolean systemIsDone;
    private Gson prettyGson;
    private Gson regularGson;
    private volatile boolean cameraError;

    public static StatisticalFolder getInstance() {
        return StatisticalFolderHolder.instance;
    }

    private static class StatisticalFolderHolder {
        private static final StatisticalFolder instance = new StatisticalFolder();
    }

    private StatisticalFolder() {
        systemRuntime = new AtomicInteger(0);
        numDetectedObjects = new AtomicInteger(0);
        numTrackedObjects = new AtomicInteger(0);
        numLandmarks = new AtomicInteger(0);
        prettyGson = new GsonBuilder().setPrettyPrinting().create();
        regularGson = new Gson();
        outputFilePath = null;
        systemIsDone = false;
        errorMsg = null;
        faultySensor = null;
        cameraError = false;
    }

    public int getSystemRuntime() {
        return systemRuntime.get();
    }

    public void increaseSystemRuntime() {
        systemRuntime.incrementAndGet();
    }

    public int getNumDetectedObjects() {
        return numDetectedObjects.get();
    }

    public int getNumTrackedObjects() {
        return numTrackedObjects.get();
    }

    public int getNumLandmarks() {
        return numLandmarks.get();
    }

    public boolean getSystemIsDone() {
        return systemIsDone;
    }

    public void setSystemIsDone(boolean isDone) {
        systemIsDone = isDone;
    }

    public void addNumDetectedObjects(int size) {
        numDetectedObjects.addAndGet(size);
    }

    public void addNumTrackedObjects(int size) {
        numTrackedObjects.addAndGet(size);
    }

    public void setNumLandmarks(int size) {
        numLandmarks.set(size);
    }

    public void setLastWorkTick(int time) {
        systemRuntime.set(time);
    }

    private String initializeOutputFile() {

        try {
            Path path = Paths.get(outputFilePath);
            Files.deleteIfExists(path.resolve("OutputError.json"));
            Files.deleteIfExists(path.resolve("output_file.json"));
            if (errorMsg != null) {
                Path errorFilePath = path.resolve("OutputError.json");
                Files.createFile(errorFilePath);
                return path.resolve("OutputError.json").toString();
            } else {
                Path outputFilePath = path.resolve("output_file.json");
                Files.createFile(outputFilePath);
                return path.resolve("output_file.json").toString();
            }
        } catch (IOException e) {
            System.err.println("An error occurred while initializing the output file: " + e.getMessage());
            return null;
        }

    }

    public synchronized void updateLandmarks(ArrayList<LandMark> landmarks) {
        for (LandMark landmark : landmarks) {
            JsonObject landmarkJsonObject = new JsonObject();
            landmarkJsonObject.addProperty("id", landmark.getID());
            landmarkJsonObject.addProperty("description", landmark.getDescription());
            JsonArray landmarkJsonCoordinates = new JsonArray();
            for (List<Double> cp : landmark.getPoints()) {
                JsonObject cpJson = new JsonObject();
                cpJson.addProperty("x", cp.get(0));
                cpJson.addProperty("y", cp.get(1));
                landmarkJsonCoordinates.add(cpJson);
            }
            landmarkJsonObject.add("coordinates", landmarkJsonCoordinates);
            this.landmarks.put(landmark.getID(), landmarkJsonObject);
        }
    }

    public void updateCamLastFrame(int time, Camera cam) {
        mapOfDetectedObjectsByTime.putIfAbsent(time, new ConcurrentHashMap<>());
        ConcurrentHashMap<String, List<DetectedObject>> currentCamMap = mapOfDetectedObjectsByTime.get(time);
        if (currentCamMap == null) {
            currentCamMap = new ConcurrentHashMap<>();
            mapOfDetectedObjectsByTime.put(time, currentCamMap);
        }
        List<DetectedObject> detectedObjects = cam.getDetectedObjects().get(time).getDetectedObjects();
        currentCamMap.put("Camera" + cam.getID(), detectedObjects);
        camerasLastTick.put("Camera" + cam.getID(), time);
        addNumDetectedObjects(detectedObjects.size());
    }

    private void createLastCameraFramesToJson() {
        List<String> cameraKeys = new ArrayList<>(camerasLastTick.keySet());
        JsonObject jsonOfAllCamerasEachWithAnArrayOfJsonObjects = new JsonObject();
        for (String cameraKey : cameraKeys) {
            JsonObject wholeCamJsonObject = new JsonObject();
            wholeCamJsonObject.addProperty("time", camerasLastTick.get(cameraKey));
            JsonArray cameraJson = new JsonArray();
            List<DetectedObject> listOfDetectedObjectByCurrCamera = mapOfDetectedObjectsByTime
                    .get(camerasLastTick.get(cameraKey))
                    .get(cameraKey);
            for (DetectedObject detectedObject : listOfDetectedObjectByCurrCamera) {
                JsonObject detectedObjectJson = new JsonObject();
                detectedObjectJson.addProperty("id", detectedObject.getId());
                detectedObjectJson.addProperty("description", detectedObject.getDescription());
                cameraJson.add(detectedObjectJson);
            }
            wholeCamJsonObject.add("detectedObjects", cameraJson);
            jsonOfAllCamerasEachWithAnArrayOfJsonObjects.add(cameraKey, wholeCamJsonObject);
        }
        camerasLastFrame = jsonOfAllCamerasEachWithAnArrayOfJsonObjects;
        for (String key : cameraKeys) {
            for (Integer time : mapOfDetectedObjectsByTime.keySet()) {
                if (mapOfDetectedObjectsByTime.get(time).containsKey(key)) {
                    for (DetectedObject detectedObject : mapOfDetectedObjectsByTime.get(time).get(key)) {
                        System.out.println("Time: " + time + " " + key + " " + detectedObject.getId());
                    }
                }
            }
        }
    }

    public void updatelastLiDarWorkerTrackerFrame(int time, LiDarWorkerTracker lidar) {
        ConcurrentHashMap<String, List<TrackedObject>> currentLiDarMap = mapOfTrackedObjectsByTime.get(time);
        if (currentLiDarMap == null) {
            currentLiDarMap = new ConcurrentHashMap<>();
            mapOfTrackedObjectsByTime.put(time, currentLiDarMap);
        }
        List<TrackedObject> trackedObjects = new ArrayList<>(lidar.getLastTrackedObject());
        currentLiDarMap.put("LiDarWorkerTracker" + lidar.getID(), trackedObjects);
        lidarWorkerTrackersLastTick.put("LiDarWorkerTracker" + lidar.getID(), time);
        addNumTrackedObjects(trackedObjects.size());
    }

    private void createLastLidarFramesToJson() {
        List<String> lidarWorkerTrackerKeys = new ArrayList<>(lidarWorkerTrackersLastTick.keySet());
        JsonObject jsonOfAllLidarsEachWithAnArrayOfJsonObjects = new JsonObject();
        for (String lidarWorkerTrackerKey : lidarWorkerTrackerKeys) {
            JsonArray lidarWorkerTrackerJson = new JsonArray();
            int timeLidarEnded = lidarWorkerTrackersLastTick.get(lidarWorkerTrackerKey);

            List<TrackedObject> listOfTrackedObjectByCurrLidar = mapOfTrackedObjectsByTime.get(timeLidarEnded)
                    .get(lidarWorkerTrackerKey);
            try {
                for (TrackedObject trackedObject : listOfTrackedObjectByCurrLidar) {
                    JsonObject trackedObjectJson = new JsonObject();
                    trackedObjectJson.addProperty("id", trackedObject.getID());
                    trackedObjectJson.addProperty("time", trackedObject.getTime());
                    trackedObjectJson.addProperty("description", trackedObject.getDescription());
                    JsonArray trackedObjectCoordinates = new JsonArray();
                    for (CloudPoint cloudPoint : trackedObject.getPoints()) {
                        JsonObject cloudPointJson = new JsonObject();
                        cloudPointJson.addProperty("x", cloudPoint.getX());
                        cloudPointJson.addProperty("y", cloudPoint.getY());
                        trackedObjectCoordinates.add(cloudPointJson);
                    }
                    trackedObjectJson.add("coordinates", trackedObjectCoordinates);
                    lidarWorkerTrackerJson.add(trackedObjectJson);
                }
            } catch (Exception e) {
                System.out.println("Error in creating the last lidar frame");
                jsonOfAllLidarsEachWithAnArrayOfJsonObjects.add(lidarWorkerTrackerKey, new JsonObject());
                return;
            }
            jsonOfAllLidarsEachWithAnArrayOfJsonObjects.add(lidarWorkerTrackerKey, lidarWorkerTrackerJson);
        }
        lidarWorkerTrackersLastFrame = jsonOfAllLidarsEachWithAnArrayOfJsonObjects;
        for (String key : lidarWorkerTrackerKeys) {
            for (Integer time : mapOfTrackedObjectsByTime.keySet()) {
                if (mapOfTrackedObjectsByTime.get(time).containsKey(key)) {
                    for (TrackedObject trackedObject : mapOfTrackedObjectsByTime.get(time).get(key)) {
                        System.out.println("Time: " + time + " " + key + " " + trackedObject.getID());
                    }
                }
            }
        }
    }

    public void updatePoses(ConcurrentHashMap<Integer, Pose> poses) {
        this.poses.clear();
        for (Integer key : poses.keySet()) {
            Pose pose = poses.get(key);
            JsonObject poseJson = new JsonObject();
            poseJson.addProperty("time", pose.getTime());
            poseJson.addProperty("x", pose.getX());
            poseJson.addProperty("y", pose.getY());
            poseJson.addProperty("yaw", pose.getYaw());
            this.poses.put(key, poseJson);
        }
    }

    public void updateError(String errorMsg, String faultySensor) {
        this.errorMsg = errorMsg;
        this.faultySensor = faultySensor;
    }

    public void createOutput() {
        outputFilePath = initializeOutputFile();
        createLastCameraFramesToJson();
        createLastLidarFramesToJson();
        JsonObject output = new JsonObject();
        createBaseOfOutput(output);
        if (errorMsg != null) {
            addErrorOutput(output);
        }
        writeOutputToFile(output);
    }

    private void createBaseOfOutput(JsonObject output) {
        JsonObject statistics;
        if (errorMsg == null) {
            output.addProperty("systemRuntime", systemRuntime.get());
            output.addProperty("numDetectedObjects", numDetectedObjects.get());
            output.addProperty("numTrackedObjects", numTrackedObjects.get());
            output.addProperty("numLandmarks", numLandmarks.get());
            JsonObject landmarkJsonObject = new JsonObject();
            for (JsonObject landmark : landmarks.values()) {
                landmarkJsonObject.add(landmark.get("id").getAsString(), landmark);
            }
            output.add("landMarks", landmarkJsonObject);
        } else {
            statistics = new JsonObject();
            statistics.addProperty("systemRuntime", systemRuntime.get());
            statistics.addProperty("numDetectedObjects", numDetectedObjects.get());
            statistics.addProperty("numTrackedObjects", numTrackedObjects.get());
            statistics.addProperty("numLandmarks", numLandmarks.get());
            JsonObject landmarkJsonObject = new JsonObject();
            for (JsonObject landmark : landmarks.values()) {
                landmarkJsonObject.add(landmark.get("id").getAsString(), landmark);
            }
            statistics.add("landMarks", landmarkJsonObject);
            output.add("statistics", statistics);
        }
    }

    private void addErrorOutput(JsonObject output) {
        output.addProperty("error", errorMsg);
        output.addProperty("faultySensor", faultySensor);
        output.add("lastCamerasFrame", camerasLastFrame);
        output.add("lastLiDarWorkerTrackersFrame", lidarWorkerTrackersLastFrame);
        List<JsonObject> posesToOutput = new ArrayList<>();
        for (Integer key : poses.keySet()) {
            if (key <= systemRuntime.get()) {
                posesToOutput.add(poses.get(key));
            }
        }
        output.add("poses", prettyGson.toJsonTree(posesToOutput));
    }

    private synchronized void writeOutputToFile(JsonObject output) {
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(output, writer);
            System.out.println("Output was created in " + outputFilePath);
        } catch (Exception e) {
            System.err.println("Failed to create the output file because of " + e.getMessage());
        }
    }

    public void decreaseCamNum() {
        numTrackedObjects.decrementAndGet();
    }

    public boolean isSystemDown() {
        return systemIsDone;
    }

    public void setCameraError(boolean cameraError) {
        this.cameraError = cameraError;
    }

    public boolean getCameraError() {
        return cameraError;
    }

    public void setConfigPath(String configPath) {
        outputFilePath = configPath;
    }
}