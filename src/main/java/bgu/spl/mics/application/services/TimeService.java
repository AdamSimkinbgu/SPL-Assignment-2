package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.Messages.TickBroadcast;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
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
        this.TicksinSeconds = TickTime; //how many ticks there are in 1 second
        this.TicksLifeSpan = Duration; // how many ticks the service will broadcast
        this.currentTick = 0;
        this.sleepTime = 1000 / TicksinSeconds;

    }


    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() {
        Thread timer = new Thread(() -> {
        while (currentTick != TicksLifeSpan) {
            try {
                sendBroadcast(new TickBroadcast(currentTick));
                currentTick++;
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        this.terminate();
        });
        timer.start();
    }
}
