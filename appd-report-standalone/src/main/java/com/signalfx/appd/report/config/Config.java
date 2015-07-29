/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.report.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.appd.process.info.AppInfo;
import com.signalfx.appd.process.info.InfoParser;

/**
 * Config handles retrieval of configurations of the AppDynamics SignalFx metric reporting
 *
 * @author 9park
 */
public class Config {

    protected static final Logger log = LoggerFactory.getLogger(Config.class);

    /**
     * Retrieve configurations by looking up in property param first then environment variable.
     *
     * @return ConnectionConfig containing AppDynamics URL, Username,Password and SignalFx Token.
     */
    public static ConnectionConfig getConnectionConfig() {
        boolean isValid = true;
        String appdUsername = getPropertyOrEnv("com.signalfx.appd.username", "APPD_USERNAME");
        if (StringUtils.isEmpty(appdUsername)) {
            log.error("AppDynamics username not specified.");
            isValid = false;
        }
        String appdPassword = getPropertyOrEnv("com.signalfx.appd.password", "APPD_PASSWORD");
        if (StringUtils.isEmpty(appdPassword)) {
            log.error("AppDynamics password not specified.");
            isValid = false;
        }
        String appdURL = getPropertyOrEnv("com.signalfx.appd.host", "APPD_HOST");
        if (StringUtils.isEmpty(appdURL)) {
            log.error("AppDynamics host not specified.");
            isValid = false;
        }
        String fxToken = getPropertyOrEnv("com.signalfx.api.token", "SIGNALFX_TOKEN");
        if (StringUtils.isEmpty(fxToken)) {
            log.error("SignalFx token not specified.");
            isValid = false;
        }
        if (isValid) {
            return new ConnectionConfig(appdUsername, appdPassword, appdURL, fxToken);
        } else {
            return null;
        }
    }

    /**
     * @return path of metrics json configuration for querying AppDynamics.
     */
    public static String getMetricJSONPath() {
        String path = getPropertyOrEnv("com.signalfx.appd.metrics",
                "SIGNALFX_APPD_METRICS");
        if (StringUtils.isEmpty(path)) {
            path = "metrics.json";
        }
        return path;
    }

    /**
     * @return interval (in minutes) which the process should retrieve the data range for.
     */
    public static int getInterval() {
        String intervalString = getPropertyOrEnv("com.signalfx.appd.interval", "APPD_INTERVAL");
        int interval;
        if (StringUtils.isEmpty(intervalString)) {
            log.warn("Interval config default to 1");
            interval = 1;
        } else {
            try {
                interval = Integer.parseInt(intervalString);
            } catch (NumberFormatException e) {
                log.warn("Invalid interval config {}, default to 1", intervalString);
                interval = 1;
            }
        }
        if (interval < 1) {
            log.warn("Interval is less than 1 minute minimum, setting to 1 minute");
            interval = 1;
        }
        return interval;
    }

    public static String getPropertyOrEnv(String propertyName, String envName) {
        return System.getProperty(propertyName, System.getenv(envName));
    }

    /**
     * @return list of {@link AppInfo} configurations that the process should query from
     * AppDynamics.
     */
    public static List<AppInfo> getSyncConfig() {
        String metricJsonPath = getMetricJSONPath();
        try {
            return InfoParser.parseInfo(new String(Files.readAllBytes(Paths.get(metricJsonPath)),
                    StandardCharsets.UTF_8));
        } catch (NoSuchFileException e) {
            log.error("{} not found", metricJsonPath);
        } catch (IOException e) {
            log.error("error reading {}: {}", metricJsonPath, e.getCause());
        }
        return null;
    }
}
