package com.signalfx.appd;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.appd.config.ConnectionConfig;
import com.signalfx.appd.config.SyncConfig;
import com.signalfx.appd.info.AppInfo;

public class AppD {

    protected static final Logger log = LoggerFactory.getLogger(AppD.class);
    protected static final SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

    public static final int MINUTE_MILLIS = 60 * 1000;

    public static void main(String[] args) {

        ConnectionConfig connectionConfig = ConnectionConfig.get();
        SyncConfig syncConfig = SyncConfig.get();

        if (syncConfig == null || connectionConfig == null) {
            return;
        }

        if (syncConfig.apps.isEmpty()) {
            log.error("There are no Apps found in config.");
            return;
        }
        int metricCount = 0;
        for (AppInfo app : syncConfig.apps) {
            metricCount += app.metrics.size();
        }
        if (metricCount == 0) {
            log.error("There are no metrics found in config.");
            return;
        }

        log.info("Starting AppDynamics sync of {} rule(s) at {} minute(s) interval",
                metricCount, syncConfig.interval);

        int intervalMillis = syncConfig.interval * MINUTE_MILLIS;

        SyncAppD syncAppD = new SyncAppD(syncConfig.apps, connectionConfig);
        long lastStart = System.currentTimeMillis();
        while (true) {
            long timeStart = System.currentTimeMillis();

            long timeDiff = timeStart - lastStart;
            long range = timeDiff / MINUTE_MILLIS + (timeDiff % MINUTE_MILLIS > 0 ? 1 : 0) + 1;
            lastStart = timeStart;
            log.trace("Starting at {} and querying for range {}", format.format(new Date(timeStart)), range);

            // Perform the actual stuff.
            syncAppD.perform(range);

            long timeEnd = System.currentTimeMillis();
            long timeTaken = timeEnd - timeStart;

            long sleepTime;
            if (timeTaken > intervalMillis) {
                log.warn("Took {} to process which is more than {} interval", timeTaken, intervalMillis);

                // Perform the next one at the next minute
                sleepTime = MINUTE_MILLIS - timeTaken % MINUTE_MILLIS;
            } else {
                sleepTime = intervalMillis - timeTaken;
            }

            log.trace("Took {}, sleeping for {}", timeTaken, sleepTime);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e){
                log.error("Sleep got interrupted");
                return;
            }
        }
    }
}
