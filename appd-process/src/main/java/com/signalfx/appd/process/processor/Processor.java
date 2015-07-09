/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process.processor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.signalfx.appd.client.model.MetricValue;
import com.signalfx.appd.process.model.MetricTimeSeries;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * Processor handles processing of MetricValues retrieved from AppDynamics and filters already
 * processed data point by timestamp
 *
 * @author 9park
 */
public class Processor {

    @Inject
    public Processor() {
    }

    // A map of MTS to last timestamp sent.
    private final Map<MetricTimeSeries, Long> mtsToLastTimestamp = new HashMap<>();

    /**
     * Process {@link MetricTimeSeries} and {@link MetricValue} filtering out data point earlier or
     * equal to timestamp that was marked as already sent.
     *
     * @param mts
     *         MetricTimeSeries for the values
     * @param metricValues
     *         metric values from AppDynamics. Timestamp needs to be incrementally ordered.
     * @return list of {@link com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.DataPoint} to be
     * submitted to SignalFx
     */
    public List<SignalFxProtocolBuffers.DataPoint> process(MetricTimeSeries mts,
                                                           List<MetricValue> metricValues) {
        Long lastTimestamp = mtsToLastTimestamp.get(mts);
        Long latestTimestamp = lastTimestamp;
        List<SignalFxProtocolBuffers.DataPoint> dataPoints = new LinkedList<>();
        SignalFxProtocolBuffers.DataPoint.Builder dataPointBuilder = SignalFxProtocolBuffers.DataPoint
                .newBuilder()
                .setMetric(mts.metricName);

        if (mts.dimensions != null) {
            for (Map.Entry<String, String> entry : mts.dimensions.entrySet()) {
                dataPointBuilder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                        .setKey(entry.getKey())
                        .setValue(entry.getValue()));
            }
        }

        for (MetricValue metricValue : metricValues) {
            if (lastTimestamp == null || metricValue.startTimeInMillis > lastTimestamp) {
                SignalFxProtocolBuffers.DataPoint dataPoint = dataPointBuilder
                        .setTimestamp(metricValue.startTimeInMillis)
                        .setValue(SignalFxProtocolBuffers.Datum.newBuilder()
                                .setIntValue(metricValue.value)).build();
                dataPoints.add(dataPoint);

                latestTimestamp = lastTimestamp == null ?
                        metricValue.startTimeInMillis :
                        Math.max(metricValue.startTimeInMillis, latestTimestamp);
            }
        }
        if (lastTimestamp == null || latestTimestamp > lastTimestamp) {
            mtsToLastTimestamp.put(mts, latestTimestamp);
        }
        return dataPoints;
    }

}
