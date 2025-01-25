package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.*;
import bgu.spl.mics.application.objects.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
            if (StatisticalFolder.getInstance().isSystemDown()) {
                return;
            }
            try {
                System.out.println("[TICKBROADCAST RECEIVED] - " + getName() + " got tick " + tick.getTick());
                int currTick = tick.getTick();
                if (lidarWorkerTracker.getStatus() == STATUS.UP) {
                    if (!eventsTODO.isEmpty()) {
                        if (eventsTODO.peek().getDetectionTime() <= currTick) {
                            TrackedObjectsEvent trackedObjectsEvent = eventsTODO.poll();
                            Future<?> future = sendEvent(trackedObjectsEvent);
                            // if (future.get(currTick + lidarWorkerTracker.getFrequency(),
                            // TimeUnit.SECONDS) == null)
                            // crashLiDarBroadcast(currTick);

                            complete(trackedObjectsEvent.getMyEvent(), true);
                            lidarWorkerTracker.updateTrackedObjectsByTime(trackedObjectsEvent.getTrackedObjects());
                            // lidarWorkerTracker.updateLastTrackedObjects(trackedObjectsEvent.getTrackedObjects());
                            StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick,
                                    lidarWorkerTracker);
                            StatisticalFolder.getInstance()
                                    .addNumTrackedObjects(trackedObjectsEvent.getTrackedObjects().size());

                        }
                    }
                    if (eventsTODO.isEmpty() && lidarWorkerTracker.getStatus().equals(STATUS.DOWN)) {
                        terminateLiDarBroadcast();
                        StatisticalFolder.getInstance().setSystemIsDone(true);
                        StatisticalFolder.getInstance().setLastWorkTick(currTick);
                    }
                }
            } catch (Exception e) {
                System.err.println("[TICKBROADCAST - CRITICAL ERROR] - " + getName() + " got error: " + e.getMessage());
                e.printStackTrace();
                crashLiDarBroadcast(tick.getTick());

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
            // lidarWorkerTracker.updateLastTrackedObjects(lidarWorkerTracker.getLastTrackedObject());
            lidarWorkerTracker.setStatus(STATUS.DOWN);
            sendBroadcast(new TerminatedBroadcast(getName()));
            terminate();
            StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(crashed.getTimeCrashed(),
                    lidarWorkerTracker);
            StatisticalFolder.getInstance()
                    .addNumTrackedObjects(lidarWorkerTracker.getTrackedObjectsByTime(crashed.getTimeCrashed()).size());
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
                ConcurrentLinkedQueue<TrackedObject> trackedObjects = lidarWorkerTracker
                        .processDetectedObjects(event, currTick);
                if (lidarWorkerTracker.getStatus().equals(STATUS.ERROR)) {
                    crashLiDarBroadcast(currTick);
                    StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick,
                            lidarWorkerTracker);
                } else {
                    // create TrackedObjectsEvent with previous calculated tracked objects
                    TrackedObjectsEvent trackedObjectsEvent = new TrackedObjectsEvent(trackedObjects, currTick, dueTick,
                            event);
                    // checks if detection tick + lidar frequency is less than current tick so we
                    // can send event
                    if (dueTick <= currTick) {
                        // update last tracked objects for a future error
                        Future<?> future = sendEvent(trackedObjectsEvent);

                        System.out.println("[TRACKEDOBJECTSEVENT - SENT] - " + getName()
                                + " sent TrackedObjectsEvent at tick " + dueTick);
                        complete(event, true);
                        // lidarWorkerTracker.updateLastTrackedObjects(trackedObjects);

                        StatisticalFolder.getInstance().addNumTrackedObjects(trackedObjects.size());
                        StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick,
                                lidarWorkerTracker);

                    } else {
                        synchronized (eventsTODO) {
                            eventsTODO.add(trackedObjectsEvent);
                            // sort the events by detection time
                            List<TrackedObjectsEvent> sortedList = eventsTODO.stream()
                                    .sorted(Comparator.comparingInt(TrackedObjectsEvent::getDetectionTime))
                                    .collect(Collectors.toList());
                            eventsTODO.clear();
                            eventsTODO.addAll(sortedList);
                        }
                    } // current tick is less than due tick
                }
                if (eventsTODO.isEmpty() && lidarWorkerTracker.getStatus().equals(STATUS.DOWN)) {

                    terminateLiDarBroadcast();
                }
            } else

            { // if lidar is down
              // System.out.println("[DETECTOBJECTSEVENT - ERROR] - " + getName() + " is
              // down");
              // sendBroadcast(new CrashedBroadcast("LidarWorker " +
              // lidarWorkerTracker.getID() + " got an error",
              // "in LiDarService " + getName()));
              // StatisticalFolder.getInstance().updateError(
              // "LidarWorker " + lidarWorkerTracker.getID() + " got an error",
              // "LiDarService " + getName());
              // terminate();
                System.out.println("[DETECTOBJECTSEVENT - ERROR] - " + getName() + " is down");
                crashLiDarBroadcast(currTick);
            }
        });

        subscribeBroadcast(ZeroCamSensBroadcast.class, (
                ZeroCamSensBroadcast zero) -> {
            System.out.println("[ZEROCAMSENSBROADCAST RECEIVED] - " + getName() + " got ZeroCamSensBroadcast");
            if (zero.getActiveCameras() == 0) {
                System.out.println("[ZEROCAMSENSBROADCAST - TERMINATING] - " + getName() + " terminating because "
                        + "all cameras are inactive");
            } else if (zero.getActiveSensors() == 0) {
                System.out.println("[ZEROCAMSENSBROADCAST - TERMINATING] - " + getName() + " terminating because "
                        + "all sensors are inactive");
            }
            sendBroadcast(new TerminatedBroadcast(getName()));
            lidarWorkerTracker.setStatus(STATUS.DOWN);
            terminate();
        });
    }

    private void terminateLiDarBroadcast() {
        System.out.println("[TERMINATED] - " + getName() + " terminating because all sensors are inactive");
        sendBroadcast(new TerminatedBroadcast(getName()));
        lidarWorkerTracker.setStatus(STATUS.DOWN);
        terminate();
    }

    private void crashLiDarBroadcast(int currTick) {
        sendBroadcast(new CrashedBroadcast("LidarWorker " + lidarWorkerTracker.getID() + " got an error",
                "LiDarService " + getName(), currTick));
        lidarWorkerTracker.setStatus(STATUS.ERROR);
        StatisticalFolder.getInstance().updateError("LidarWorker " + lidarWorkerTracker.getID(),
                lidarWorkerTracker.getErrorMsg());
        StatisticalFolder.getInstance().updatelastLiDarWorkerTrackerFrame(currTick, lidarWorkerTracker);
        terminate();
    }
}
