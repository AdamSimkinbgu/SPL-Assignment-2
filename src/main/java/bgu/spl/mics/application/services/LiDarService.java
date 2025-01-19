package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.*;
import bgu.spl.mics.application.objects.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

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
    private PriorityQueue<TrackedObjectsEvent> eventsTODO;

    /**
     * Constructor for LiDarService.
     *
     * @param lidarWorkerTracker A LiDAR Tracker worker object that this service
     *                           will use to process data.
     */
    public LiDarService(LiDarWorkerTracker lidarWorkerTracker) {
        super("LiDarService" + lidarWorkerTracker.getID());
        this.lidarWorkerTracker = lidarWorkerTracker;
        this.eventsTODO = new PriorityQueue<>(Comparator.comparingInt(event -> event.getDetectionTime()));
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
            System.out.println(getName() + " got tick " + currTick);
            if (lidarWorkerTracker.getStatus() == STATUS.UP) {
                while (!eventsTODO.isEmpty()) {
                    if (eventsTODO.peek().getDetectionTime() <= currTick) {
                        TrackedObjectsEvent trackedObjectsEvent = eventsTODO.poll();
                        complete(trackedObjectsEvent.getMyEvent(), true);
                        lidarWorkerTracker.updateLastTrackedObjects(trackedObjectsEvent.getTrackedObjects());
                        sendEvent(trackedObjectsEvent);
                        System.out.println(getName() + " sent TrackedObjectsEvent at tick " + tick.getTick() +
                                " for objects from tick " + trackedObjectsEvent.getDetectionTime());
                        StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick, lidarWorkerTracker);
                    } else {
                        break;
                    }
                }
                if (eventsTODO.isEmpty()) {
                    if (lidarWorkerTracker.getStatus().equals(STATUS.DOWN)) {
                        System.out.println(getName() + " is finished");
                    } else {
                        System.out.println(getName() + " has no more events to send");
                    }
                    sendBroadcast(new TerminatedBroadcast(getName()));
                    terminate();
                }
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {
            System.out.println(getName() + " got TerminatedBroadcast from " + terminated.getTerminatorName());
            terminate();
        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {
            System.out.println(getName() + " got CrashedBroadcast from " + crashed.getCrasher());
            // sendBroadcast(new TerminatedBroadcast(getName())); ???
            terminate();
        });

        subscribeEvent(DetectObjectsEvent.class, event -> { // here we send TrackedObjectEvents
            int currTick = event.getSentTime();
            int dueTick = currTick + lidarWorkerTracker.getFrequency();
            System.out.println(getName() + " got tick " + currTick);
            if (lidarWorkerTracker.getStatus() == STATUS.UP) {
                // get the detected objects and time they were detected
                StampedDetectedObjects detectedObjects = event.getStampedDetectedObjects();
                // check if was an error in detections of objects and turning the service to
                // down if so
                ArrayList<TrackedObject> trackedObjects = lidarWorkerTracker.calculateTrackedObjects(detectedObjects);
                if (lidarWorkerTracker.getStatus().equals(STATUS.DOWN)) {
                    System.out.println(getName() + " got an error");
                    sendBroadcast(new CrashedBroadcast("LidarWorker " + lidarWorkerTracker.getID() + " got an error",
                            "in LiDarService " + getName()));
                    terminate();
                    StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick, lidarWorkerTracker);
                } else {
                    // create TrackedObjectsEvent with previous calculated tracked objects
                    TrackedObjectsEvent trackedObjectsEvent = new TrackedObjectsEvent(trackedObjects, currTick, dueTick,
                            event);
                    // checks if detection tick + lidar frequency is less than current tick so we
                    // can send event
                    if (dueTick <= currTick) {
                        // update last tracked objects for a future error
                        complete(event, true);
                        lidarWorkerTracker.updateLastTrackedObjects(trackedObjects);
                        sendEvent(trackedObjectsEvent);
                        System.out.println(getName() + " sent TrackedObjectsEvent at tick " + dueTick);
                        StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick, lidarWorkerTracker);
                    } else {
                        eventsTODO.add(trackedObjectsEvent);
                    } // current tick is less than due tick
                }
                if (eventsTODO.isEmpty()) {
                    if (lidarWorkerTracker.getStatus().equals(STATUS.DOWN)) {
                        System.out.println(getName() + " is finished");
                    } else {
                        System.out.println(getName() + " has no more events to send");
                    }
                    sendBroadcast(new TerminatedBroadcast(getName()));
                    terminate();
                }
            }
        });
    }
}
