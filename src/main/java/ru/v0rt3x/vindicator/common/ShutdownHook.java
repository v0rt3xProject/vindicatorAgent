package ru.v0rt3x.vindicator.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.agent.VindicatorAgent;

public class ShutdownHook implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);

    @Override
    public void run() {
        VindicatorAgent.getInstance().shutdown();

        while (!VindicatorAgent.getInstance().isReadyForShutDown()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Unable to shutdown properly", e);
            }
        }
    }
}
