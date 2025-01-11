package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.*;
import bgu.spl.mics.application.objects.FusionSlam;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 * 
 * This service receives TrackedObjectsEvents from LiDAR workers and PoseEvents from the PoseService,
 * transforming and updating the map with new landmarks.
 */
public class FusionSlamService extends MicroService {
    private FusionSlam fusionSlam;

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlamService");
        this.fusionSlam = fusionSlam;
        // maybe add later
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, TickBroadcasts, TerminatedBroadcast, and
     * CrashedBroadcasts.
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {
        System.out.println("FusionSlamService started");

        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {

        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {

        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {

        });

        subscribeEvent(TrackedObjectsEvent.class, event -> {

        });

        subscribeEvent(PoseEvent.class, event -> {

        });
    }
}
