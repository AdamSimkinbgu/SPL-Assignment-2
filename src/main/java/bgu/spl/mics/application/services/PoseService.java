package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.CrashedBroadcast;
import bgu.spl.mics.application.Messages.PoseEvent;
import bgu.spl.mics.application.Messages.TerminatedBroadcast;
import bgu.spl.mics.application.Messages.TickBroadcast;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * PoseService is responsible for maintaining the robot's current pose (position
 * and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {
    private GPSIMU gpsimu;

    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu) {
        super("PoseService");
        this.gpsimu = gpsimu;
    }

    /**
     * Initializes the PoseService.
     * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the
     * current pose.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            int currTick = tick.getTick();
            gpsimu.setCurrentTick(currTick);
            System.out.println(getName() + " got tickBroadcast at tick " + currTick);
            Pose pose = gpsimu.getPoseAtTick(currTick);
            if (gpsimu.getStatus() == STATUS.UP) {
                if (pose != null) {
                    System.out.println(getName() + " sending an event about the pose at tick " + currTick);
                    sendEvent(new PoseEvent(getName(), pose));
                    return;  // not sure if this is needed
                }
                if (gpsimu.getStatus() == STATUS.DOWN) {
                    System.err.println(getName() + " is down");
                    sendBroadcast(new TerminatedBroadcast(getName()));
                    terminate();
                    StatisticalFolder.getInstance().updatePoses(gpsimu.getPoses());
                }
            } else {
                System.err.println(getName() + " is down");
                sendBroadcast(new TerminatedBroadcast(getName()));
                terminate();
                StatisticalFolder.getInstance().updatePoses(gpsimu.getPoses());
            }
        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {
            if (crashed.getCrasher().equals(getName())) {
                System.err.println(getName() + " crashed with error: " + crashed.getErrorMsg());
                sendBroadcast(new CrashedBroadcast("GPSIMU crashed", getName())); // im not sure what error message can be in this case
                terminate();
                StatisticalFolder.getInstance().updatePoses(gpsimu.getPoses());
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {
            if (terminated.getTerminatorName().equals(getName())) {
                System.err.println(getName() + " terminated");
                sendBroadcast(new TerminatedBroadcast(getName()));                terminate();
                StatisticalFolder.getInstance().updatePoses(gpsimu.getPoses());
            }
        });

    }
}
