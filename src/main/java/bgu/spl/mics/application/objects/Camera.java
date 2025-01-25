package bgu.spl.mics.application.objects;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */
public class Camera {
    private int id;
    private int frequency;
    private STATUS status;
    private ConcurrentHashMap<Integer, StampedDetectedObjects> detectedObjects;
    private ArrayList<DetectedObject> lastDetectedObjects;
    private int timeLimit;
    private String errorMsg;
    // private StampedDetectedObjects lastDetectedObjects;

    public Camera(int id, int frequency, ConcurrentHashMap<Integer, StampedDetectedObjects> detectedObjects,
            int timeLimit) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.detectedObjects = detectedObjects;
        this.timeLimit = timeLimit;
        this.lastDetectedObjects = new ArrayList<>();
        this.errorMsg = null;
        this.lastDetectedObjects = new ArrayList<>();
    }

    public Camera(int id, int frequency, String filePath, String cameraKey) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.detectedObjects = new ConcurrentHashMap<>();
        this.lastDetectedObjects = new ArrayList<>();

        loadDetectedObjectsFromFilePath(filePath, cameraKey);
    }

    private void loadDetectedObjectsFromFilePath(String filePath, String cameraKey) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            java.lang.reflect.Type type = new TypeToken<Map<String, List<List<StampedDetectedObjects>>>>() {
            }.getType();
            Map<String, List<List<StampedDetectedObjects>>> detectedObjectsMap = gson.fromJson(reader, type);
            List<List<StampedDetectedObjects>> nestedCameraObjects = detectedObjectsMap.get(cameraKey);
            if (!(nestedCameraObjects == null)) {
                List<StampedDetectedObjects> cameraDetectedObjects = new ArrayList<>();
                for (List<StampedDetectedObjects> stampedDetectedObjects : nestedCameraObjects) {
                    cameraDetectedObjects.addAll(stampedDetectedObjects);
                }
                detectedObjects.putAll(cameraDetectedObjects.stream()
                        .collect(Collectors.toConcurrentMap(StampedDetectedObjects::getTime, d -> d)));
                timeLimit = cameraDetectedObjects.stream().mapToInt(StampedDetectedObjects::getTime).max().orElse(0);
            } else {
                timeLimit = 0;
            }
            System.out.println("Camera " + id + " was loaded with " + detectedObjects.size() + " detected objects");
        } catch (Exception e) {
            System.out.println("Failed to load detected objects for camera " + id + " because of " + e.getMessage());
        }
    }

    public void isDone(int time) {
        if (time >= timeLimit) {
            setStatus(STATUS.DOWN);
        }
    }

    public int getID() {
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public ConcurrentHashMap<Integer, StampedDetectedObjects> getDetectedObjects() {
        return detectedObjects;
    }

    public StampedDetectedObjects getDetectedObjects(int time) {
        StampedDetectedObjects sdo = detectedObjects.values().stream().filter(d -> d.getTime() == time).findFirst()
                .orElse(null);
        if (sdo != null) {
            DetectedObject error = sdo.getDetectedObjects().stream().filter(DetectedObject::isError).findFirst()
                    .orElse(null);
            if (error != null) {
                setStatus(STATUS.ERROR);
                errorMsg = error.getDescription();
                return null;
            }
        }
        return sdo;
    }

    public void clearDetectedObjects() {
        detectedObjects.clear();
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void updateLastDetectedObjects(ArrayList<DetectedObject> detectedObjects) {
        lastDetectedObjects = detectedObjects;
    }

    public ArrayList<DetectedObject> getLastDetectedObjects() {
        // public StampedDetectedObjects getLastDetectedObjects() {
        return lastDetectedObjects;
    }
}