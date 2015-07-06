package com.signalfx.appd.request;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.appd.config.ConnectionConfig;
import com.signalfx.appd.RequestException;
import com.signalfx.appd.model.MetricData;
import com.signalfx.appd.model.MetricValue;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class MetricDataRequest {

    protected static final Logger log = LoggerFactory.getLogger(MetricDataRequest.class);

    private final ConnectionConfig connectionConfig;
    private String appName;
    private TimeParams timeParams;
    private String metricPath;

    public MetricDataRequest(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setTimeParams(TimeParams timeParams) {
        this.timeParams = timeParams;
    }

    public void setMetricPath(String metricPath) {
        this.metricPath = metricPath;
    }


    public List<MetricData> get() throws RequestException {
        HttpResponse<JsonNode> response;
        try {
            response = Unirest.get(this.connectionConfig.appdURL + "/controller/rest/applications/" + appName + "/metric-data")
                    .header("accept", "application/json")
                    .basicAuth(this.connectionConfig.appdUsername, this.connectionConfig.appdPassword)
                    .queryString(getQueryString())
                    .queryString("output", "json")
                    .asJson();
        } catch (UnirestException e) {
            log.error("Something was wrong with sending AppD request.");
            throw new RequestException("Something was wrong with sending request.", e);
        }
        if (response == null) {
            log.error("AppD response is empty.");
            throw new RequestException("Response is empty.");
        }
        return process(response.getBody());
    }

    protected Map<String, Object> getQueryString() {
        Map<String, Object> qs = new HashMap<>();
        if (timeParams != null) {
            qs.put("time-range-type", timeParams.type);
            qs.put("duration-in-mins", timeParams.duration);
            qs.put("start-time", timeParams.startTime);
            qs.put("end-time", timeParams.endTime);
        }
        qs.put("rollup", false);
        if (metricPath != null) {
            qs.put("metric-path", metricPath);
        }
        return qs;
    }

    protected List<MetricData> process(JsonNode node) {
        JSONArray dataArray = node.getArray();
        List<MetricData> list = new LinkedList<>();
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject data = dataArray.getJSONObject(i);
            MetricData metricData =
                    new MetricData(data.getString("frequency"), data.getLong("metricId"),
                        data.getString("metricName"), data.getString("metricPath"));
            list.add(metricData);
            JSONArray valueArray = data.getJSONArray("metricValues");
            for (int j = 0; j< valueArray.length(); j++) {
                JSONObject value = valueArray.getJSONObject(j);
                metricData.metricValues.add(
                        new MetricValue(value.getLong("count"), value.getLong("value"),
                                value.getLong("max"), value.getLong("min"), value.getLong("sum"),
                                value.getLong("startTimeInMillis")));
            }
        }
        return list;
    }

    public static class TimeParams {
        private final String type;
        private final long duration;
        private final long startTime;
        private final long endTime;

        public static TimeParams beforeNow(long duration) {
            return new TimeParams("BEFORE_NOW", duration, 0, 0);
        }

        private TimeParams(String type, long duration, long startTime, long endTime) {
            this.type = type;
            this.duration = duration;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}
