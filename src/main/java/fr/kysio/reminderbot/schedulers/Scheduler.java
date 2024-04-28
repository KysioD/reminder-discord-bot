package fr.kysio.reminderbot.schedulers;

import discord4j.core.GatewayDiscordClient;
import io.sentry.Sentry;

import java.util.logging.Logger;

public abstract class Scheduler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Scheduler.class.getName());
    private final String name;
    private final long startingDelay;
    private final long delay;

    public Scheduler(String name, long startingDelay, long delay) {
        this.name = name;
        this.startingDelay = startingDelay;
        this.delay = delay;
    }

    public String getName() {
        return name;
    }

    public long getDelay() {
        return delay;
    }

    public void start() {
        LOGGER.info("Starting " + name + " scheduler");
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(startingDelay);
                LOGGER.info("Running scheduler " + name);
                long time = System.currentTimeMillis();
                process();
                time = System.currentTimeMillis() - time;
                LOGGER.info("Scheduler " + name + " finished after " + time + "ms");
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }
    }

    protected abstract void process();
}
