
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
    private String outputFilePath = initializeOutputFile();
    private JsonObject camerasLastFrame = new JsonObject();
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
        mapOfDetectedObjectsByTime.putIfAbsent(time, new ConcurrentHashMap<>());
        // we pull the camera using the time and get the c
        ConcurrentHashMap<String, List<DetectedObject>> currentCamMap = mapOfDetectedObjectsByTime.get(time);
        if (currentCamMap == null) {
            currentCamMap = new ConcurrentHashMap<>();
            mapOfDetectedObjectsByTime.put(time, currentCamMap);
        }
        currentCamMap.put("Camera" + cam.getID(), cam.getLastDetectedObjects());
        camerasLastTick.put("Camera" + cam.getID(), time);
    }

    private void createLastCameraFramesToJson() {
        List<String> cameraKeys = new ArrayList<>(camerasLastTick.keySet()); // get all camera names
        JsonObject jsonOfAllCamerasEachWithAnArrayOfJsonObjects = new JsonObject(); // create the json object to set
        for (String cameraKey : cameraKeys) { // iterate over all cameras
            JsonArray cameraJson = new JsonArray(); // create an array to hold all the current camera detected objects
            // the max time of the camera that holds a non empty list of detected objects
            int timeCameraEnded = mapOfDetectedObjectsByTime.keySet().stream()
                    .filter(time -> mapOfDetectedObjectsByTime.get(time).containsKey(cameraKey)).max(Integer::compare)
                    .orElse(mapOfDetectedObjectsByTime.keySet().stream().max(Integer::compare).orElse(0));
            List<DetectedObject> listOfDetectedObjectByCurrCamera = mapOfDetectedObjectsByTime.get(timeCameraEnded)
                    .get(cameraKey); // get the detected objects of the camera
            for (DetectedObject detectedObject : listOfDetectedObjectByCurrCamera) { // iterate over all detected
                                                                                     // objects
                JsonObject detectedObjectJson = new JsonObject(); // create a json object to hold the current detected
                                                                  // object
                detectedObjectJson.addProperty("id", detectedObject.getId()); // add the id of the current detected
                                                                              // object
                detectedObjectJson.addProperty("description", detectedObject.getDescription()); // add the description
                                                                                                // of the current
                                                                                                // detected object
                cameraJson.add(detectedObjectJson); // add the current detected object to the array of detected objects
            }
            jsonOfAllCamerasEachWithAnArrayOfJsonObjects.add(cameraKey, cameraJson); // add the array of detected
                                                                                     // objects to the camera
        }
        camerasLastFrame = jsonOfAllCamerasEachWithAnArrayOfJsonObjects; // set the completed json object
        for (String key : cameraKeys) {
            // i want to see all of the ticks and their entires
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
        // check if data already exists in the sf class
        ConcurrentHashMap<String, List<TrackedObject>> currentLiDarMap = mapOfTrackedObjectsByTime.get(time);
        if (currentLiDarMap == null) {
            currentLiDarMap = new ConcurrentHashMap<>();
            mapOfTrackedObjectsByTime.put(time, currentLiDarMap);
        }
        currentLiDarMap.put("LiDarWorkerTracker" + lidar.getID(), lidar.getTrackedObjectsByTime(time));
        lidarWorkerTrackersLastTick.put("LiDarWorkerTracker" + lidar.getID(), time);
    }

    private void createLastLidarFramesToJson() {
        List<String> lidarWorkerTrackerKeys = new ArrayList<>(lidarWorkerTrackersLastTick.keySet()); // get all lidar
                                                                                                     // names
        JsonObject jsonOfAllLidarsEachWithAnArrayOfJsonObjects = new JsonObject(); // create the json object to set
        for (String lidarWorkerTrackerKey : lidarWorkerTrackerKeys) { // iterate over all lidars
            JsonArray lidarWorkerTrackerJson = new JsonArray(); // create an array to hold all the tracked objects of
                                                                // the lidar
            int timeLidarEnded = mapOfTrackedObjectsByTime.keySet().stream()
                    .filter(time -> !mapOfTrackedObjectsByTime.get(time).isEmpty())
                    .max(Integer::compare).orElse(mapOfTrackedObjectsByTime.keySet().stream().max(Integer::compare)
                            .orElse(0)); // the max time of the lidar that holds a non empty list of tracked objects

            List<TrackedObject> listOfTrackedObjectByCurrLidar = mapOfTrackedObjectsByTime.get(timeLidarEnded)
                    .get(lidarWorkerTrackerKey); // get the tracked objects of the lidar
            for (TrackedObject trackedObject : listOfTrackedObjectByCurrLidar) { // iterate over all tracked objects
                JsonObject trackedObjectJson = new JsonObject(); // create a json object to hold the current tracked
                                                                 // object
                trackedObjectJson.addProperty("id", trackedObject.getID()); // add the id of the current tracked object
                trackedObjectJson.addProperty("time", trackedObject.getTime()); // add the time of the current tracked
                                                                                // object
                trackedObjectJson.addProperty("description", trackedObject.getDescription()); // add the description of
                                                                                              // the current tracked
                                                                                              // object
                JsonArray trackedObjectCoordinates = new JsonArray(); // create an array to hold the coordinates of the
                                                                      // current tracked object
                for (CloudPoint cloudPoint : trackedObject.getPoints()) { // iterate over all the points of the current
                                                                          // tracked object
                    JsonObject cloudPointJson = new JsonObject(); // create a json object to hold the current point
                    cloudPointJson.addProperty("x", cloudPoint.getX()); // add the x coordinate of the point
                    cloudPointJson.addProperty("y", cloudPoint.getY()); // add the y coordinate of the point
                    trackedObjectCoordinates.add(cloudPointJson); // add the point to the array
                }
                trackedObjectJson.add("coordinates", trackedObjectCoordinates); // add the array of coordinates to the
                                                                                // current tracked object
                lidarWorkerTrackerJson.add(trackedObjectJson); // add the current tracked object to the array of tracked
                                                               // objects
            }
            jsonOfAllLidarsEachWithAnArrayOfJsonObjects.add(lidarWorkerTrackerKey, lidarWorkerTrackerJson); // add the
                                                                                                            // array of
                                                                                                            // tracked
                                                                                                            // objects
                                                                                                            // to the
                                                                                                            // lidar
        }
        lidarWorkerTrackersLastFrame = jsonOfAllLidarsEachWithAnArrayOfJsonObjects; // set the completed json object
        for (String key : lidarWorkerTrackerKeys) {
            // i want to see all of the ticks and their entires
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
        checkIfOutputFileExists();
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
        output.add("lastCameraFrames", camerasLastFrame);
        output.add("lastLiDarFrames", lidarWorkerTrackersLastFrame);
        List<JsonObject> posesToOutput = new ArrayList<>();
        // we need to filter the poses to only get the ones upto the system runtime
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
}