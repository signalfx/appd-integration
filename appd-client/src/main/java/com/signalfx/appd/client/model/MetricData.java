package com.signalfx.appd.client.model;

import java.util.LinkedList;
import java.util.List;

/**
 * MetricData represents each Metric data from AppDynamics query.
 */
public class MetricData {

    public final String frequency;
    public final long id;
    public final String metricName;
    public final String metricPath;
    public final List<MetricValue> metricValues;

    public MetricData(String frequency, long id, String metricName, String metricPath) {
        this.frequency = frequency;
        this.id = id;
        this.metricName = metricName;
        this.metricPath = metricPath;
        this.metricValues = new LinkedList<>();
    }
}
