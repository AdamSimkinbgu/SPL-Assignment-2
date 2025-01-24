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
        System.out.println("[INITIALIZING] - " + getName() + " started");
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {
            System.out.println("[TERMINATED] - " + getName() + " terminated by " + terminated.getTerminatorName());
            StatisticalFolder.getInstance().updateLandmarks(fusionSlam.getLandmarks());
            if (terminated.getTerminatorName().equals("TimeService")) {
                sendBroadcast(new TerminatedBroadcast(getName()));
                terminate();
            } else if (terminated.getTerminatorName().contains("LiDarService")) {
                fusionSlam.decreaseSensor();

            } else if (terminated.getTerminatorName().contains("CameraService")) {
                fusionSlam.decreaseCameras();
            }
            if (fusionSlam.getNumberOfActiveSensors() == 0 || fusionSlam.getNumberOfActiveCameras() == 0) {
                System.out.println(
                        "[TERMINATED] - " + getName()
                                + " terminated because all sensors are inactive, should updating landmarks?");
                sendBroadcast(new ZeroCamSensBroadcast(fusionSlam.getNumberOfActiveSensors(),
                        fusionSlam.getNumberOfActiveCameras()));
                terminate();
            }
        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crash) -> {
            if (crash.getCrasher().contains("LiDarService")) {
                fusionSlam.decreaseSensor();
            } else if (crash.getCrasher().contains("CameraService")) {
                fusionSlam.decreaseCameras();
            }
            if (fusionSlam.getNumberOfActiveSensors() == 0 || fusionSlam.getNumberOfActiveCameras() == 0) {
                System.out.println(
                        "[TERMINATED] - " + getName()
                                + " terminated because all sensors are inactive, should updating landmarks?");
                sendBroadcast(new ZeroCamSensBroadcast(fusionSlam.getNumberOfActiveSensors(),
                        fusionSlam.getNumberOfActiveCameras()));
                terminate();
            }
        });

        subscribeEvent(TrackedObjectsEvent.class, event -> {
            System.out.println("[TRACKEDOBJECTSEVENT RECEIVED] - " + getName() + " got TrackedObjectsEvent");
            fusionSlam.analyzeObjects(event.getTrackedObjects());
            complete(event, true);
            StatisticalFolder.getInstance().setNumLandmarks(fusionSlam.getLandmarks().size());
        });

        subscribeEvent(PoseEvent.class, event -> {
            System.out.println("[POSEEVENT RECEIVED] - " + getName() + " got PoseEvent");
            fusionSlam.addPose(event.getPose());
            complete(event, true);
            StatisticalFolder.getInstance().updatePoses(fusionSlam.getPoses());
        });
    }
}
