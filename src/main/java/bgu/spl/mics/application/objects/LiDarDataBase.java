package bgu.spl.mics.application.objects;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for
 * tracked objects.
 */
public class LiDarDataBase {
    private static String filePath = Paths.get("example/lidar_data.json").toString();
    private List<StampedCloudPoints> cloudPoints;

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */
    private static class SingletonHolder {
        private static LiDarDataBase instance = new LiDarDataBase();
    }

    public static LiDarDataBase getInstance(String filePath) {
        return SingletonHolder.instance;
    }

    private LiDarDataBase() {
        this.cloudPoints = loadCloudPoints(filePath);
    }

    private List<StampedCloudPoints> loadCloudPoints(String filePath) {
        if (!checkForFile()) {
            return new ArrayList<>(); // return empty list if the file doens't exist
        }
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, new TypeToken<List<StampedCloudPoints>>() {
            }.getType());
        } catch (IOException e) {
            System.err.println("Failed to load LiDAR data from file: " + filePath + ". Error: " + e.getMessage());
            return new ArrayList<>(); // return empty list without cloud points after load problem
        }
    }

    private boolean checkForFile() {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("LiDAR data file not found at: " + filePath);
            return false;
        }
        return true;
    }

    public List<StampedCloudPoints> getCloudPoints() {
        // getter for cloud points
        return cloudPoints;
    }

}
