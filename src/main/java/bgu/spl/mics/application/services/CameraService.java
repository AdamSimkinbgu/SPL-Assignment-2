package bgu.spl.mics.application.services;

import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.CrashedBroadcast;
import bgu.spl.mics.application.Messages.DetectObjectsEvent;
import bgu.spl.mics.application.Messages.TerminatedBroadcast;
import bgu.spl.mics.application.Messages.TickBroadcast;
import bgu.spl.mics.application.Messages.ZeroCamSensBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private Camera camera;
    private ConcurrentLinkedQueue<DetectObjectsEvent> eventQ;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) {
        super("CameraService" + camera.getID());
        this.camera = camera;
        this.eventQ = new ConcurrentLinkedQueue<>();
    }

    private void crashCameraBroadcast() {
        sendBroadcast(new CrashedBroadcast(camera.getErrorMsg(), getName()));
        terminate();
    }

    private void terminatedCameraBroadcast() {
        sendBroadcast(new TerminatedBroadcast(getName()));
        terminate();
    }

    private void terminatedCameraBroadcastWithStatistics(int currTick) {
        System.err.println("[TERMINATED] - " + getName() + " terminated, updating last frame");
        sendBroadcast(new TerminatedBroadcast(getName()));
        terminate();
        StatisticalFolder.getInstance().updateCamLastFrame(currTick, camera); // should update a variable in the
                                                                              // statistical folder instead
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for
     * sending
     * DetectObjectsEvents.
     */
    @Override
    protected synchronized void initialize() {
        System.out.println("[INITIALIZING] - " + getName() + " started");
        subscribeBroadcast(TickBroadcast.class, tick -> {
            System.out.println("[TICKBROADCAST RECEIVED] - " + getName() + " got tick " + tick.getTick());
            try {
                int currTick = tick.getTick();
                int dueTime = currTick + camera.getFrequency();
                if (camera.getStatus() == STATUS.UP) {
                    System.out.println(
                            "[TICKBROADCAST - DETECTING] - " + getName() + " detecting objects at tick " + dueTime);
                    StampedDetectedObjects detectedObjects = camera.getDetectedObjects(currTick);
                    if (camera.getStatus() == STATUS.ERROR) { // camera got error during detecting objects
                        crashCameraBroadcast();
                    } else {
                        if (detectedObjects != null) {
                            DetectObjectsEvent newEvent = new DetectObjectsEvent(getName(), dueTime, detectedObjects);
                            eventQ.add(newEvent);
                        }
                        while (!eventQ.isEmpty()) {
                            DetectObjectsEvent event = eventQ.peek();
                            if (event.getSentTime() > currTick)
                                break; // no events are due yet
                            else {
                                event = eventQ.poll();
                                Future<?> future = sendEvent(event);
                                if (future == null) {
                                    System.err.println("[ERROR - CRASHING] - " + getName() + " terminating with error: "
                                            + "Failed to send DetectObjectsEvent");
                                    camera.setStatus(STATUS.ERROR);
                                } else {
                                    System.out.println("[DETECTOBJECTSEVENT - SENT] - " + getName()
                                            + " sent DetectObjectsEvent at tick " + dueTime);

                                    // Uncomment and ensure thread safety if needed
                                    // StatisticalFolder.getInstance().updateCamLastFrame(currTick, camera);
                                }
                                // StatisticalFolder.getInstance().updateCamLastFrame(currTick, camera);
                            }
                        }
                    }
                    if (camera.getStatus() == STATUS.DOWN) {
                        System.out.println("[TERMINATED - CAMERA DOWN] - " + getName() + " terminated");
                        terminatedCameraBroadcastWithStatistics(currTick); // camera crashed
                    }
                } else {
                    terminatedCameraBroadcast();
                }
                if (currTick == camera.getTimeLimit()) // camera reached its time limit
                    camera.setStatus(STATUS.DOWN);
            } catch (Exception e) {
                System.err.println("[ERROR - CRASHING] - " + getName() + " terminating with error: " + e.getMessage());
                e.printStackTrace();
                sendBroadcast(new CrashedBroadcast(e.getMessage(), getName()));
                // terminate();
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            System.err.println("[TERMINATEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                    + terminated.getTerminatorName());
            camera.setStatus(STATUS.DOWN);
            terminate();
        });

        subscribeBroadcast(CrashedBroadcast.class, crash -> {
            System.err.println("[CRASHEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                    + crash.getCrasher());
            camera.setStatus(STATUS.DOWN);
            terminate();
        });

        subscribeBroadcast(ZeroCamSensBroadcast.class, zero -> {
            System.out.println("[ZEROCAMSENSBROADCAST RECEIVED] - " + getName() + " got ZeroCamSensBroadcast");
            if (zero.getActiveCameras() == 0) {
                System.out.println("[ZEROCAMSENSBROADCAST - TERMINATING] - " + getName() + " terminating because "
                        + "all cameras are inactive");
            } else if (zero.getActiveSensors() == 0) {
                System.out.println("[ZEROCAMSENSBROADCAST - TERMINATING] - " + getName() + " terminating because "
                        + "all sensors are inactive");
            }
            sendBroadcast(new TerminatedBroadcast(getName()));
            terminate();
            camera.setStatus(STATUS.DOWN);
        });

    }

    public Camera getCamera() {
        return camera;
    }
}