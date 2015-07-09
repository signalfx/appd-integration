/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.report;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.signalfx.appd.client.MetricDataRequest;
import com.signalfx.appd.process.reporter.Reporter;
import com.signalfx.appd.report.config.ConnectionConfig;
import com.signalfx.appd.report.reporter.SignalFxRestReporter;

/**
 * Module configurations for AppDReport using given connection config.
 *
 * @author 9park
 */
public class AppDReportModule extends AbstractModule {

    private final ConnectionConfig connectionConfig;
    private final MetricRegistry metricRegistry;

    public AppDReportModule(ConnectionConfig connectionConfig, MetricRegistry metricRegistry) {
        this.connectionConfig = connectionConfig;
        this.metricRegistry = metricRegistry;
    }

    @Override
    protected void configure() {
        bind(MetricDataRequest.class).toInstance(new MetricDataRequest(connectionConfig.appdURL, connectionConfig.appdUsername, connectionConfig.appdPassword));
        bind(Reporter.class).toInstance(new SignalFxRestReporter(connectionConfig.fxToken));
        bind(MetricRegistry.class).toInstance(metricRegistry);
    }
}
