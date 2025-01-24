
package bgu.spl.mics.application.objects;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    private String outputFilePath = initializeOutputFile();
    private JsonObject camerasLastFrame = new JsonObject();
    private JsonObject lidarWorkerTrackersLastFrame = new JsonObject();
    private ConcurrentHashMap<Integer, JsonObject> poses = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, JsonObject> landmarks = new ConcurrentHashMap<>();
    private String errorMsg;
    private String faultySensor;
    private volatile boolean systemIsDone;
    private Gson prettyGson;
    private Gson regularGson;

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
        File file = new File(outputFilePath);
        file.delete();
        prettyGson = new GsonBuilder().setPrettyPrinting().create();
        regularGson = new Gson();
        systemIsDone = false;
        errorMsg = null;
        faultySensor = null;
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
        Path path = Paths.get("output_file_new_test.json");
        outputFilePath = path.toAbsolutePath().toString(); // Ensures absolute path for clarity

        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
                System.out.println("Created new file: " + outputFilePath);
            } catch (IOException e) {
                System.err.println("Failed to create file: " + outputFilePath);
                e.printStackTrace();
                // Handle the exception as per your application's requirements
            }
        } else {
            System.out.println("File already exists: " + outputFilePath);
        }
        return path.toAbsolutePath().toString();
    }

    private void checkIfOutputFileExists() {
        File file = new File(outputFilePath);
        if (file.exists()) {
            return;
        } else {
            try {
                if (file.createNewFile()) {
                    return;
                } else {
                    System.err.println("Failed to create the file.");
                }
            } catch (IOException e) {
                System.err.println("An error occurred while creating the file: " + e.getMessage());
            }
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
        JsonObject allCamerasLastFrames = camerasLastFrame == null ? new JsonObject() : camerasLastFrame;
        JsonObject lastCamerasFrame = allCamerasLastFrames.has("lastCamerasFrame")
                ? allCamerasLastFrames.getAsJsonObject("lastCamerasFrame")
                : new JsonObject();

        JsonObject currCamLastFrame = new JsonObject();
        currCamLastFrame.addProperty("time", time);
        JsonArray cameraLastDetectedObjectsArray = new JsonArray();
        ArrayList<DetectedObject> detectedObjectsList = cam.getLastDetectedObjects();
        for (DetectedObject detectedObject : detectedObjectsList) {
            JsonObject currentDetectedObject = new JsonObject();
            currentDetectedObject.addProperty("id", detectedObject.getId());
            currentDetectedObject.addProperty("description", detectedObject.getDescription());
            cameraLastDetectedObjectsArray.add(currentDetectedObject);
        }
        currCamLastFrame.add("detectedObjects", cameraLastDetectedObjectsArray);
        camerasLastFrame.add("Camera " + cam.getID(), currCamLastFrame);
    }

    public void updatelastLiDarWorkerTrackerFrame(int time, LiDarWorkerTracker lidar) {
        // check if data already exists in the sf class
        JsonObject currTrackedObject;
        for (TrackedObject trackedObject : lidar.getLastTrackedObject()) {
            currTrackedObject = new JsonObject();
            currTrackedObject.addProperty("id", trackedObject.getID());
            currTrackedObject.addProperty("time", time);
            currTrackedObject.addProperty("description", trackedObject.getDescription());
            JsonArray trackedObjectCoordinates = new JsonArray();
            for (CloudPoint cp : trackedObject.getPoints()) {
                JsonObject cpJson = new JsonObject();
                cpJson.addProperty("x", cp.getX());
                cpJson.addProperty("y", cp.getY());
                trackedObjectCoordinates.add(cpJson);
            }
            currTrackedObject.add("coordinates", trackedObjectCoordinates);
            lidarWorkerTrackersLastFrame.add("LiDarWorkerTracker " + lidar.getID(), currTrackedObject);
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
        checkIfOutputFileExists();
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
        output.add("lastCameraFrames", camerasLastFrame);
        output.add("lastLiDarFrames", lidarWorkerTrackersLastFrame);
        output.add("poses", prettyGson.toJsonTree(poses.values()));
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
}