package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.CrashedBroadcast;
import bgu.spl.mics.application.Messages.DetectObjectsEvent;
import bgu.spl.mics.application.Messages.TerminatedBroadcast;
import bgu.spl.mics.application.Messages.TickBroadcast;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.ArrayList;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {
    private LiDarWorkerTracker LiDarWorkerTracker;
    /**
     * Constructor for LiDarService.
     *
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker LiDarWorkerTracker) {
        super("LiDarService");
        this.LiDarWorkerTracker = LiDarWorkerTracker;
        // maybe more added later
    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     * subscribes to  TickBroadcast, TerminatedBroadcast, CrashedBroadcast, DetectObjectsEvent
     * Tick need current time
     * Terminated needs nothing just the lidar name (getname())
     * Crashed needs errormsg and lidar name
     * DetectObjectsEvent needs lidar name, time that the stampedDetectedObjects was detected,
     * and the stampedDetectedObjects itself
     */
    @Override
    protected void initialize() {
        System.out.println("LiDarService started");
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            int currTick = tick.getTick();
            int dueTick = currTick + LiDarWorkerTracker.getFrequency();
            System.out.println("LiDarService " + getName() + " got tick " + currTick);

        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {

        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {

        });

        subscribeEvent(DetectObjectsEvent.class, event -> { // here we send TrackedObjectEvents
            int currTick = event.getSentTime();
            int dueTick = currTick + LiDarWorkerTracker.getFrequency();
            System.out.println("LiDarService " + getName() + " got tick " + currTick);
            if (LiDarWorkerTracker.getStatus() == STATUS.UP) {
                // process data
                // send TrackedObjectsEvent
                ArrayList<TrackedObject> trackedObjects = LiDarWorkerTracker.processData(event.getStampedDetectedObjects());
            }
        });
    }
}
