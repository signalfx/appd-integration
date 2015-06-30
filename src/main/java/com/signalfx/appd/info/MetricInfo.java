package com.signalfx.appd.info;

import java.util.Map;

public class MetricInfo {

    //AppDynamics Metric Path
    public final String metricPath;

    //SignalFX metric name
    public final String metricName;

    //SignalFX dimensions
    public final Map<String, String> dimensions;

    public MetricInfo(String metricPath, String metricName, Map<String, String> dimensions) {
        this.metricPath = metricPath;
        this.metricName = metricName != null ? metricName : metricPath;
        this.dimensions = dimensions;
    }
}
