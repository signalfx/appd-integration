package com.signalfx.appd;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.appd.config.ConnectionConfig;
import com.signalfx.appd.info.AppInfo;
import com.signalfx.appd.info.MetricInfo;
import com.signalfx.appd.model.MetricData;
import com.signalfx.appd.request.MetricDataRequest;

/**
 * SyncAppD performs syncing of AppDynamics metrics to SignalFx
 */
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

    /**
     * Perform syncing of AppDynamics metrics to SignalFx
     * @param range number of minute to query back from current time.
     */
    public void perform(long range) {
        for (AppInfo app : apps) {
            dataRequest.setAppName(app.name);
            for (MetricInfo metricInfo : app.metrics) {
                dataRequest.setTimeParams(
                        MetricDataRequest.TimeParams.beforeNow(range));
                dataRequest.setMetricPath(metricInfo.metricPath);

                // If query contains *, we could be getting more than one result so we are using
                // the actual path from the result instead of the configurations metric name.
                boolean useActualPath = metricInfo.metricPath.contains("*");

                try {
                    List<MetricData> metricDataList = dataRequest.get();

                    if (metricDataList.size() > 0) {
                        for (MetricData metricData : metricDataList) {
                            if (!metricData.metricValues.isEmpty()) {
                                reportMetric.report(metricData.metricValues,
                                        useActualPath ?
                                                metricData.metricPath :
                                                metricInfo.metricName,
                                        metricInfo.dimensions);
                            }
                        }
                    } else {
                        // no metrics found, something is wrong with selection
                        log.warn("No metric found for query \"{}\"", metricInfo.metricPath);
                    }
                } catch (RequestException e) {
                    // too bad
                    log.warn("Metric report failure for \"{}\"", metricInfo.metricPath);
                }
            }
        }
    }
}
