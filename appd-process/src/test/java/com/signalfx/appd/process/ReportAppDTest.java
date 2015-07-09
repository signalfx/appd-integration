/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.signalfx.appd.client.MetricDataRequest;
import com.signalfx.appd.client.exception.RequestException;
import com.signalfx.appd.client.exception.UnauthorizedException;
import com.signalfx.appd.client.model.MetricData;
import com.signalfx.appd.client.model.MetricValue;
import com.signalfx.appd.process.info.AppInfo;
import com.signalfx.appd.process.info.MetricInfo;
import com.signalfx.appd.process.reporter.Reporter;
import com.signalfx.appd.process.status.StatusType;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public class ReportAppDTest {

    @Test
    /**
     * Normal process of retrieving and reporting metrics.
     */
    public void testProcess() throws Exception {
        AppInfo app = new AppInfo("any");
        app.metrics.add(new MetricInfo("A|B", "C", null));

        MetricData metricData = new MetricData("", 0L, "name", "A|B");
        metricData.metricValues.add(new MetricValue(1, 1, 1, 1, 1, 2));

        MetricDataRequest request = Mockito.mock(MetricDataRequest.class);
        Mockito.when(request.get()).thenReturn(Collections.singletonList(metricData));

        Reporter reporter = Mockito.mock(Reporter.class);

        MetricRegistry metricRegistry = new MetricRegistry();

        ReportAppD reportAppD = Guice.createInjector(
                new AppDReportTestModule(request, reporter, metricRegistry)).getInstance(ReportAppD.class);
        reportAppD.perform(Collections.singletonList(app),
                MetricDataRequest.TimeParams.beforeNow(1L));

        Map<String, String> expectedDimensions = getExpectedDimensions();
        expectedDimensions.put("C", "A");

        Mockito.verify(request, Mockito.times(1)).get();
        Mockito.verify(reporter, Mockito.times(1)).report(
                Collections.singletonList(getDataPoint("B", 2, 1, expectedDimensions)));
        assertEquals(1,
                metricRegistry.counter(StatusType.dataPointsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.mtsEmpty.name()).getCount());
        assertEquals(1,
                metricRegistry.counter(StatusType.mtsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.appdRequestFailure.name()).getCount());
    }

    @Test
    /**
     * Unable to retrieve metrics from AppDynamics.
     */
    public void testMetricRequestFailure() throws Exception {
        AppInfo app = new AppInfo("any");
        app.metrics.add(new MetricInfo("A|B", "C", null));

        MetricDataRequest request = Mockito.mock(MetricDataRequest.class);
        Mockito.when(request.get()).thenThrow(new RequestException(""));

        Reporter reporter = Mockito.mock(Reporter.class);

        MetricRegistry metricRegistry = new MetricRegistry();

        ReportAppD reportAppD = Guice.createInjector(
                new AppDReportTestModule(request, reporter, metricRegistry)).getInstance(
                ReportAppD.class);
        reportAppD.perform(Collections.singletonList(app),
                MetricDataRequest.TimeParams.beforeNow(10L));

        Mockito.verify(request, Mockito.times(1)).get();
        Mockito.verify(reporter, Mockito.never()).report(Mockito.anyList());
        assertEquals(0,
                metricRegistry.counter(StatusType.dataPointsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.mtsEmpty.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.mtsReported.name()).getCount());
        assertEquals(1,
                metricRegistry.counter(StatusType.appdRequestFailure.name()).getCount());
    }

    @Test
    /**
     * Request to AppDynamics was not authorized.
     */
    public void testMetricRequestUnauthorized() throws Exception {
        AppInfo app = new AppInfo("any");
        app.metrics.add(new MetricInfo("A|B", "C", null));

        MetricDataRequest request = Mockito.mock(MetricDataRequest.class);
        Mockito.when(request.get()).thenThrow(new UnauthorizedException(""));

        Reporter reporter = Mockito.mock(Reporter.class);

        MetricRegistry metricRegistry = new MetricRegistry();

        ReportAppD reportAppD = Guice.createInjector(
                new AppDReportTestModule(request, reporter, metricRegistry)).getInstance(ReportAppD.class);
        reportAppD.perform(Collections.singletonList(app),
                MetricDataRequest.TimeParams.beforeNow(10L));

        Mockito.verify(request, Mockito.times(1)).get();
        Mockito.verify(reporter,
                Mockito.never()).report(Mockito.anyList());
        assertEquals(0,
                metricRegistry.counter(StatusType.dataPointsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.mtsEmpty.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.mtsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.appdRequestFailure.name()).getCount());
    }

    @Test
    /**
     * Submitting metrics to SignalFx failure.
     */
    public void testProcessReportFailure() throws Exception {
        AppInfo app = new AppInfo("any");
        app.metrics.add(new MetricInfo("A|B", "C", null));

        MetricData metricData = new MetricData("", 0L, "name", "A|B");
        metricData.metricValues.add(new MetricValue(1, 1, 1, 1, 1, 2));

        MetricDataRequest request = Mockito.mock(MetricDataRequest.class);
        Mockito.when(request.get()).thenReturn(Collections.singletonList(metricData));

        Reporter reporter = Mockito.mock(Reporter.class);
        Mockito.doThrow(new Reporter.ReportException("Something", null))
                .when(reporter).report(Mockito.anyList());

        MetricRegistry metricRegistry = new MetricRegistry();

        ReportAppD reportAppD = Guice.createInjector(
                new AppDReportTestModule(request, reporter, metricRegistry)).getInstance(ReportAppD.class);
        reportAppD.perform(Collections.singletonList(app),
                MetricDataRequest.TimeParams.beforeNow(10L));

        Map<String, String> expectedDimensions = getExpectedDimensions();
        expectedDimensions.put("C", "A");

        Mockito.verify(request, Mockito.times(1)).get();
        Mockito.verify(reporter, Mockito.times(1)).report(
                Collections.singletonList(getDataPoint("B", 2, 1, expectedDimensions)));
        assertEquals(0,
                metricRegistry.counter(StatusType.dataPointsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.mtsEmpty.name()).getCount());
        assertEquals(1,
                metricRegistry.counter(StatusType.mtsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.appdRequestFailure.name()).getCount());
    }

    @Test
    /**
     * Normal process with metrics with empty value list from AppDynamics.
     */
    public void testProcessMetricEmpty() throws Exception {
        AppInfo app = new AppInfo("any");
        app.metrics.add(new MetricInfo("A|B", "C", null));

        MetricData metricData = new MetricData("", 0L, "name", "A|B");

        MetricDataRequest request = Mockito.mock(MetricDataRequest.class);
        Mockito.when(request.get()).thenReturn(Collections.singletonList(metricData));

        Reporter reporter = Mockito.mock(Reporter.class);

        MetricRegistry metricRegistry = new MetricRegistry();

        ReportAppD reportAppD = Guice.createInjector(
                new AppDReportTestModule(request, reporter, metricRegistry)).getInstance(
                ReportAppD.class);
        reportAppD.perform(Collections.singletonList(app),
                MetricDataRequest.TimeParams.beforeNow(10L));

        Mockito.verify(request, Mockito.times(1)).get();
        Mockito.verify(reporter,
                Mockito.never()).report(Mockito.anyList());
        assertEquals(0,
                metricRegistry.counter(StatusType.dataPointsReported.name()).getCount());
        assertEquals(1,
                metricRegistry.counter(StatusType.mtsEmpty.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.mtsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.appdRequestFailure.name()).getCount());
    }

    @Test
    /**
     * Normal process with empty list of metrics from AppDynamics.
     */
    public void testProcessNoMetrics() throws Exception {
        AppInfo app = new AppInfo("any");
        app.metrics.add(new MetricInfo("A|B", "C", null));

        MetricDataRequest request = Mockito.mock(MetricDataRequest.class);
        Mockito.when(request.get()).thenReturn(Collections.<MetricData>emptyList());

        Reporter reporter = Mockito.mock(Reporter.class);

        MetricRegistry metricRegistry = new MetricRegistry();

        ReportAppD reportAppD = Guice.createInjector(
                new AppDReportTestModule(request, reporter, metricRegistry)).getInstance(ReportAppD.class);
        reportAppD.perform(Collections.singletonList(app),
                MetricDataRequest.TimeParams.beforeNow(10L));

        Mockito.verify(request, Mockito.times(1)).get();
        Mockito.verify(reporter,
                Mockito.never()).report(Mockito.anyList());
        assertEquals(0,
                metricRegistry.counter(StatusType.dataPointsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.mtsEmpty.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.mtsReported.name()).getCount());
        assertEquals(0,
                metricRegistry.counter(StatusType.appdRequestFailure.name()).getCount());
    }

    private SignalFxProtocolBuffers.DataPoint getDataPoint(String metricName, long timestamp,
                                                           long value,
                                                           Map<String, String> dimensions) {
        SignalFxProtocolBuffers.DataPoint.Builder builder = SignalFxProtocolBuffers.DataPoint
                .newBuilder();
        if (dimensions != null) {
            for (Map.Entry<String, String> entry : dimensions.entrySet()) {
                builder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                        .setKey(entry.getKey())
                        .setValue(entry.getValue()));
            }
        }
        return builder.setMetric(metricName).setTimestamp(timestamp).setValue(
                SignalFxProtocolBuffers.Datum.newBuilder()
                        .setIntValue(value)).build();
    }

    private Map<String, String> getExpectedDimensions() {
        Map<String, String> expectedDimensions = new HashMap<>();
        expectedDimensions.put("metric_source", "AppDynamics");
        return expectedDimensions;
    }

    public static class AppDReportTestModule extends AbstractModule {

        private MetricDataRequest metricDataRequest;
        private Reporter reporter;
        private MetricRegistry metricRegistry;

        public AppDReportTestModule(MetricDataRequest metricDataRequest, Reporter reporter,
                                    MetricRegistry metricRegistry) {
            this.metricDataRequest = metricDataRequest;
            this.reporter = reporter;
            this.metricRegistry = metricRegistry;
        }

        @Override
        protected void configure() {
            bind(MetricDataRequest.class).toInstance(metricDataRequest);
            bind(Reporter.class).toInstance(reporter);
            bind(MetricRegistry.class).toInstance(metricRegistry);
        }
    }
}
