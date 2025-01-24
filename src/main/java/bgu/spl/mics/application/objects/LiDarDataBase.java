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
    private static String filePath = null;
    private List<StampedCloudPoints> stampedCloudPoints;

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
        if (LiDarDataBase.filePath == null) {
            LiDarDataBase.filePath = filePath;
        }
        return SingletonHolder.instance;
    }

    private LiDarDataBase() {
        this.stampedCloudPoints = loadCloudPoints(filePath);
        // System.out.println("CloudPoints: " + stampedCloudPoints);
    }

    public void setFilePath(String fp) {
        filePath = fp;
    }

    private List<StampedCloudPoints> loadCloudPoints(String filePath) {
        if (!checkForFile()) {
            return new ArrayList<>(); // return empty list if the file doens't exist
        }
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            java.lang.reflect.Type type = new TypeToken<List<StampedCloudPoints>>() {
            }.getType();
            List<StampedCloudPoints> stampedCloudPoints = gson.fromJson(reader, type);
            return stampedCloudPoints;
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

    public List<StampedCloudPoints> getStampedCloudPoints() {
        // getter for cloud points
        return stampedCloudPoints;
    }

    public StampedCloudPoints getStampedCloudPoints(int time, String id) {
        // getter for cloud points
        for (StampedCloudPoints stampedCP : stampedCloudPoints) {
            if (stampedCP.getTime() == time && stampedCP.getID().equals(id)) {
                return stampedCP;
            }
        }
        return null;
    }

}
