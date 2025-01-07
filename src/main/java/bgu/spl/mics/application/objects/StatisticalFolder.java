package bgu.spl.mics.application.objects;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks
 * identified.
 */
public class StatisticalFolder {
    private int systemRuntime; // the total runtime of the system - measured in ticks.
    private int numDetectedObjects; // the cumulative count of objects detected all cameras. This includes both
                                    // initial detections and subsequent re-detections.
    private int numTrackedObjects; // the cumulative count of objects tracked by all LiDAR workers, encompassing
                                   // both new tracks and ongoing tracking of previously detected objects
    private int numLandmarks; // the total number of unique landmarks identified and mapped within the
                              // environment, this count is updated only when new landmarks are added to the
                              // map

    public static StatisticalFolder getInstance() {
        return StatisticalFolderHolder.instance;
    }

    private static class StatisticalFolderHolder {
        private static final StatisticalFolder instance = new StatisticalFolder();
    }

    private StatisticalFolder() {
        systemRuntime = 0;
        numDetectedObjects = 0;
        numTrackedObjects = 0;
        numLandmarks = 0;
    }

    public int getSystemRuntime() {
        return systemRuntime;
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

}
