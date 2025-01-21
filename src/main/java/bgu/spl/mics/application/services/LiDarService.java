package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.*;
import bgu.spl.mics.application.objects.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private ConcurrentLinkedQueue<TrackedObjectsEvent> eventsTODO;

    /**
     * Constructor for LiDarService.
     *
     * @param lidarWorkerTracker A LiDAR Tracker worker object that this service
     *                           will use to process data.
     */
    public LiDarService(LiDarWorkerTracker lidarWorkerTracker) {
        super("LiDarService" + lidarWorkerTracker.getID());
        this.lidarWorkerTracker = lidarWorkerTracker;
        this.eventsTODO = new ConcurrentLinkedQueue<>();
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
        System.out.println("[INITIALIZING] - " + getName() + " started");
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            System.out.println("[TICKBROADCAST RECEIVED] - " + getName() + " got tick " + tick.getTick());
            int currTick = tick.getTick();
            if (lidarWorkerTracker.getStatus() == STATUS.UP) {
                while (!eventsTODO.isEmpty()) {
                    if (eventsTODO.peek().getDetectionTime() <= currTick) {
                        TrackedObjectsEvent trackedObjectsEvent = eventsTODO.poll();
                        sendEvent(trackedObjectsEvent);
                        complete(trackedObjectsEvent.getMyEvent(), true);
                        lidarWorkerTracker.updateLastTrackedObjects(trackedObjectsEvent.getTrackedObjects());
                        System.out.println(
                                "[TICKBROADCAST - SUCCESS] - " + getName() + " sent TrackedObjectsEvent at tick "
                                        + trackedObjectsEvent.getDueTime());
                    } else {
                        System.out.println("[TICKBROADCAST - NO WORK] - " + getName() + " has no more events to send");
                        break;
                    }
                }
                if (eventsTODO.isEmpty() && lidarWorkerTracker.getStatus().equals(STATUS.DOWN)) {
                    System.out.println("[TICKBROADCAST - TERMINATE] - " + getName() + " has no more events to send");
                    sendBroadcast(new TerminatedBroadcast(getName()));
                    terminate();
                }
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {
            System.out.println("[TERMINATEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                    + terminated.getTerminatorName());
            lidarWorkerTracker.setStatus(STATUS.DOWN);
            sendBroadcast(new TerminatedBroadcast(getName()));
            terminate();

        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {
            System.out.println("[CRASHEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                    + crashed.getCrasher());
            lidarWorkerTracker.setStatus(STATUS.DOWN);
            sendBroadcast(new TerminatedBroadcast(getName()));
            terminate();
            // StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick,
            // blahblah fix later
        });

        subscribeEvent(DetectObjectsEvent.class, event -> { // here we send TrackedObjectEvents
            System.out.println("[DETECTOBJECTSEVENT RECEIVED] - " + getName() + " got DetectObjectsEvent at tick "
                    + event.getSentTime() + " from " + event.getDetectorName());
            int currTick = event.getSentTime();
            int dueTick = currTick + lidarWorkerTracker.getFrequency();
            if (lidarWorkerTracker.getStatus() == STATUS.UP) {
                // get the detected objects and time they were detected
                StampedDetectedObjects detectedObjects = event.getStampedDetectedObjects();
                // check if was an error in detections of objects and turning the service to
                // down if so
                ArrayList<TrackedObject> trackedObjects = lidarWorkerTracker.calculateTrackedObjects(detectedObjects);
                if (lidarWorkerTracker.getStatus().equals(STATUS.ERROR)) {
                    System.out.println("[ERROR - CRASHING] - " + getName() + " terminating with error: "
                            + "Failed to detect objects");
                    sendBroadcast(new CrashedBroadcast("LidarWorker " + lidarWorkerTracker.getID() + " got an error",
                            "in LiDarService " + getName()));
                    terminate();
                    // StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick,
                    // lidarWorkerTracker);
                } else {
                    // create TrackedObjectsEvent with previous calculated tracked objects
                    TrackedObjectsEvent trackedObjectsEvent = new TrackedObjectsEvent(trackedObjects, currTick, dueTick,
                            event);
                    // checks if detection tick + lidar frequency is less than current tick so we
                    // can send event
                    if (dueTick <= currTick) {
                        // update last tracked objects for a future error
                        Future<?> future = sendEvent(trackedObjectsEvent);
                        if (future == null) {
                            System.err.println("[ERROR - CRASHING] - " + getName() + " terminating with error: "
                                    + "Failed to send TrackedObjectsEvent");
                            lidarWorkerTracker.setStatus(STATUS.ERROR);
                        } else {
                            System.out.println("[DETECTOBJECTSEVENT - SENT] - " + getName()
                                    + " sent TrackedObjectsEvent at tick " + dueTick);
                            complete(event, true);
                            lidarWorkerTracker.updateLastTrackedObjects(trackedObjects);
                        }
                        // StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick,
                        // lidarWorkerTracker);
                    } else {
                        eventsTODO.add(trackedObjectsEvent);
                        eventsTODO.stream().sorted(Comparator.comparingInt(TrackedObjectsEvent::getDetectionTime))
                                .collect(ConcurrentLinkedQueue::new, ConcurrentLinkedQueue::add,
                                        ConcurrentLinkedQueue::addAll);
                    } // current tick is less than due tick
                }
                if (eventsTODO.isEmpty() && lidarWorkerTracker.getStatus().equals(STATUS.DOWN)) {
                    System.out.println(
                            "[DETECTOBJECTSEVENT - NO EVENTS] - " + getName() + " has no more events to send");
                    sendBroadcast(new TerminatedBroadcast(getName()));
                    terminate();
                }
            } else { // if lidar is down
                System.out.println("[DETECTOBJECTSEVENT - ERROR] - " + getName() + " is down");
                sendBroadcast(new CrashedBroadcast("LidarWorker " + lidarWorkerTracker.getID() + " got an error",
                        "in LiDarService " + getName()));
                terminate();
            }
        });

        subscribeBroadcast(ZeroCamSensBroadcast.class, (ZeroCamSensBroadcast zero) -> {
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
            lidarWorkerTracker.setStatus(STATUS.DOWN);
        });
    }
}
