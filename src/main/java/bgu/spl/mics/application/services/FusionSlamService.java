package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.*;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 * 
 * This service receives TrackedObjectsEvents from LiDAR workers and PoseEvents
 * from the PoseService,
 * transforming and updating the map with new landmarks.
 */
public class FusionSlamService extends MicroService {
    private FusionSlam fusionSlam;

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global
     *                   map.
     */
    public FusionSlamService() {
        super("FusionSlamService");
        this.fusionSlam = FusionSlam.getInstance();
        // maybe add later
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents,
     * TickBroadcasts, TerminatedBroadcast, and
     * CrashedBroadcasts.
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {
            if (terminated.getTerminatorName().equals("TimeService")) {
                System.out
                        .println("FusionSlamService " + getName() + " terminated by " + terminated.getTerminatorName());
                sendBroadcast(new TerminatedBroadcast(getName()));
                terminate();
            } else if (terminated.getTerminatorName().equals("LiDarService")
                    || terminated.getTerminatorName().equals("CameraService")) {
                fusionSlam.decreaseSensor();
                if (fusionSlam.getNumberOfActiveSensors() == 0) {
                    System.out.println(
                            "FusionSlamService " + getName() + " terminated by " + terminated.getTerminatorName());
                    sendBroadcast(new TerminatedBroadcast(getName()));
                    terminate();
                }
            }
        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {
            sendBroadcast(new TerminatedBroadcast(getName()));
            terminate();
            StatisticalFolder.getInstance(); // update the statistical folder somehow
        });

        subscribeEvent(TrackedObjectsEvent.class, event -> {
            fusionSlam.analyzeObjects(event.getTrackedObjects());
            complete(event, true);
        });

        subscribeEvent(PoseEvent.class, event -> {
            fusionSlam.addPose(event.getPose());
            complete(event, true);
        });
    }

    public void registerSensor() {
        fusionSlam.increaseSensor();
    }
}
