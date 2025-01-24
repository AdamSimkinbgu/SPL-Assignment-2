package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
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
        System.out.println("[INITIALIZING] - " + getName() + " started");
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            System.out.println("[TICKBROADCAST RECEIVED] - " + getName() + " got tick " + tick.getTick());
            try {
                int currTick = tick.getTick();
                Pose pose = gpsimu.getPoseAtTick(currTick);
                if (pose != null) {
                    PoseEvent poseEvent = new PoseEvent(getName(), pose);
                    Future<?> future = sendEvent(poseEvent);
                    // if (future.get() == null) {
                    // poseCrashBroadcast(currTick);
                    // }
                } else {
                    poseCrashBroadcast(currTick);
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println("[TICKBROADCAST - INVALID]" + "PoseService: Invalid tick " + e.getMessage());
            } catch (Exception e) {
                // System.err.println(
                // "[TICKBROADCAST - CRITICAL ERROR]" + "PoseService: Error processing tick " +
                // e.getMessage());
                // e.printStackTrace();
                // StatisticalFolder.getInstance().updatePoses(gpsimu.getPoses()); // should
                // update a variable in the
                // // statistical folder instead
                // sendBroadcast(new CrashedBroadcast("PoseService crashed", getName(),
                // tick.getTick()));
                // terminate();
                poseCrashBroadcast(tick.getTick());
            }
        });

        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {
            System.out.println("[CRASHEDBROADCAST RECEIVED] - " + getName() + " got crashed broadcast from "
                    + crashed.getCrasher());
            gpsimu.setStatus(STATUS.DOWN);
            sendBroadcast(new TerminatedBroadcast(getName()));
            StatisticalFolder.getInstance().updatePoses(gpsimu.getPoses());
            terminate();
            // update poses statistical folder

        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {
            System.out.println("[TERMINATEDBROADCAST RECEIVED] - " + getName() + " got terminated broadcast from "
                    + terminated.getTerminatorName());
            gpsimu.setStatus(STATUS.DOWN);
            sendBroadcast(new TerminatedBroadcast(getName()));
            StatisticalFolder.getInstance().updatePoses(gpsimu.getPoses());
            terminate();
        });
    }

    private void poseCrashBroadcast(int currTick) {
        sendBroadcast(new CrashedBroadcast("PoseService crashed at tick " + currTick, getName(), currTick));
        gpsimu.setStatus(STATUS.ERROR);
        terminate();
    }
}
