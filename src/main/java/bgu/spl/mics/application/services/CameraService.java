package bgu.spl.mics.application.services;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import bgu.spl.mics.Callback;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.CrashedBroadcast;
import bgu.spl.mics.application.Messages.DetectObjectsEvent;
import bgu.spl.mics.application.Messages.TerminatedBroadcast;
import bgu.spl.mics.application.Messages.TickBroadcast;
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
        super("CameraService");

    }

    private void crashCameraBroadcast() {
        System.err.println("CameraService " + getName() + " crashed with error: " + camera.getErrorMsg());
        sendBroadcast(new CrashedBroadcast(camera.getErrorMsg(), getName()));
        terminate();
    }

    private void terminatedCameraBroadcast() {
        System.err.println("CameraService " + getName() + " terminated");
        sendBroadcast(new TerminatedBroadcast(getName()));
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
        System.out.println(getName() + " started");
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            int currTick = tick.getTick();
            int dueTime = currTick + camera.getFrequency();
            System.out.println("CameraService " + getName() + " got tick " + currTick);
            if (camera.getStatus() == STATUS.UP) {
                StampedDetectedObjects detectedObjects = camera.getDetectedObjects(currTick);
                if (camera.getStatus() == STATUS.ERROR) // camera got error during detecting objects
                    crashCameraBroadcast();
                else {
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
                            sendEvent(event);
                            System.out.println("CameraService " + getName() + " sent DetectObjectsEvent at tick " + dueTime);
                            StatisticalFolder.getInstance().updateCamLastFrame(currTick, camera);
                            // update statistical folder
                        }
                    }
                }
                if (camera.getStatus() == STATUS.DOWN)
                    terminatedCameraBroadcast(); // camera crashed
            } else
                terminatedCameraBroadcast(); // camera was not up
        });
        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            System.err.println("CameraService " + getName() + " terminated");
            camera.setStatus(STATUS.DOWN);
            terminate();
        });
        subscribeBroadcast(CrashedBroadcast.class, crash -> {
            System.err.println("CameraService " + getName() + " crashed with error: " + crash.getErrorMsg());
            camera.setStatus(STATUS.DOWN);
            terminate();
        });



    }
}