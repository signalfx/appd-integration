package com.signalfx.appd;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.appd.config.ConnectionConfig;
import com.signalfx.appd.model.MetricValue;

import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.CountingOnSendErrorHandler;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * ReportMetric handles reporting metrics to SignalFx
 */
public class ReportMetric {

    protected static final Logger log = LoggerFactory.getLogger(ReportMetric.class);

    // A map of <Metric, Dimension> to last timestamp sent.
    private final Map<String, Map<Map<String, String>, Long>> metricToLastTimestampMap = new HashMap<>();

    private final ConnectionConfig connectionConfig;

    private final List<OnSendErrorHandler> onSendErrorHandlers;
    private final CountingOnSendErrorHandler countingOnSendErrorHandler;

    public ReportMetric(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
        this.onSendErrorHandlers = new LinkedList<>();

        this.countingOnSendErrorHandler = new CountingOnSendErrorHandler();
        this.onSendErrorHandlers.add(countingOnSendErrorHandler);
    }

    public void report(List<MetricValue> values, String metricName, Map<String, String> dimensions) throws RequestException {
        SignalFxReceiverEndpoint dataPointEndpoint = new SignalFxEndpoint();
        AggregateMetricSender mf =
                new AggregateMetricSender("appd-integration",
                        new HttpDataPointProtobufReceiverFactory(
                                dataPointEndpoint)
                                .setVersion(2),
                        new StaticAuthToken(connectionConfig.fxToken),
                        onSendErrorHandlers);
        try (AggregateMetricSender.Session i = mf.createSession()) {
            SignalFxProtocolBuffers.DataPoint.Builder dataPointBuilder = SignalFxProtocolBuffers.DataPoint
                    .newBuilder()
                    .setMetric(metricName);
            if (dimensions != null) {
                for (Map.Entry<String, String> entry : dimensions.entrySet()) {
                    dataPointBuilder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                            .setKey(entry.getKey())
                            .setValue(entry.getValue()));
                }
            }
            long lastTimestamp = getLastTimestamp(metricName, dimensions);
            long latestTimestamp = lastTimestamp;
            for (MetricValue value : values) {
                if (value.startTimeInMillis > lastTimestamp) {
                    i.setDatapoint(
                            dataPointBuilder
                                    .setValue(
                                            SignalFxProtocolBuffers.Datum.newBuilder()
                                                    .setIntValue(value.value))
                                    .setTimestamp(value.startTimeInMillis)
                                    .build());
                    latestTimestamp = Math.max(value.startTimeInMillis, latestTimestamp);
                }
            }
            if (latestTimestamp > lastTimestamp) {
                putLastTimestamp(metricName, dimensions, latestTimestamp);
            }
        } catch (IOException e) {
                log.error("Metric \"{}\" send failure (count {}).",
                        values.size(), metricName);
                throw new RequestException("There was something wrong with sending request", e);
        }
    }

    private long getLastTimestamp(String metricName, Map<String, String> dimensions) {
        Map<Map<String, String>, Long> dimensionsToTimestamp = metricToLastTimestampMap.get(metricName);
        if (dimensionsToTimestamp == null) {
            return 0;
        }
        Long timestamp = dimensionsToTimestamp.get(dimensions);
        if (timestamp == null) {
            return 0;
        }
        return timestamp;
    }

    private void putLastTimestamp(String metricName, Map<String, String> dimensions, long timestamp) {
        Map<Map<String, String>, Long> dimensionsToTimestamp = metricToLastTimestampMap.get(metricName);
        if (dimensionsToTimestamp == null) {
            dimensionsToTimestamp = new HashMap<>();
            metricToLastTimestampMap.put(metricName, dimensionsToTimestamp);
        }
        dimensionsToTimestamp.put(dimensions, timestamp);
    }
}
