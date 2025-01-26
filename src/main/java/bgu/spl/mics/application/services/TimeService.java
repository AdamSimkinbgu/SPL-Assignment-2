package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.TerminatedBroadcast;
import bgu.spl.mics.application.Messages.TickBroadcast;
import bgu.spl.mics.application.Messages.ZeroCamSensBroadcast;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * TimeService acts as the global timer for the system, broadcasting
 * TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {
    private int TicksinSeconds;
    private int TicksLifeSpan;
    private int currentTick;
    private int sleepTime;
    private StatisticalFolder sf;

    /**
     * Constructor for TimeService.
     *
     * @param TickTime The duration of each tick in milliseconds.
     * @param Duration The total number of ticks before the service terminates.
     */
    public TimeService(int TickTime, int Duration) {
        super("TimeService");
        this.TicksinSeconds = TickTime; // how many ticks there are in 1 second
        this.TicksLifeSpan = Duration; // how many ticks the service will broadcast
        this.currentTick = 1;
        this.sleepTime = 1000 / TicksinSeconds;
        this.sf = StatisticalFolder.getInstance();
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified
     * duration.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminat) -> {
            System.out.println("[TERMINATED] - " + getName() + " received ZeroCamSensBroadcast, terminating");
            StatisticalFolder.getInstance().setLastWorkTick(currentTick);
            sendBroadcast(new TerminatedBroadcast(getName()));
            terminate();
        });

        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            try {
                if (currentTick <= TicksLifeSpan && !sf.getSystemIsDone()) {
                    Thread.sleep(sleepTime);
                    System.out.println("[TICKBROADCAST RECEIVED] - " + getName() + " got tick " + tick.getTick());
                    currentTick++;
                    sendBroadcast(new TickBroadcast(currentTick));
                }
                else{
                    System.out.println("[TERMINATED] - " + getName() + " terminated because system done early from an error or reached max ticks");
                    sendBroadcast(new TerminatedBroadcast(getName()));
                    terminate();
                }
            } catch (InterruptedException e) {
                System.out.println("[INTERRUPTED] - " + "TimeService was interrupted at tick " + currentTick
                        + " with error: " + e.getMessage() + ", terminating");
                Thread.currentThread().interrupt();
                sendBroadcast(new TerminatedBroadcast(getName()));
                terminate();
            }
        });

        System.out.println("[INITIALIZING] - " + getName() + " started broadcasting ticks every " + sleepTime + "ms, "
                + TicksLifeSpan + " ticks total, starting tick " + currentTick);

        sendBroadcast(new TickBroadcast(currentTick));
    }
}
//        do {
//            try {
//                System.out.println("[TICKBROADCAST - SENT] - " + getName() + " sent tick " + currentTick);
//                sendBroadcast(new TickBroadcast(currentTick));
//                Thread.sleep(sleepTime);
//                currentTick++;
//                StatisticalFolder.getInstance().increaseSystemRuntime();
//            } catch (InterruptedException e) {
//                System.out.println("[INTERRUPTED] - " + "TimeService was interrupted at tick " + currentTick
//                        + " with error: " + e.getMessage() + ", terminating");
//                break;
//            }
//        } while (currentTick <= TicksLifeSpan && sf.getSystemIsDone() == false);
//        // sendBroadcast(new TerminatedBroadcast(getName()));
//        System.out.println("[TERMINATED] - " + getName() + " terminated");
//        terminate();
//        StatisticalFolder.getInstance().createOutput();
//    }

