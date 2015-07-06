package com.signalfx.appd.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.appd.info.AppInfo;
import com.signalfx.appd.info.MetricInfo;

public class SyncConfig {

    protected static final Logger log = LoggerFactory.getLogger(SyncConfig.class);

    public final int interval;
    public final List<AppInfo> apps;

    private SyncConfig(int interval, List<AppInfo> apps) {
        this.interval = interval;
        this.apps = apps;
    }

    public static SyncConfig get() {
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
            List<AppInfo> apps = new LinkedList<>();
            JSONObject configJson = new JSONObject(metricConfigString);
            int interval = configJson.getInt("interval");
            JSONArray appsJson = configJson.getJSONArray("apps");
            for (int i = 0; i < appsJson.length(); i++) {
                JSONObject appJson = appsJson.getJSONObject(i);
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
                    if (metricPath.contains("*") && metricName != null) {
                        log.warn("Found metric path \"{}\" with * and metric name \"{}\". Metric name will be ignored.", metricPath, metricName);
                    }
                    appInfo.metrics.add(new MetricInfo(metricPath, metricName, dimensions));
                }
            }

            if (interval < 1) {
                log.warn("Interval is less than 1 minute minimum, setting to 1 minute");
                interval = 1;
            }

            return new SyncConfig(interval, apps);
        } catch (JSONException e) {
            log.error("Error parsing metrics.json: {}", e.getMessage());
            return null;
        }
    }
}
