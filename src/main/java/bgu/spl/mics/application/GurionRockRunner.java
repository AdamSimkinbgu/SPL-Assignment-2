package bgu.spl.mics.application;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.LiDarDataBase;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.services.CameraService;
import bgu.spl.mics.application.services.FusionSlamService;
import bgu.spl.mics.application.services.LiDarService;
import bgu.spl.mics.application.services.PoseService;
import bgu.spl.mics.application.services.TimeService;

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * <p>
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 * </p>
 */
public class GurionRockRunner {

    /**
     * The main method of the simulation.
     * This method sets up the necessary components, parses configuration files,
     * initializes services, and starts the simulation.
     *
     * @param args Command-line arguments. The first argument is expected to be the
     *             path to the configuration file.
     */
    public static void main(String[] args) {
        System.out.println("Hello World!");
        if (args.length < 1) {
            System.err.println("Error: Configuration file path is required as first argument.");
            return;
        }

        String configPath = args[0];
        File configFile = new File(configPath);
        String configDirectory = configFile.getParent();

        Path path = Paths.get(configPath);
        Path parent = path.getParent();
        StatisticalFolder.getInstance().setConfigPath(parent.toString());

        List<CameraService> cameraServices;
        List<LiDarService> lidarServices;
        PoseService poseService;
        try (FileReader reader = new FileReader(configPath)) {
            Gson gson = new Gson();
            JsonObject config = gson.fromJson(reader, JsonObject.class);

            cameraServices = createCameras(config, configDirectory, gson);
            lidarServices = createLidarServices(config, configDirectory, gson);
            poseService = createPoseSerive(config, configDirectory, gson);

            FusionSlam fusionSlam = FusionSlam.getInstance();
            FusionSlamService fusionSlamService = new FusionSlamService();

            int numActiveCameras = cameraServices.size();
            int numActiveSensors = lidarServices.size();

            fusionSlam.setNumberOfActiveCameras(numActiveCameras);
            fusionSlam.setNumberOfActiveSensors(numActiveSensors);

            System.out.println("Active Cameras: " + numActiveCameras);
            System.out.println("Active Sensors: " + numActiveSensors);

            int tickTime = config.get("TickTime").getAsInt();
            int duration = config.get("Duration").getAsInt();
            TimeService timeService = new TimeService(tickTime, duration);

            List<Thread> threads = new ArrayList<>();
            for (CameraService cameraService : cameraServices) {
                threads.add(new Thread(cameraService));
            }
            for (LiDarService lidarService : lidarServices) {
                threads.add(new Thread(lidarService));
            }
            threads.add(new Thread(poseService));
            threads.add(new Thread(fusionSlamService));

            Thread timeServiceThread = new Thread(timeService);
            threads.add(timeServiceThread);

            for (Thread thread : threads) {
                if (thread != timeServiceThread) {
                    thread.start();
                }
            }

            Thread.sleep(100);
            timeServiceThread.start();

            for (Thread thread : threads) {
                thread.join();
            }
            StatisticalFolder.getInstance().createOutput();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private static PoseService createPoseSerive(JsonObject config, String configDirectory, Gson gson) {
        String poseFilePath = config.get("poseJsonFile").getAsString();
        poseFilePath = Paths.get(configDirectory, poseFilePath).toString(); // Adjust path
        PoseService poseService = null;
        try (FileReader poseReader = new FileReader(poseFilePath)) {
            // java.lang.reflect.Type poseListType = new
            // com.google.gson.reflect.TypeToken<List<Pose>>() {
            // }.getType();
            // HashMap<Integer, Pose> poseList = gson.fromJson(poseReader, poseListType);
            // // Compute maxTime as the maximum time in the poseList
            // int maxTime = poseList.stream().mapToInt(Pose::getTime).max().orElse(0); //
            // Default to 0 if the list is
            // // empty
            ConcurrentHashMap<Integer, Pose> poseList = new ConcurrentHashMap<Integer, Pose>();
            Pose[] poses = gson.fromJson(poseReader, Pose[].class);
            for (Pose pose : poses) {
                poseList.put(pose.getTime(), pose);
            }
            GPSIMU gpsimu = new GPSIMU(poseList); // GPSIMU gpsimu = new GPSIMU(poseList, maxTime);
            poseService = new PoseService(gpsimu);
        } catch (IOException e) {
            System.err.println("Error reading pose data file: " + poseFilePath);
            e.printStackTrace();
        }
        return poseService;
    }

    private static List<LiDarService> createLidarServices(JsonObject config, String configDirectory, Gson gson) {
        List<LiDarService> lidarServices = new ArrayList<>();
        JsonObject lidarConfig = config.getAsJsonObject("LiDarWorkers");
        String lidarDataPath = lidarConfig.get("lidars_data_path").getAsString();
        lidarDataPath = Paths.get(configDirectory, lidarDataPath).toString();
        LiDarDataBase.getInstance(lidarDataPath);
        JsonArray lidarConfigs = lidarConfig.getAsJsonArray("LidarConfigurations");

        for (com.google.gson.JsonElement lidarJson : lidarConfigs) {
            int id = lidarJson.getAsJsonObject().get("id").getAsInt();
            int frequency = lidarJson.getAsJsonObject().get("frequency").getAsInt();
            LiDarWorkerTracker lidarWorker = new LiDarWorkerTracker(id, frequency);
            lidarServices.add(new LiDarService(lidarWorker));
        }
        return lidarServices;
    }

    private static List<CameraService> createCameras(JsonObject config, String configDirectory, Gson gson) {
        List<CameraService> cameraServices = new ArrayList<>();
        JsonObject camerasConfig = config.getAsJsonObject("Cameras");
        String cameraDataPath = camerasConfig.get("camera_datas_path").getAsString();
        cameraDataPath = Paths.get(configDirectory, cameraDataPath).toString();
        JsonArray cameraConfigs = camerasConfig.getAsJsonArray("CamerasConfigurations");
        try (FileReader cameraDataReader = new FileReader(cameraDataPath)) {
            JsonObject cameraData = gson.fromJson(cameraDataReader, JsonObject.class);

            for (com.google.gson.JsonElement cameraConfig : cameraConfigs) {
                JsonObject cameraJson = cameraConfig.getAsJsonObject();
                int id = cameraJson.get("id").getAsInt();
                int frequency = cameraJson.get("frequency").getAsInt();
                String cameraKey = cameraJson.get("camera_key").getAsString();
                JsonArray stampedObjectsJson = cameraData.getAsJsonArray(cameraKey);
                ConcurrentHashMap<Integer, StampedDetectedObjects> detectedObjectsList = new ConcurrentHashMap<>();

                for (com.google.gson.JsonElement stampedObjectJson : stampedObjectsJson) {
                    JsonObject stampedObject = stampedObjectJson.getAsJsonObject();
                    int time = stampedObject.get("time").getAsInt();
                    JsonArray detectedObjectsJson = stampedObject.getAsJsonArray("detectedObjects");

                    ArrayList<DetectedObject> detectedObjects = new ArrayList<>();
                    for (com.google.gson.JsonElement detectedObjectJson : detectedObjectsJson) {
                        JsonObject detectedObject = detectedObjectJson.getAsJsonObject();
                        String idStr = detectedObject.get("id").getAsString();
                        String description = detectedObject.get("description").getAsString();
                        detectedObjects.add(new DetectedObject(idStr, description));
                    }
                    detectedObjectsList.put(time, new StampedDetectedObjects(time, detectedObjects));
                }
                int maxTime = detectedObjectsList.values().stream().mapToInt(StampedDetectedObjects::getTime).max()
                        .orElse(0);

                Camera camera = new Camera(id, frequency, detectedObjectsList, maxTime + frequency);
                cameraServices.add(new CameraService(camera));
            }
        } catch (IOException e) {
            System.err.println("Error reading camera data file: " + cameraDataPath);
            e.printStackTrace();
        }

        return cameraServices;
    }
}
