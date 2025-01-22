
package bgu.spl.mics.application.objects;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
        JsonObject lastFrameJson = new JsonObject();
        lastFrameJson.addProperty("time", time);
        // because we already know we are going to override the last frame, we can just
        // add the new data
        lastFrameJson.add("detectedObjects", prettyGson.toJsonTree(cam.getDetectedObjects(time)));
        currCamLastFrame.add("Camera" + cam.getID(), lastFrameJson);
        lastCamerasFrame.add("Camera" + cam.getID(), lastFrameJson);
        allCamerasLastFrames.add("lastCamerasFrame", lastCamerasFrame);
        camerasLastFrame = allCamerasLastFrames;
    }

    public void updatelastLiDarWorkerTrackerFrame(int time, LiDarWorkerTracker lidar) {
        JsonObject allLiDarWorkerTrackersLastFrames = lidarWorkerTrackersLastFrame == null ? new JsonObject()
                : lidarWorkerTrackersLastFrame;
        JsonObject lastLiDarWorkerTrackersFrames = allLiDarWorkerTrackersLastFrames.has("lastLiDarWorkerTrackersFrame")
                ? allLiDarWorkerTrackersLastFrames.getAsJsonObject("lastLiDarWorkerTrackerFrame")
                : new JsonObject();
        JsonObject currLiDarWorkerTrackerLastFrame = new JsonObject();
        JsonObject lastFrameJson = new JsonObject();
        lastFrameJson.addProperty("id", lidar.getID());
        lastFrameJson.addProperty("time", time);
        ConcurrentLinkedQueue<TrackedObject> trackedObjects = lidar.getLastTrackedObject();
        for (TrackedObject trackedObject : trackedObjects) {
            // for each tracked object, make {id, time, description, coordinates}
            JsonObject trackedObjectJson = new JsonObject();
            trackedObjectJson.addProperty("id", trackedObject.getID());
            trackedObjectJson.addProperty("time", trackedObject.getTime());
            trackedObjectJson.addProperty("description", trackedObject.getDescription());
            JsonArray trackedObjectPoints = new JsonArray();
            for (CloudPoint cp : trackedObject.getPoints()) {
                // for each coordinate, make {x, y}
                JsonObject cpJson = new JsonObject();
                cpJson.addProperty("x", cp.getX());
                cpJson.addProperty("y", cp.getY());
                trackedObjectPoints.add(cpJson);
            }
            trackedObjectJson.add("coordinates", trackedObjectPoints);
            lastFrameJson.add(trackedObject.getID(), trackedObjectJson);
        }
        // if needed, these will override the last frame of the lidar that is being
        // updated
        // other wise, they will just add the new data
        currLiDarWorkerTrackerLastFrame.add("LiDarWorkerTracker" + lidar.getID(), lastFrameJson);
        lastLiDarWorkerTrackersFrames.add("LiDarWorkerTracker" + lidar.getID(), currLiDarWorkerTrackerLastFrame);
        allLiDarWorkerTrackersLastFrames.add("lastLiDarWorkerTrackersFrame", lastLiDarWorkerTrackersFrames);
        lidarWorkerTrackersLastFrame = allLiDarWorkerTrackersLastFrames;
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
        addGoodOutput(output);
        if (errorMsg != null) {
            addErrorOutput(output);
        }
        writeOutputToFile(output);
    }

    private void addGoodOutput(JsonObject output) {
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
        }
    }

    private void addErrorOutput(JsonObject output) {
        JsonObject statistics = new JsonObject();
        for (String key : output.keySet()) {
            statistics.add(key, output.get(key));
        }
        if (errorMsg != null) {
            JsonObject landmarkJsonObject = new JsonObject();

            for (JsonObject landmark : landmarks.values()) {
                landmarkJsonObject.add(landmark.get("id").getAsString(), landmark);
            }
            statistics.add("landMarks", landmarkJsonObject);
        }
        output.add("statistics", statistics);
        output.addProperty("error", errorMsg);
        output.addProperty("faultySensor", faultySensor);
        output.add("lastCamerasFrame", camerasLastFrame);
        output.add("lastLiDarWorkerTrackersFrame", lidarWorkerTrackersLastFrame);
        output.addProperty("poses", poses.toString());
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
}