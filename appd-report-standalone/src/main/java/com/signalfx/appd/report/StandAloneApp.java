/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.report;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.signalfx.appd.client.MetricDataRequest;
import com.signalfx.appd.process.ReportAppD;
import com.signalfx.appd.report.config.Config;
import com.signalfx.appd.report.config.ConnectionConfig;
import com.signalfx.appd.process.info.AppInfo;
import com.signalfx.codahale.reporter.SignalFxReporter;

/**
 * StandAloneApp is a stand alone process that reports AppDynamics metrics to SignalFx.
 *
 * It use configurations specified in property file/parameters or environment variables.
 *
 * Property Parameters
 *    (Required)
 *    com.signalfx.appd.username - AppDynamics username
 *    com.signalfx.appd.password - AppDynamics password
 *    com.signalfx.appd.host - AppDynamics host
 *    com.signalfx.api.token - SignalFx token
 *
 *    (Optional)
 *    com.signalfx.appd.metrics - metric configurations filename (default to metrics.json)
 *    com.signalfx.appd.interval - time in minutes of metric lookup interval (default to 1 minute)
 *
 * Environment Variables
 *    (Required)
 *    APPD_USERNAME - AppDynamics username
 *    APPD_PASSWORD - AppDynamics password
 *    APPD_HOST - AppDynamics host
 *    SIGNALFX_TOKEN- SignalFx token
 *
 *    (Optional)
 *    SIGNALFX_APPD_METRICS - metric configurations filename (default to metrics.json)
 *    APPD_INTERVAL - time in minutes of metric lookup interval (default to 1 minute)
 *
 * It also uses metric configuration json file to perform query of metrics from AppDynamics and
 * do the mapping to metric names/dimensions in SignalFx.
 *
 * @author 9park
 */
public class StandAloneApp {
    protected static final Logger log = LoggerFactory.getLogger(StandAloneApp.class);
    protected static final SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

    public static final int MINUTE_MILLIS = 60 * 1000;

    public static void main(String[] args) {
        ConnectionConfig connectionConfig = Config.getConnectionConfig();

        List<AppInfo> apps = Config.getSyncConfig();

        if (apps == null || connectionConfig == null) {
            return;
        }

        if (apps.isEmpty()) {
            log.error("There are no Apps found in config.");
            return;
        }
        int metricCount = 0;
        for (AppInfo app : apps) {
            metricCount += app.metrics.size();
        }
        if (metricCount == 0) {
            log.error("There are no metrics found in config.");
            return;
        }

        int interval = Config.getInterval();

        log.info("Starting AppDynamics sync of {} rule(s) at {} minute(s) interval",
                metricCount, interval);

        MetricRegistry metricRegistry = new MetricRegistry();
        SignalFxReporter signalFxReporter =
                new SignalFxReporter.Builder(metricRegistry, connectionConfig.fxToken).build();
        signalFxReporter.start(1, TimeUnit.SECONDS);

        Injector injector =
                Guice.createInjector(new AppDReportModule(connectionConfig, metricRegistry));

        ReportAppD syncAppD = injector.getInstance(ReportAppD.class);

        int intervalMillis = interval * MINUTE_MILLIS;
        long lastStart = System.currentTimeMillis();
        while (true) {
            long timeStart = System.currentTimeMillis();

            long timeDiff = timeStart - lastStart;
            long range = timeDiff / MINUTE_MILLIS + (timeDiff % MINUTE_MILLIS > 0 ? 1 : 0) + 1;
            lastStart = timeStart;
            log.trace("Starting at {} and querying for range {}",
                    format.format(new Date(timeStart)), range);

            // Perform the actual stuff.
            syncAppD.perform(apps, MetricDataRequest.TimeParams.beforeNow(range));

            long timeEnd = System.currentTimeMillis();
            long sleepTime;
            long timeTaken = timeEnd - timeStart;

            if (timeTaken > intervalMillis) {
                log.warn("Took {} to process which is more than {} interval", timeTaken,
                        intervalMillis);

                // Perform the next one at the next minute
                sleepTime = MINUTE_MILLIS - timeTaken % MINUTE_MILLIS;
            } else {
                sleepTime = intervalMillis - timeTaken;
            }

            log.trace("Took {}, sleeping for {}", timeTaken, sleepTime);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                log.error("Sleep got interrupted");
                return;
            }
        }
    }
}
