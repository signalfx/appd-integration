package com.signalfx.appd.model;

public class MetricValue {
    public final long count;
    public final long value;
    public final long max;
    public final long min;
    public final long sum;
    public final long startTimeInMillis;

    public MetricValue(long count, long value, long max, long min, long sum, long startTimeInMillis) {
        this.count = count;
        this.value = value;
        this.max = max;
        this.min = min;
        this.sum = sum;
        this.startTimeInMillis = startTimeInMillis;
    }
}
