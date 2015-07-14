/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.client;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.signalfx.appd.client.exception.RequestException;
import com.signalfx.appd.client.exception.UnauthorizedException;
import com.signalfx.appd.client.model.MetricData;
import com.signalfx.appd.client.model.MetricValue;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * MetricDataRequest performs request to AppDynamics REST API to retrieve metrics data.
 *
 * AppDynamics API is documented <a href="https://docs.appdynamics.com/display/PRO40/Use+the+AppDynamics+REST+API">here</a>.
 */
public class MetricDataRequest {

    /**
     * AppDynamics host URL
     */
    private final String appdURL;

    /**
     * AppDynamics username
     */
    private final String appdUsername;

    /**
     * AppDynamics password
     */
    private final String appdPassword;

    /**
     * AppDynamics application name
     */
    private String appName;

    /**
     * Time parameters to query the data.
     */
    private TimeParams timeParams;

    /**
     * AppDynamics metric path as query parameter
     */
    private String metricPath;

    public MetricDataRequest(String url, String username, String password) {
        this.appdURL = url;
        this.appdUsername = username;
        this.appdPassword = password;
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

    /**
     * Perform retrieval of metrics from AppDynamics using specified parameters.
     *
     * @return list of metric data.
     * @throws RequestException
     *         when there was an error with request.
     * @throws UnauthorizedException
     *         when unable to authorize with given credentials.
     */
    public List<MetricData> get() throws RequestException, UnauthorizedException {
        HttpResponse<String> response;
        try {
            response = Unirest.get(
                    this.appdURL + "/controller/rest/applications/" + appName + "/metric-data")
                    .header("accept", "application/json")
                    .basicAuth(this.appdUsername, this.appdPassword)
                    .queryString(getQueryString())
                    .queryString("output", "json")
                    .asString();
        } catch (UnirestException e) {
            throw new RequestException("Something was wrong with sending request.", e);
        }
        if (response == null) {
            throw new RequestException("Response is empty.");
        }
        switch (response.getStatus()) {
        case 200: {
            return process(new JsonNode(response.getBody()));
        }
        case 401: {
            throw new UnauthorizedException("Authentication failed");
        }
        default: {
            throw new RequestException("Unhandled response code " + response.getStatus());
        }
        }
    }

    /**
     * Generate querystring for the request.
     *
     * @return map of query strings.
     */
    protected Map<String, Object> getQueryString() {
        Map<String, Object> qs = new HashMap<>();
        if (timeParams != null) {
            qs.put("time-range-type", timeParams.type);
            if (timeParams.duration > 0) {
                qs.put("duration-in-mins", timeParams.duration);
            }
            if (timeParams.startTime > 0) {
                qs.put("start-time", timeParams.startTime);
            }
            if (timeParams.endTime > 0) {
                qs.put("end-time", timeParams.endTime);
            }
        }
        qs.put("rollup", false);
        if (metricPath != null) {
            qs.put("metric-path", metricPath);
        }
        return qs;
    }

    /**
     * Process the JSON response from the request.
     *
     * @param node
     *         root node of JSON response.
     * @return list of {@link MetricData}
     */
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
            for (int j = 0; j < valueArray.length(); j++) {
                JSONObject value = valueArray.getJSONObject(j);
                metricData.metricValues.add(
                        new MetricValue(value.getLong("count"), value.getLong("value"),
                                value.getLong("max"), value.getLong("min"), value.getLong("sum"),
                                value.getLong("startTimeInMillis")));
            }
        }
        return list;
    }

    /**
     * TimeParams represent time parameters in querystring.
     */
    public static class TimeParams {
        private final String type;
        private final long duration;
        private final long startTime;
        private final long endTime;

        /**
         * @param duration
         *         duration (in minutes) to return the metric data.
         * @return {@link com.signalfx.appd.client.MetricDataRequest.TimeParams}
         */
        public static TimeParams beforeNow(long duration) {
            return new TimeParams("BEFORE_NOW", duration, 0, 0);
        }

        /**
         * @param duration
         *         duration (in minutes) to return the metric data.
         * @param endTime
         *         end time (in milliseconds) until which the metric data is returned in UNIX epoch
         *         time.
         * @return {@link com.signalfx.appd.client.MetricDataRequest.TimeParams}
         */
        public static TimeParams beforeTime(long duration, long endTime) {
            return new TimeParams("BEFORE_TIME", duration, 0, endTime);
        }

        /**
         * @param duration
         *         duration (in minutes) to return the metric data.
         * @param startTime
         *         start time (in milliseconds) from which the metric data is returned in UNIX epoch
         *         time.
         * @return {@link com.signalfx.appd.client.MetricDataRequest.TimeParams}
         */
        public static TimeParams afterTime(long duration, long startTime) {
            return new TimeParams("AFTER_TIME", duration, startTime, 0);
        }

        /**
         * @param startTime
         *         start time (in milliseconds) from which the metric data is returned in UNIX epoch
         *         time.
         * @param endTime
         *         end time (in milliseconds) until which the metric data is returned in UNIX epoch
         *         time.
         * @return {@link com.signalfx.appd.client.MetricDataRequest.TimeParams}
         */
        public static TimeParams betweenTime(long startTime, long endTime) {
            return new TimeParams("BETWEEN_TIMES", 0, startTime, endTime);
        }

        protected TimeParams(String type, long duration, long startTime, long endTime) {
            this.type = type;
            this.duration = duration;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public boolean equals(Object that) {
            return that instanceof TimeParams && equals((TimeParams) that);
        }

        public boolean equals(TimeParams that) {
            return (this.type == null ? that.type == null : this.type.equals(that.type)) &&
                    this.duration == that.duration && this.startTime == that.startTime &&
                    this.endTime == that.endTime;
        }

        @Override
        public String toString() {
            return String.format("Type: %s, Duration: %d, StartTime: %d, EndTime: %d",
                    type, duration, startTime, endTime);
        }
    }
}
