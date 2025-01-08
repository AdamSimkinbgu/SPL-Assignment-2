package bgu.spl.mics.application.objects;

import bgu.spl.mics.MessageBusImpl;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for
 * tracked objects.
 */
public class LiDarDataBase {
    private static String filePath;

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */
    private static class SingletonHolder {
        private static LiDarDataBase instance = new LiDarDataBase(filePath);
    }
    public static LiDarDataBase getInstance(String filePath) {
        return SingletonHolder.instance;
    }

    private LiDarDataBase(String filePath) {
        this.filePath = filePath;
    }
}
