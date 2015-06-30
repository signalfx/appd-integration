package com.signalfx.appd;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.appd.config.ConnectionConfig;
import com.signalfx.appd.info.MetricInfo;
import com.signalfx.appd.model.MetricData;
import com.signalfx.appd.model.MetricValue;

import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.CountingOnSendErrorHandler;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public class ReportMetric {

    protected static final Logger log = LoggerFactory.getLogger(ReportMetric.class);

    private final Map<MetricInfo, Long> metricToLastTimestampMap = new HashMap<>();

    private final ConnectionConfig connectionConfig;

    private final List<OnSendErrorHandler> onSendErrorHandlers;
    private final CountingOnSendErrorHandler countingOnSendErrorHandler;

    public ReportMetric(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
        this.onSendErrorHandlers = new LinkedList<>();

        this.countingOnSendErrorHandler = new CountingOnSendErrorHandler();
        this.onSendErrorHandlers.add(countingOnSendErrorHandler);
    }

    public void report(MetricData data, MetricInfo info) throws RequestException {
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
                    .setMetric(info.metricName);
            if (info.dimensions != null) {
                for (Map.Entry<String, String> entry : info.dimensions.entrySet()) {
                    dataPointBuilder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                            .setKey(entry.getKey())
                            .setValue(entry.getValue()));
                }
            }

            long lastTimestamp = metricToLastTimestampMap.containsKey(info) ? metricToLastTimestampMap.get(info) : 0;
            long latestTimestamp = lastTimestamp;
            for (MetricValue value : data.metricValues) {
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
                metricToLastTimestampMap.put(info, latestTimestamp);
            }
        } catch (IOException e) {
                log.error("Metric \"{}\" send failure (count {}).",
                        data.metricValues.size(), data.metricPath);
                throw new RequestException("There was something wrong with sending request", e);
        }
    }
}
