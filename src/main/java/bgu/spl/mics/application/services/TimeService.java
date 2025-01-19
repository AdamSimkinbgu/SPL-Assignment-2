package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.TerminatedBroadcast;
import bgu.spl.mics.application.Messages.TickBroadcast;
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
        this.currentTick = 0;
        this.sleepTime = 1000 / TicksinSeconds;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified
     * duration.
     */
    @Override
    protected void initialize() {
        System.out.println(getName() + " started");
        do {
            sendBroadcast(new TickBroadcast(currentTick));
            System.out.println(getName() + " sent tick " + currentTick);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                System.out.println("TimeService was interrupted at tick " + currentTick);
                break;
            }
            currentTick++;
        } while (currentTick < TicksLifeSpan);
        sendBroadcast(new TerminatedBroadcast(getName()));
        System.out.println(getName() + " terminated");
        terminate();
        StatisticalFolder.getInstance().updateStatistics();
    }
}
