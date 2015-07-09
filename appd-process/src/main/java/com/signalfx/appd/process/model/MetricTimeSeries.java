/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process.model;

import java.util.Map;

public class MetricTimeSeries {
    public String metricName;
    public Map<String, String> dimensions;

    public MetricTimeSeries(String metricName, Map<String, String> dimensions) {
        this.metricName = metricName;
        this.dimensions = dimensions;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (metricName != null ? metricName.hashCode() : 0);
        result = prime * result + (dimensions != null ? dimensions.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof MetricTimeSeries && equals((MetricTimeSeries) that);
    }

    public boolean equals(MetricTimeSeries that) {
        return (this.metricName == null ?
                that.metricName == null :
                this.metricName.equals(that.metricName)) &&
                (this.dimensions == null ?
                        that.dimensions == null :
                        this.dimensions.equals(that.dimensions));
    }
}
