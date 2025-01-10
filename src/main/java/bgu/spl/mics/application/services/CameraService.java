package bgu.spl.mics.application.services;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.mics.Callback;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.DetectObjectsEvent;
import bgu.spl.mics.application.Messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private Camera camera;
    private ConcurrentLinkedQueue<DetectObjectsEvent> detectedObjects;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) {
        super("CamaeraService" + camera);
        // TODO Implement this
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts, TerminatedBroadcast, CrashedBroadcast and sets up callbacks for
     * sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tickbroadcast) -> {

        });
    }
}