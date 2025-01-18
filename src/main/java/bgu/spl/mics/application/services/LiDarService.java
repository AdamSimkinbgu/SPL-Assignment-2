package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.*;
import bgu.spl.mics.application.objects.*;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and
 * process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {
    private LiDarWorkerTracker lidarWorkerTracker;
    private PriorityBlockingQueue<>
    /**
     * Constructor for LiDarService.
     *
     * @param lidarWorkerTracker A LiDAR Tracker worker object that this service
     *                           will use to process data.
     */
    public LiDarService(LiDarWorkerTracker lidarWorkerTracker) {
        super("LiDarService");
        this.lidarWorkerTracker = lidarWorkerTracker;
        // maybe more added later
    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     * subscribes to TickBroadcast, TerminatedBroadcast, CrashedBroadcast,
     * DetectObjectsEvent
     * Tick need current time
     * Terminated needs nothing just the lidar name (getname())
     * Crashed needs errormsg and lidar name
     * DetectObjectsEvent needs lidar name, time that the stampedDetectedObjects was
     * detected,
     * and the stampedDetectedObjects itself
     */
    @Override
    protected void initialize() {
        System.out.println("LiDarService started");
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            int currTick = tick.getTick();
            int dueTick = currTick + lidarWorkerTracker.getFrequency();
            System.out.println("LiDarService " + getName() + " got tick " + currTick);

        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {

        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {

        });

        subscribeEvent(DetectObjectsEvent.class, event -> { // here we send TrackedObjectEvents
            int currTick = event.getSentTime();
            int dueTick = currTick + lidarWorkerTracker.getFrequency();
            System.out.println("LiDarService " + getName() + " got tick " + currTick);
            if (lidarWorkerTracker.getStatus() == STATUS.UP) {
                // process data
                // send TrackedObjectsEvent
                // ArrayList<TrackedObject> trackedObjects = each object has id, time, description.
                StampedDetectedObjects detectedObjects = event.getStampedDetectedObjects();
                ArrayList<TrackedObject> trackedObjects = lidarWorkerTracker.calculateTrackedObjects(detectedObjects);
                if (lidarWorkerTracker.getStatus().equals(STATUS.DOWN)){
                    System.out.println("LiDarService " + getName() + " got an error");
                    sendBroadcast(new CrashedBroadcast("LidarWorker " + lidarWorkerTracker.getID() + " got an error", "in LiDarService " + getName()));
                    terminate();
                    StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick, lidarWorkerTracker);
                }
                else {
                    TrackedObjectsEvent trackedObjectsEvent = new TrackedObjectsEvent(trackedObjects, currTick, dueTick);
                    if (dueTick <= currTick) {
                        lidarWorkerTracker.updateLastTrackedObjects(trackedObjects);
                        sendEvent(trackedObjectsEvent);
                        System.out.println("LiDarService " + getName() + " sent TrackedObjectsEvent at tick " + dueTick);
                        StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick, lidarWorkerTracker);
                    }
                    else {
                }
                // detectobject event has detectorname(which camera detected), at what time it was send and
                // object of stampeddetectedobjects
                // stampeddetectedobjects has time and arraylist of detectedobjects
                // detected object has id, description.
            }
        });
    }
}
