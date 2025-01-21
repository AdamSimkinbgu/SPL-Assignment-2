
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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks
 * identified.
 */
public class StatisticalFolder {
    private int systemRuntime;
    private int numDetectedObjects;
    private int numTrackedObjects;
    private int numLandmarks;
    private String outputFilePath = initializeOutputFile();

    public static StatisticalFolder getInstance() {
        return StatisticalFolderHolder.instance;
    }

    private static class StatisticalFolderHolder {
        private static final StatisticalFolder instance = new StatisticalFolder();
    }

    private StatisticalFolder() {
        systemRuntime = new Random().nextInt(1000);
        numDetectedObjects = 0;
        numTrackedObjects = 0;
        numLandmarks = 0;
        File file = new File(outputFilePath);
        file.delete();
    }

    public int getSystemRuntime() {
        return systemRuntime;
    }

    public void increaseSystemRuntime() {
        systemRuntime++;
    }

    public int getNumDetectedObjects() {
        return numDetectedObjects;
    }

    public int getNumTrackedObjects() {
        return numTrackedObjects;
    }

    public int getNumLandmarks() {
        return numLandmarks;
    }

    public void increaseNumDetectedObjects() {
        numDetectedObjects++;
    }

    public void increaseNumTrackedObjects() {
        numTrackedObjects++;
    }

    public void increaseNumLandmarks() {
        numLandmarks++;
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

    /*
     * the following functions are (just to keep it neat):
     * 1) updateStatistics
     * 2) updateLandmarks
     * 3) updateCamLastFrame
     * 4) updatelastLiDarWorkerTrackerFrame
     * 5) updatePoses
     * 6) updateOutputCamError
     * are more needed? add under this comment
     */

    public void updateStatistics() {
        checkIfOutputFileExists();
        try (FileReader reader = new FileReader(outputFilePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject output = reader.ready() ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonObject();
            JsonObject statistics = output.has("statistics") ? output.getAsJsonObject("statistics") : new JsonObject();
            statistics.addProperty("systemRuntime", systemRuntime);
            statistics.addProperty("numDetectedObjects", numDetectedObjects);
            statistics.addProperty("numTrackedObjects", numTrackedObjects);
            statistics.addProperty("numLandmarks", numLandmarks);
            output.add("statistics", statistics);
            synchronized (outputFilePath) {
                try (FileWriter writer = new FileWriter(outputFilePath)) {
                    gson.toJson(output, writer);
                    System.out.println("Statistics were updated in " + outputFilePath);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to update the statistics in the output file because of " + e.getMessage());
        }
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

    public void updateLandmarks(ArrayList<LandMark> landmarks) {
        checkIfOutputFileExists();
        try (FileReader reader = new FileReader(outputFilePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject output = reader.ready() ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonObject();
            JsonObject landMarks = output.has("landMarks") ? output.getAsJsonObject("landMarks") : new JsonObject();
            for (LandMark landmark : landmarks) {
                JsonObject landmarkJson = new JsonObject();
                landmarkJson.addProperty("id", landmark.getID());
                landmarkJson.addProperty("description", landmark.getDescription());
                JsonObject coordinates = new JsonObject();
                for (List<Double> point : landmark.getPoints()) {
                    JsonObject pointJson = new JsonObject();
                    pointJson.addProperty("x", point.get(0));
                    pointJson.addProperty("y", point.get(1));
                    coordinates.add("Coordinates", pointJson);
                    // landmarkJson.add(landmark.getID(), landmarkJson);
                }
                landMarks.add(landmark.getID(), landmarkJson);

            }
            output.add("landMarks", landMarks);
            synchronized (outputFilePath) {
                try (FileWriter writer = new FileWriter(outputFilePath)) {
                    gson.toJson(output, writer);
                    System.out.println("Statistics were updated in " + outputFilePath);
                }
            }
            numLandmarks = landMarks.size();
        } catch (Exception e) {
            System.err.println("Failed to update the LandMarks in the output file because of " + e.getMessage());
        }
    }

    public void updateCamLastFrame(int time, Camera cam) {
        checkIfOutputFileExists();
        try (FileReader reader = new FileReader(outputFilePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject output = reader.ready() ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonObject();
            JsonObject cameraLastFrame = output.has("lastCamerasFrame") ? output.getAsJsonObject("lastCamerasFrame")
                    : new JsonObject();
            JsonObject lastFrameJson = new JsonObject();
            lastFrameJson.addProperty("time", time);
            lastFrameJson.add("detectedObjects", gson.toJsonTree(cam.getDetectedObjects().get(time)));
            cameraLastFrame.add("Camera" + cam.getID(), lastFrameJson);
            output.add("lastCamerasFrame", cameraLastFrame);
            synchronized (outputFilePath) {
                try (FileWriter writer = new FileWriter(outputFilePath)) {
                    gson.toJson(output, writer);
                    System.out.println("Statistics were updated in " + outputFilePath);
                }
            }
            numDetectedObjects += cam.getDetectedObjects().values().stream()
                    .filter(detectedObjects -> detectedObjects.getTime() == time).count();
        } catch (Exception e) {
            System.err.println("Failed to update the camera's output file because of " + e.getMessage());
        }
    }

    public void updatelastLiDarWorkerTrackerFrame(int time, LiDarWorkerTracker lidar) {
        checkIfOutputFileExists();
        try (FileReader reader = new FileReader(outputFilePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject output = reader.ready() ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonObject();
            JsonObject lidarLastFrame = output.has("lastLiDarWorkerTrackersFrame")
                    ? output.getAsJsonObject("lastLiDarWorkerTrackersFrame")
                    : new JsonObject();
            JsonObject lastFrameJson = new JsonObject();
            lastFrameJson.addProperty("time", time);
            lastFrameJson.add("trackedObjects",
                    lidar.didDetectAny() ? gson.toJsonTree(lidar.getLastTrackedObject()) : new JsonObject());
            lidarLastFrame.add("LiDarWorkerTracker" + lidar.getID(), lastFrameJson);
            output.add("lastLiDarWorkerTrackersFrame", lidarLastFrame);
            synchronized (outputFilePath) {
                try (FileWriter writer = new FileWriter(outputFilePath)) {
                    gson.toJson(output, writer);
                    System.out.println("Statistics were updated in " + outputFilePath);
                }
            }
            numTrackedObjects += lidar.getLastTrackedObject().stream()
                    .filter(trackedObject -> trackedObject.getTime() == time).count();
        } catch (Exception e) {
            System.err.println("Failed to update the LiDarWorker's output file because of " + e.getMessage());
        }
    }

    public void updatePoses(ConcurrentHashMap<Integer, Pose> poses) {
        checkIfOutputFileExists();
        try (FileReader reader = new FileReader(outputFilePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject output = reader.ready() ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonObject();
            JsonObject posesJson = output.has("poses") ? output.getAsJsonObject("poses") : new JsonObject();
            for (Pose pose : poses.values()) {
                JsonObject poseJson = new JsonObject();
                poseJson.addProperty("time", pose.getTime());
                poseJson.addProperty("x", pose.getX());
                poseJson.addProperty("y", pose.getY());
                poseJson.addProperty("yaw", pose.getYaw());
                posesJson.add("pose" + pose.getTime(), poseJson);
            }
            output.add("poses", posesJson);
            synchronized (outputFilePath) {
                try (FileWriter writer = new FileWriter(outputFilePath)) {
                    gson.toJson(output, writer);
                    System.out.println("Statistics were updated in " + outputFilePath);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to update the poses in the output file because of " + e.getMessage());
        }
    }

    public void updateOutputCamError(int time, Camera cam) {
        checkIfOutputFileExists();
        try (FileReader reader = new FileReader(outputFilePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject output = reader.ready() ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonObject();
            String errorMsg = cam.getErrorMsg();
            output.addProperty("error", errorMsg);
            output.addProperty("faultySensor", "Camera" + cam.getID());
            try (FileWriter writer = new FileWriter(outputFilePath)) {
                gson.toJson(output, writer);
                System.out.println("Error of Camera " + cam.getID() + " was updated in " + outputFilePath);
            }
        } catch (Exception e) {
            System.err.println("Failed to update the camera's output file because of " + e.getMessage());
        }
    }

}