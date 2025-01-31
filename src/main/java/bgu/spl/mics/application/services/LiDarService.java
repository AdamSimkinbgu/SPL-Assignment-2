package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.*;
import bgu.spl.mics.application.objects.*;

import java.util.Comparator;
import java.util.PriorityQueue;
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

// lidar 1 : furniture 1 at time 7 , furniture 2 at time 7, furniture 3 at time
// 7 and lidar freq is 2
// lidar 2 : wall 3 at time 4, wall 4 at time 4, wall 1 at time 4 and lidar freq
// is 4
// camera 1 : furniture 1,2,3 at time 7 is camera freq is 1
// camera 2 : got wall 1 at time 4, got wall 4 at time 4, got wall 3 at time 4
// and camera freq is 2
// their landmaks : wall 1,2,3,4 and chair_base 1 and circular_base 1 and door 1
public class LiDarService extends MicroService {
    private LiDarWorkerTracker lidarWorkerTracker;
    private PriorityQueue<TrackedObjectsEvent> eventsTODO;
    private int currTick;
    private boolean canTerminate;
    private int shouldTerminateAtTick;

    /**
     * Constructor for LiDarService.
     *
     * @param lidarWorkerTracker A LiDAR Tracker worker object that this service
     *                           will use to process data.
     */
    public LiDarService(LiDarWorkerTracker lidarWorkerTracker) {
        super("LiDarService" + lidarWorkerTracker.getID());
        this.lidarWorkerTracker = lidarWorkerTracker;
        this.eventsTODO = new PriorityQueue<>(Comparator.comparingLong(TrackedObjectsEvent::getDetectedTick));
        this.currTick = 0;
        this.canTerminate = false;
        this.shouldTerminateAtTick = -1;
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
            if (StatisticalFolder.getInstance().isSystemDown()) {
                return;
            }
            try {
                currTick++;
                System.out.println("[TICKBROADCAST RECEIVED] - " + getName() + " got tick " + tick.getTick());
                if (getShouldTerminateAtTick() >= currTick) {
                    if (!eventsTODO.isEmpty()) {
                        // synchronized (eventsTODO){
                        // iterate over each lidars priority queue events
                        // for (int key: lidareventsMap.keySet()) {
                        // PriorityQueue<TrackedObjectsEvent> lidarpq = lidareventsMap.get(key);
                        // while (!lidarpq.isEmpty() && lidarpq.peek().getDetectedTick() +
                        // lidarWorkerTracker.getFrequency() <= currTick) {
                        // TrackedObjectsEvent trackedObjectsEvent = lidarpq.poll();
                        // sendEvent(trackedObjectsEvent);
                        // complete(trackedObjectsEvent.getMyEvent(), true);
                        // lidarWorkerTracker.updateLastTrackedObjects(trackedObjectsEvent.getTrackedObjects());
                        // System.out.println("lidar " + lidarWorkerTracker.getID() + " got from tick "
                        // + trackedObjectsEvent.getDetectedTick() + " last tracked objects are: " +
                        // trackedObjectsEvent.toString());
                        // StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(
                        // trackedObjectsEvent.getDetectionTime(), lidarWorkerTracker);
                        // }
                        // }
                        for (TrackedObjectsEvent trackobjevent : eventsTODO) {
                            synchronized (trackobjevent) {
                                if (trackobjevent.getDetectedTick() + lidarWorkerTracker.getFrequency() <= currTick) {
                                    TrackedObjectsEvent trackedObjectsEvent = eventsTODO.poll();
                                    sendEvent(trackedObjectsEvent);
                                    complete(trackedObjectsEvent.getMyEvent(), true);
                                    lidarWorkerTracker
                                            .updateLastTrackedObjects(trackedObjectsEvent.getTrackedObjects());
                                    System.out.println("lidar " + lidarWorkerTracker.getID() + " got from tick "
                                            + trackedObjectsEvent.getDetectedTick() + " last tracked objects are: "
                                            + trackedObjectsEvent.toString());
                                    StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(
                                            trackedObjectsEvent.getDetectionTime(), lidarWorkerTracker);
                                }
                            }
                        }
                    }
                    crashLiDarBroadcast(currTick);
                } else {
                    if (lidarWorkerTracker.getStatus() == STATUS.UP) {
                        // if (lidarWorkerTracker.getShouldTerminateAtTick() > 0) {
                        // lidarFinalTickOrCurrTick = lidarWorkerTracker.getShouldTerminateAtTick();
                        // } else {
                        // lidarFinalTickOrCurrTick = currTick;
                        // }
                        // put here another check if the lidar is detected error the condition will be
                        // for getShouldTerminateAtTick else getDetectedTime
                        while (!eventsTODO.isEmpty()
                                && eventsTODO.peek().getDetectedTick() + lidarWorkerTracker.getFrequency() <= currTick
                                && eventsTODO.peek() != null) {
                            synchronized (eventsTODO.peek()) {
                                TrackedObjectsEvent trackedObjectsEvent = eventsTODO.poll();
                                sendEvent(trackedObjectsEvent);
                                complete(trackedObjectsEvent.getMyEvent(), true);
                                lidarWorkerTracker.updateLastTrackedObjects(trackedObjectsEvent.getTrackedObjects());
                                System.out.println("lidar " + lidarWorkerTracker.getID() + " got from tick "
                                        + trackedObjectsEvent.getDetectedTick() + " last tracked objects are: "
                                        + trackedObjectsEvent.toString());
                                StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(
                                        trackedObjectsEvent.getDetectionTime(), lidarWorkerTracker);
                            }
                        }
                        // for (int key: lidareventsMap.keySet()) {
                        // PriorityQueue<TrackedObjectsEvent> lidarpq = lidareventsMap.get(key);
                        // while (!lidarpq.isEmpty() && lidarpq.peek().getDetectedTick() +
                        // lidarWorkerTracker.getFrequency() <= currTick) {
                        // TrackedObjectsEvent trackedObjectsEvent = lidarpq.poll();
                        // sendEvent(trackedObjectsEvent);
                        // complete(trackedObjectsEvent.getMyEvent(), true);
                        // lidarWorkerTracker.updateLastTrackedObjects(trackedObjectsEvent.getTrackedObjects());
                        // System.out.println("lidar " + lidarWorkerTracker.getID() + " got from tick "
                        // + trackedObjectsEvent.getDetectedTick() + " last tracked objects are: " +
                        // trackedObjectsEvent.toString());
                        // StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(
                        // trackedObjectsEvent.getDetectionTime(), lidarWorkerTracker);
                        // }
                        // }
                        if (eventsTODO.isEmpty() && canTerminate) {
                            terminateLiDarBroadcast();
                        }
                        //
                    }
                }
            } catch (Exception e) {
                System.err.println("[TICKBROADCAST - CRITICAL ERROR] - " + getName() + " got error: " + e.getMessage());
                e.printStackTrace();
                crashLiDarBroadcast(tick.getTick());

            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {
            if (terminated.getTerminatorName().equals("TimeService")) {
                System.out.println("[TERMINATEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                        + terminated.getTerminatorName());
                lidarWorkerTracker.setStatus(STATUS.DOWN);
                sendBroadcast(new TerminatedBroadcast(getName()));
                terminate();
            }
        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {
            System.out.println("[CRASHEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                    + crashed.getCrasher());
            // checkIfUpdateBeforeCrash();
            // StatisticalFolder.getInstance().updateError("LidarWorker " +
            // lidarWorkerTracker.getID(),
            // lidarWorkerTracker.getErrorMsg());
            if (!StatisticalFolder.getInstance().getSystemIsDone()) {
                StatisticalFolder.getInstance().setSystemIsDone(true);
            }
            lidarWorkerTracker.setStatus(STATUS.DOWN);
            terminate();
        });

        subscribeEvent(DetectObjectsEvent.class, event -> { // here we send TrackedObjectEvents
            // synchronized (event) {
            System.out.println("[DETECTOBJECTSEVENT RECEIVED] - " + getName() + " got DetectObjectsEvent at tick "
                    + event.getDetectedTick() + " from " + event.getDetectorName());
            if (lidarWorkerTracker.getStatus() == STATUS.DOWN) {
                System.out.println("[ERROR - LiadrWorker] - " + getName() + " is down");
                terminateLiDarBroadcast();
            } else {
                ConcurrentLinkedQueue<TrackedObject> trackedObjects = lidarWorkerTracker
                        .calculateTrackedObjects(event.getStampedDetectedObjects(), event.getDetectedTick());
                if (lidarWorkerTracker.getShouldTerminateAtTick() > 0) {
                    this.shouldTerminateAtTick = lidarWorkerTracker.getShouldTerminateAtTick();
                }
                // if (lidarWorkerTracker.getStatus() == STATUS.ERROR) {
                // System.out.println("[ERROR - CRASHING] - " + getName() + " terminating with
                // error: "
                // + lidarWorkerTracker.getErrorMsg());
                // crashLiDarBroadcast(event.getDetectedTime());
                // } else {
                if (!event.getIsError()) {
                    if (event.getDetectedTick() + lidarWorkerTracker.getFrequency() <= currTick) {
                        lidarWorkerTracker.updateLastTrackedObjects(trackedObjects);
                        sendEvent(new TrackedObjectsEvent(trackedObjects, event.getDetectedTick(),
                                event.getHandledTick() + lidarWorkerTracker.getFrequency(), currTick, event));
                        StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(
                                currTick - lidarWorkerTracker.getFrequency(), lidarWorkerTracker);
                    } else {
                        // PriorityQueue<TrackedObjectsEvent> lidarpq =
                        // lidareventsMap.getOrDefault(lidarWorkerTracker.getID(), new
                        // PriorityQueue<>(Comparator.comparingLong(TrackedObjectsEvent::getDetectedTick)));
                        // lidarpq.add(new TrackedObjectsEvent(trackedObjects, event.getDetectedTick(),
                        // event.getHandledTick() + lidarWorkerTracker.getFrequency(), currTick,
                        // event));
                        if (!event.getInserted()) {
                            event.setInserted(true);
                            eventsTODO.add(new TrackedObjectsEvent(trackedObjects, event.getDetectedTick(),
                                    event.getHandledTick() + lidarWorkerTracker.getFrequency(), currTick, event));
                        }
                    }
                }
                complete(event, true);
            }
        });

        subscribeBroadcast(ZeroCamSensBroadcast.class, (ZeroCamSensBroadcast zero) -> {
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
            lidarWorkerTracker.setStatus(STATUS.DOWN);
        });
    }

    private int getShouldTerminateAtTick() {
        return shouldTerminateAtTick;
    }

    private void terminateLiDarBroadcast() {
        sendBroadcast(new TerminatedBroadcast(getName()));
        lidarWorkerTracker.setStatus(STATUS.DOWN);
        StatisticalFolder.getInstance().setSystemIsDone(true);
        // StatisticalFolder.getInstance().setLastWorkTick(currTick);
        terminate();
    }

    private void crashLiDarBroadcast(int currTick) {
        sendBroadcast(new CrashedBroadcast("LidarWorker " + lidarWorkerTracker.getID() + " got an error",
                "LiDarService " + getName(), currTick));
        // maybe add handle crash method from the tracker
        lidarWorkerTracker.setStatus(STATUS.ERROR);
        StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick, lidarWorkerTracker);
        StatisticalFolder.getInstance().updateError("LidarWorker " + lidarWorkerTracker.getID(),
                lidarWorkerTracker.getErrorMsg());
        if (!StatisticalFolder.getInstance().getSystemIsDone()) {
            StatisticalFolder.getInstance().setSystemIsDone(true);
        }
        terminate();
    }
}

// int currTick = event.getDetectedTime();
// int dueTick = currTick + lidarWorkerTracker.getFrequency();
// if (lidarWorkerTracker.getStatus() == STATUS.UP) {
// // get the detected objects and time they were detected
// StampedDetectedObjects detectedObjects = event.getStampedDetectedObjects();
// // check if was an error in detections of objects and turning the service to
// // down if so
// ConcurrentLinkedQueue<TrackedObject> trackedObjects = lidarWorkerTracker
// .calculateTrackedObjects(detectedObjects);
// if (lidarWorkerTracker.getStatus().equals(STATUS.ERROR)) {
// System.out.println("[ERROR - CRASHING] - " + getName() + " terminating with
// error: "
// + "Failed to detect objects");
// sendBroadcast(new CrashedBroadcast("LidarWorker " +
// lidarWorkerTracker.getID() + " got an error",
// "in LiDarService " + getName()));
// terminate();
// //
// StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick,
// // lidarWorkerTracker);
// } else {
// // create TrackedObjectsEvent with previous calculated tracked objects
// TrackedObjectsEvent trackedObjectsEvent = new
// TrackedObjectsEvent(trackedObjects, currTick, dueTick,
// event);
// // checks if detection tick + lidar frequency is less than current tick so we
// // can send event
// if (dueTick <= currTick) {
// // update last tracked objects for a future error
// Future<?> future = sendEvent(trackedObjectsEvent);
//
// StatisticalFolder.getInstance().addNumTrackedObjects(trackedObjects.size());
//
// if (future == null) {
// System.err.println("[ERROR - CRASHING] - " + getName() + " terminating with
// error: "
// + "Failed to send TrackedObjectsEvent");
// lidarWorkerTracker.setStatus(STATUS.ERROR);
// } else {
// System.out.println("[DETECTOBJECTSEVENT - SENT] - " + getName()
// + " sent TrackedObjectsEvent at tick " + dueTick);
// complete(event, true);
// lidarWorkerTracker.updateLastTrackedObjects(trackedObjects);
// }
// //
// StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick,
// // lidarWorkerTracker);
// } else {
// eventsTODO.add(trackedObjectsEvent);
// // sort the events by detection time
// eventsTODO.stream().sorted(Comparator.comparingInt(TrackedObjectsEvent::getDetectionTime))
// .collect(ConcurrentLinkedQueue::new, ConcurrentLinkedQueue::add,
// ConcurrentLinkedQueue::addAll);
// } // current tick is less than due tick
// }
// if (eventsTODO.isEmpty() &&
// lidarWorkerTracker.getStatus().equals(STATUS.DOWN)) {
// System.out.println(
// "[DETECTOBJECTSEVENT - NO EVENTS] - " + getName() + " has no more events to
// send");
// sendBroadcast(new TerminatedBroadcast(getName()));
// terminate();
// }
// } else { // if lidar is down
// System.out.println("[DETECTOBJECTSEVENT - ERROR] - " + getName() + " is
// down");
// sendBroadcast(new CrashedBroadcast("LidarWorker " +
// lidarWorkerTracker.getID() + " got an error",
// "in LiDarService " + getName()));
// StatisticalFolder.getInstance().updateError(
// "LidarWorker " + lidarWorkerTracker.getID() + " got an error",
// "LiDarService " + getName());
// terminate();
// }
// lidar 1 = 1362
// lidar 2 = 1461