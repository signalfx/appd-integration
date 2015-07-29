/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.signalfx.appd.client.MetricDataRequest;
import com.signalfx.appd.client.exception.RequestException;
import com.signalfx.appd.client.exception.UnauthorizedException;
import com.signalfx.appd.client.model.MetricData;
import com.signalfx.appd.process.info.AppInfo;
import com.signalfx.appd.process.info.MetricInfo;
import com.signalfx.appd.process.model.MetricTimeSeries;
import com.signalfx.appd.process.processor.Processor;
import com.signalfx.appd.process.reporter.Reporter;
import com.signalfx.appd.process.status.StatusType;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * ReportAppD performs reporting of AppDynamics metrics to SignalFx
 *
 * @author 9park
 */
public class ReportAppD {

    protected static final Logger log = LoggerFactory.getLogger(ReportAppD.class);

    private final MetricDataRequest dataRequest;
    private final Processor processor;
    private final Reporter reporter;

    private final Counter counterDataPointsReported;
    private final Counter counterMtsReported;
    private final Counter counterMtsEmpty;
    private final Counter counterAppDRequestFailure;

    @Inject
    public ReportAppD(MetricDataRequest metricDataRequest, Processor processor, Reporter reporter,
                      MetricRegistry metricRegistry) {
        this.dataRequest = metricDataRequest;
        this.processor = processor;
        this.reporter = reporter;

        counterDataPointsReported = metricRegistry.counter(StatusType.dataPointsReported.name());
        counterMtsReported = metricRegistry.counter(StatusType.mtsReported.name());
        counterMtsEmpty = metricRegistry.counter(StatusType.mtsEmpty.name());
        counterAppDRequestFailure = metricRegistry.counter(StatusType.appdRequestFailure.name());
    }

    /**
     * Perform reading and reporting of AppDynamics metrics to SignalFx
     *
     * @param timeParams
     *         Time paracounters to query metrics from AppDynamics.
     */
    public void perform(List<AppInfo> apps, MetricDataRequest.TimeParams timeParams) {
        List<SignalFxProtocolBuffers.DataPoint> dataPoints = new LinkedList<>();
        for (AppInfo app : apps) {
            dataRequest.setAppName(app.name);
            for (MetricInfo metricInfo : app.metrics) {
                dataRequest.setTimeParams(timeParams);
                dataRequest.setMetricPath(metricInfo.metricPathQuery);

                List<MetricData> metricDataList;
                try {
                    metricDataList = dataRequest.get();
                } catch (RequestException e) {
                    // too bad
                    log.error("Metric query failure for \"{}\"", metricInfo.metricPathQuery);
                    counterAppDRequestFailure.inc();
                    continue;
                } catch (UnauthorizedException e) {
                    log.error("AppDynamics authentication failed");
                    return;
                }
                if (metricDataList != null && metricDataList.size() > 0) {
                    for (MetricData metricData : metricDataList) {
                        MetricTimeSeries mts =
                                metricInfo.getMetricTimeSeries(metricData.metricPath);
                        List<SignalFxProtocolBuffers.DataPoint> mtsDataPoints = processor
                                .process(mts, metricData.metricValues);
                        dataPoints.addAll(mtsDataPoints);
                        if (!mtsDataPoints.isEmpty()) {
                            counterMtsReported.inc();
                        } else {
                            counterMtsEmpty.inc();
                        }
                    }
                } else {
                    // no metrics found, something is wrong with selection
                    log.warn("No metric found for query \"{}\"", metricInfo.metricPathQuery);
                }
            }
        }
        if (!dataPoints.isEmpty()) {
            try {
                reporter.report(dataPoints);
                counterDataPointsReported.inc(dataPoints.size());
            } catch (Reporter.ReportException e) {
                log.error("There were errors reporting metric");
            }
        }
    }
}
