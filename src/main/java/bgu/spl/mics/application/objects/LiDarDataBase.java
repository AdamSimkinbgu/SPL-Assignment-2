package bgu.spl.mics.application.objects;

import bgu.spl.mics.MessageBusImpl;

import java.util.List;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for
 * tracked objects.
 */
public class LiDarDataBase {
    private static String filePath = "lidar_data.json";
    // private List<StampedCloudPoints> stampedCloudPoints;

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
        loadCloudPoints();
    }

    private void loadCloudPoints() {
        // load cloud points from file
    }

}
