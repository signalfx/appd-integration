package com.signalfx.appd.info;

import java.util.Map;

/**
 * MetricInfo contains information about each metric mapping from AppDynamics metric path to
 * SignalFx metric name and dimensions
 */
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

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (metricPath != null ? metricPath.hashCode() : 0);
        result = prime * result + (metricName != null ? metricName.hashCode() : 0);
        result = prime * result + (dimensions != null ? dimensions.hashCode() : 0);
        return result;
    }
}
