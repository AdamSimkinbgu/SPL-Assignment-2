package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

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

    private void sendCrashCameraBroadcast(int currTick) {
        StatisticalFolder.getInstance().updateError(camera.getErrorMsg(), "Camera" + camera.getID());
        sendBroadcast(new CrashedBroadcast(camera.getErrorMsg(), getName(), currTick));
        camera.setStatus(STATUS.ERROR);
        terminate();
    }

    private void sendTerminatedCameraBroadcast() {
        sendBroadcast(new TerminatedBroadcast(getName()));
        camera.setStatus(STATUS.DOWN);
        terminate();
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
            if (StatisticalFolder.getInstance().isSystemDown()) {
                return;
            }
            int currTick = tick.getTick();
            try {
                if (camera.getStatus() == STATUS.UP) {
                    System.out.println(
                            "[TICKBROADCAST - DETECTING] - " + getName() + " detecting objects at tick " + currTick);
                    StampedDetectedObjects detectedObjects = camera.getDetectedObjects(currTick);
                    if (camera.getStatus() == STATUS.ERROR) { // camera got error during detecting objects
                        sendCrashCameraBroadcast(currTick);
                    } else {
                        if (detectedObjects != null) {
                            int doneDetectedTime = detectedObjects.getTime() + camera.getFrequency();
                            DetectObjectsEvent newEvent = new DetectObjectsEvent(getName(), doneDetectedTime,
                                    detectedObjects);
                            eventQ.add(newEvent);
                        }
                        while (!eventQ.isEmpty()) {
                            DetectObjectsEvent event = eventQ.peek();
                            if (event.getDetectedTime() > currTick)
                                break; // no events are due yet
                            else {
                                event = eventQ.poll();
                                Future<?> future = sendEvent(event);
                                // if (future.get(currTick + camera.getFrequency(), TimeUnit.SECONDS) == null) {
                                // sendCrashCameraBroadcast(currTick);
                                // }
                                // make the detected objects available for the updateLastDetectedObjects method
                                updateLastCamFrame(detectedObjects, currTick);
                                // if (future.get(currTick + camera.getFrequency(), TimeUnit.SECONDS) == null) {
                                // sendCrashCameraBroadcast(currTick);
                                // }

                                StatisticalFolder.getInstance().addNumDetectedObjects(
                                        event.getStampedDetectedObjects().getDetectedObjects().size());

                            }
                        }
                    }
                    if (camera.getStatus() == STATUS.DOWN) {
                        sendTerminatedCameraBroadcast();
                    }
                } else {
                    sendTerminatedCameraBroadcast();
                    StatisticalFolder.getInstance().setSystemIsDone(true);
                    StatisticalFolder.getInstance().setLastWorkTick(currTick);
                }
                if (currTick == camera.getTimeLimit()) // camera reached its time limit
                    camera.setStatus(STATUS.DOWN);
            } catch (Exception e) {
                e.printStackTrace();
                sendCrashCameraBroadcast(currTick);
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            if (terminated.getTerminatorName().contains("TimeService")) {
                System.err.println("[TERMINATEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                        + terminated.getTerminatorName());
                sendBroadcast(new TerminatedBroadcast(getName()));
                camera.setStatus(STATUS.DOWN);
                terminate();
            }
        });

        subscribeBroadcast(CrashedBroadcast.class, crash -> {
            System.err.println("[CRASHEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                    + crash.getCrasher());
            StatisticalFolder.getInstance().updateCamLastFrame(crash.getTimeCrashed(), camera);
            sendBroadcast(new TerminatedBroadcast(getName()));
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
            sendTerminatedCameraBroadcast();
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
}