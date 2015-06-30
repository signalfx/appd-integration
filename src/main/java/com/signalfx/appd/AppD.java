package com.signalfx.appd;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.appd.config.ConnectionConfig;
import com.signalfx.appd.info.AppInfo;
import com.signalfx.appd.info.MetricInfo;

public class AppD {

    protected static final Logger log = LoggerFactory.getLogger(AppD.class);
    protected static final SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

    public static final int MINUTE_MILLIS = 60 * 1000;

    public static void main(String[] args) {
        // Read connection configs
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("config.properties"));
        } catch (FileNotFoundException e) {
            log.error("config.properties not found");
            return;
        } catch (IOException e) {
            log.error("error loading config.properties");
            return;
        }
        final String appdUsername = prop.getProperty("appd.username");
        final String appdPassword = prop.getProperty("appd.password");
        final String appdHost = prop.getProperty("appd.host");
        final String signalfxToken = prop.getProperty("signalfx.token");
        int intervalMinutes = Integer.parseInt(prop.getProperty("interval", "1"));

        if (intervalMinutes < 1) {
            log.warn("Interval is less than 1 minute minimum, setting to 1 minute");
            intervalMinutes = 1;
        }

        ConnectionConfig connectionConfig = new ConnectionConfig(appdUsername, appdPassword, appdHost,
                signalfxToken);

        // Read metrics config
        List<AppInfo> apps = getAppConfig();
        if (apps == null) {
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
        }

        log.info("Starting AppDynamics sync of {} metric(s) at {} minute(s) interval",
                metricCount, intervalMinutes);

        int intervalMillis = intervalMinutes * MINUTE_MILLIS;

        SyncAppD syncAppD = new SyncAppD(apps, connectionConfig);
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

    private static List<AppInfo> getAppConfig() {
        List<AppInfo> apps = new LinkedList<>();
        String metricConfigString;
        try {
            metricConfigString = new String(Files.readAllBytes(Paths.get("metrics.json")),
                    StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            log.error("metrics.json not found");
            return null;
        } catch (IOException e) {
            log.error("error reading metrics.json: {}");
            return null;
        }

        try {
            JSONArray configJson = new JSONArray(metricConfigString);
            for (int i = 0; i < configJson.length(); i++) {
                JSONObject appJson = configJson.getJSONObject(i);
                AppInfo appInfo = new AppInfo(appJson.getString("name"));
                apps.add(appInfo);
                JSONArray metricsJson = appJson.getJSONArray("metrics");
                for (int j = 0; j < metricsJson.length(); j++) {
                    JSONObject metricJson = metricsJson.getJSONObject(j);
                    String metricPath = metricJson.getString("metric_path");
                    String metricName = metricJson.has("metric_name") ?
                            metricJson.getString("metric_name") : null;
                    Map<String, String> dimensions;
                    if (metricJson.has("dimensions")) {
                        dimensions = new HashMap<>();
                        JSONObject dimensionsJson = metricJson.getJSONObject("dimensions");
                        for (Object key : dimensionsJson.keySet()) {
                            String keyString = key.toString();
                            dimensions.put(keyString, dimensionsJson.getString(keyString));
                        }
                    } else {
                        dimensions = null;
                    }
                    appInfo.metrics.add(new MetricInfo(metricPath, metricName, dimensions));
                }
            }
        } catch (JSONException e) {
            log.error("Error parsing metrics.json: {}", e.getMessage());
            return null;
        }
        return apps;
    }
}
