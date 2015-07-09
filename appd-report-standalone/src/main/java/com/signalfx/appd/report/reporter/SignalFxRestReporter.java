/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.report.reporter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.signalfx.appd.process.reporter.Reporter;
import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * SignalFxRestReporter handles submitting data points to SignalFx using REST API and protobuf.
 *
 * @author 9park
 */
public class SignalFxRestReporter implements Reporter {

    private final AggregateMetricSender metricSender;

    @Inject
    public SignalFxRestReporter(String token) {
        SignalFxReceiverEndpoint dataPointEndpoint = new SignalFxEndpoint();
        metricSender =
                new AggregateMetricSender("appd-integration",
                        new HttpDataPointProtobufReceiverFactory(
                                dataPointEndpoint)
                                .setVersion(2),
                        new StaticAuthToken(token),
                        Collections.<OnSendErrorHandler>emptySet());
    }

    public void report(List<SignalFxProtocolBuffers.DataPoint> dataPoints) throws ReportException {
        try (AggregateMetricSender.Session i = metricSender.createSession()) {
            for (SignalFxProtocolBuffers.DataPoint dataPoint : dataPoints) {
                i.setDatapoint(dataPoint);
            }
        } catch (IOException e) {
            throw new ReportException("There was something wrong with sending request", e);
        }
    }
}
