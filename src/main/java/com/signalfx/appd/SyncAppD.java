package com.signalfx.appd;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.appd.config.ConnectionConfig;
import com.signalfx.appd.info.AppInfo;
import com.signalfx.appd.info.MetricInfo;
import com.signalfx.appd.model.MetricData;
import com.signalfx.appd.request.MetricDataRequest;

public class SyncAppD {

    protected static final Logger log = LoggerFactory.getLogger(SyncAppD.class);

    private final List<AppInfo> apps;
    private final MetricDataRequest dataRequest;
    private final ReportMetric reportMetric;

    public SyncAppD(List<AppInfo> apps, ConnectionConfig connectionConfig) {
        this.apps = apps;
        this.dataRequest = new MetricDataRequest(connectionConfig);
        this.reportMetric = new ReportMetric(connectionConfig);
    }

    public void perform(long duration) {
        for (AppInfo app : apps) {
            dataRequest.setAppName(app.name);
            for (MetricInfo metric : app.metrics) {
                dataRequest.setTimeParams(
                        MetricDataRequest.TimeParams.beforeNow(duration));
                dataRequest.setMetricPath(metric.metricPath);

                try {
                    List<MetricData> metricDataList = dataRequest.get();
                    if (metricDataList.size() > 0) {
                        if (metricDataList.size() > 1) {
                            // too many metrics found, we will just use the first one.
                            log.warn("{} metric paths found, only the first one is used.", metricDataList.size());
                        }
                        reportMetric.report(metricDataList.get(0), metric);
                    } else {
                        // no metrics found, something is wrong with selection
                        log.error("No metric path found for metric path selection \"{}\"", metric.metricPath);
                    }
                } catch (RequestException e) {
                    // too bad
                    log.warn("Metric report failure for \"{}\"", metric.metricPath);
                }
            }
        }
    }
}
