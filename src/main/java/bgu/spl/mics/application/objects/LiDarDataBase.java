package bgu.spl.mics.application.objects;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for
 * tracked objects.
 */
public class LiDarDataBase {
    private static LiDarDataBase instance = null;
    private String filePath;

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */

    public static LiDarDataBase getInstance(String filePath) {
        if (instance == null) {
            // Double check locking - this is thread safe but maybe not the best solution
            synchronized (LiDarDataBase.class) {
                if (instance == null)
                    instance = new LiDarDataBase(filePath);
            }
        }
        return instance;
    }

    private LiDarDataBase(String filePath) {
        this.filePath = filePath;
    }
}
