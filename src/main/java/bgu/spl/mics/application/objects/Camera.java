package bgu.spl.mics.application.objects;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private List<DetectedObject> detectedObjects;
    private int timeLimit;

    public Camera(int id, int frequency, List<DetectedObject> detectedObjects, int timeLimit) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.detectedObjects = Collections.unmodifiableList(detectedObjects);
        this.timeLimit = timeLimit;
    }

    public Camera(int id, int frequency, String filePath, String cameraKey) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.detectedObjects = new ArrayList<>();
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
                detectedObjects.addAll(detectedObjects);
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

    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }

    public void detectObject(DetectedObject object) {
        detectedObjects.add(object);
    }

    public void clearDetectedObjects() {
        detectedObjects.clear();
    }

}