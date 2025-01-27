package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
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
    private ConcurrentHashMap<Integer, DetectObjectsEvent> eventMap;
    private ConcurrentLinkedQueue<DetectObjectsEvent> pendingEventQueue;
    private int currTick;
    private boolean canTerminate;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) {
        super("CameraService" + camera.getID());
        this.camera = camera;
        this.pendingEventQueue = new ConcurrentLinkedQueue<>();
        this.currTick = 0;
        this.canTerminate = false;
    }

    private void sendCrashCameraBroadcast(int currTick) {
        sendBroadcast(new CrashedBroadcast(camera.getErrorMsg(), getName(), currTick));
        StatisticalFolder.getInstance().updateError(camera.getErrorMsg(), "Camera" + camera.getID());
        camera.setStatus(STATUS.ERROR);
        // checkIfUpdateBeforeCrash(pendingEventQueue);
        if (!StatisticalFolder.getInstance().getSystemIsDone()) {
            StatisticalFolder.getInstance().setSystemIsDone(true);
        }
        terminate();
    }

    private void sendTerminatedCameraBroadcast() {
        sendBroadcast(new TerminatedBroadcast(getName()));
        camera.setStatus(STATUS.DOWN);
        if (!StatisticalFolder.getInstance().getSystemIsDone()) {
            StatisticalFolder.getInstance().setSystemIsDone(true);
        }
        terminate();
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for
     * sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        System.out.println("[INITIALIZING] - " + getName() + " started");
        subscribeBroadcast(TickBroadcast.class, tick -> {
            try {
                int tickToHandleIn = tick.getTick() + camera.getFrequency();
                currTick++;
                if (currTick == camera.getTimeLimit() || canTerminate) { // camera reached its time limit
                    camera.setStatus(STATUS.DOWN);
                    sendTerminatedCameraBroadcast();
                } else {
                    System.out.println("[TICKBROADCAST RECEIVED] - " + getName() + " got tick " + tick.getTick());
                    if (camera.getStatus() == STATUS.UP) {
                        System.out.println(
                                "[TICKBROADCAST - DETECTING] - " + getName() + " detecting objects at tick "
                                        + currTick);
                        StampedDetectedObjects detectedObjects = camera.getDetectedObjects(currTick);
                        if (camera.getStatus() == STATUS.ERROR) { // camera got error during detecting objects
                            sendCrashCameraBroadcast(currTick);
                        } else {
                            if (detectedObjects != null) { // im don't think this is necessary
                                DetectObjectsEvent newEvent = new DetectObjectsEvent(getName(), tickToHandleIn,
                                        detectedObjects);
                                pendingEventQueue.add(newEvent);
                            }
                            if (!pendingEventQueue.isEmpty()
                                    && pendingEventQueue.peek().getDetectedTime() <= currTick) {
                                DetectObjectsEvent event = pendingEventQueue.poll();
                                sendEvent(event);
                                StatisticalFolder.getInstance().updateCamLastFrame(currTick,
                                        camera);
                            }
                        }
                    } else {
                        sendTerminatedCameraBroadcast();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendCrashCameraBroadcast(currTick);
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            if (terminated.getTerminatorName().equals("TimeService")) {
                System.err.println("[TERMINATEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                        + terminated.getTerminatorName());
                camera.setStatus(STATUS.DOWN);
                terminate();
            }
        });

        subscribeBroadcast(CrashedBroadcast.class, crash -> {
            System.err.println("[CRASHEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                    + crash.getCrasher());
            // checkIfUpdateBeforeCrash(pendingEventQueue);
            camera.setStatus(STATUS.DOWN);
            terminate();
        });

        subscribeBroadcast(ZeroCamSensBroadcast.class, zero -> {
            System.out.println("[ZEROCAMSENSBROADCAST RECEIVED] - " + getName() + " got ZeroCamSensBroadcast");
            if (zero.getActiveCameras() == 0) {
                System.out.println("[ZEROCAMSENSBROADCAST - TERMINATING] - " + getName() + " terminating because "
                        + "all cameras are inactive");
                canTerminate = true;
            } else if (zero.getActiveSensors() == 0) {
                System.out.println("[ZEROCAMSENSBROADCAST - TERMINATING] - " + getName() + " terminating because "
                        + "all sensors are inactive");
                canTerminate = true;
            }
            camera.setStatus(STATUS.DOWN);
        });
    }

    private void updateLastCamFrame(StampedDetectedObjects detectedObjects, int currTick) {
        ArrayList<DetectedObject> detectedObjectsList = new ArrayList<>();
        detectedObjectsList.addAll(detectedObjects.getDetectedObjects());
        camera.updateLastDetectedObjects(detectedObjectsList);
        StatisticalFolder.getInstance().updateCamLastFrame(currTick, camera);
        StatisticalFolder.getInstance().addNumDetectedObjects(
                detectedObjects.getDetectedObjects().size());
    }

    public Camera getCamera() {
        return camera;
    }

    private void checkIfUpdateBeforeCrash(ConcurrentLinkedQueue<DetectObjectsEvent> pendingEventQueue) {
        camera.cameraCheckBeforeCrash(pendingEventQueue);
    }
}