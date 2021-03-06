/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process.info;

import java.util.Arrays;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.signalfx.appd.process.model.MetricTimeSeries;

/**
 * MetricInfo contains information about each metric mapping from AppDynamics metric path to
 * SignalFx metric name and dimensions.
 *
 * AppDynamics metric paths are described as a pipe-delimited string (|),
 * for example Performance|AppServer1|Resources|CPU.
 *
 * Each metric is reported to SignalFx with the last element of this path as the metric name,
 * and each previous element mapped to a dimension according to the dimensionsPathMap.
 *
 * Elements can be ignored by specifying the target dimension as - (dash) in the dimensionsPathMap.
 *
 * e.g. MetricPath = Performance|AppServer1|Resources|CPU
 *      DimensionsPathMap = category|host|-
 *
 *      would be mapped to
 *      {
 *         metric_name : "CPU"
 *         dimensions {
 *              category: "Performance",
 *              host: "AppServer1"
 *          }
 *       }
 *
 * Wild cards (asterisk *) can be used to specify that all matching AppDynamics metrics are
 * to be collected. Mapping to dimensions through the dimensionsPathMap will still happen on
 * the actual value of that metric path element.
 *
 * Extra dimensions could also be added to metric data with extra {@link Map}
 *
 * @author 9park
 */
public class MetricInfo {

    /**
     * AppDynamics Metric Path used in a query
     */
    public final String metricPathQuery;

    /**
     * SignalFX dimensions
     */
    public final Map<String, String> dimensions;

    /**
     * Mapping of AppDynamics path to SignalFx dimensions
     */
    private final String[] dimensionsPath;

    /**
     * @param metricPathQuery
     *         pipe (|) separated metric path used for querying against AppDynamics.
     * @param dimensionsPathMap
     *         pipe(|) separated mapping of dimension names to be mapped against corresponding
     *         position in metric path.
     * @param dimensions
     *         extra dimensions to be included with the metric.
     * @throws InvalidPropertiesFormatException
     *         when metric path query and dimensions path map size
     */
    @JsonCreator
    public MetricInfo(@JsonProperty("metric_path") String metricPathQuery,
                      @JsonProperty("dimensions_path_map") String dimensionsPathMap,
                      @JsonProperty("dimensions") Map<String, String> dimensions)
            throws InvalidPropertiesFormatException {
        this.metricPathQuery = metricPathQuery;
        if (dimensions == null) {
            this.dimensions = new HashMap<>();
        } else {
            this.dimensions = dimensions;
        }
        // Always add metric_source as AppDynamics.
        this.dimensions.put("metric_source", "AppDynamics");

        dimensionsPath = dimensionsPathMap.split("\\|");

        int metricPathLength = metricPathQuery.split("\\|").length;
        if (metricPathLength != dimensionsPath.length + 1) {
            throw new InvalidPropertiesFormatException(
                    String.format("MetricPath %s has %d properties but dimensions path have %d",
                            metricPathQuery, metricPathLength, dimensionsPath.length));
        }
    }

    /**
     * Create a {@link MetricTimeSeries} for a given metric path. It will use the given metric path
     * as dimensions mapping and add extra dimensions as specified in the MetricInfo.
     *
     * @param actualMetricPath
     *         metric path used to mapped to dimensions in metric time series.
     * @return {@link MetricTimeSeries}
     */
    public MetricTimeSeries getMetricTimeSeries(String actualMetricPath) {
        Map<String, String> actualDimensions = new HashMap<>();

        actualDimensions.putAll(dimensions);

        String[] path = actualMetricPath.split("\\|");
        for (int i = 0; i < dimensionsPath.length; i++) {
            if ("-".equals(dimensionsPath[i])) {
                continue;
            }
            actualDimensions.put(dimensionsPath[i], path[i]);
        }
        return new MetricTimeSeries(path[path.length - 1], actualDimensions);
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + metricPathQuery.hashCode();
        result = prime * result + Arrays.hashCode(dimensionsPath);
        result = prime * result + dimensions.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof MetricInfo && equals((MetricInfo) that);
    }

    public boolean equals(MetricInfo that) {
        return this.metricPathQuery.equals(that.metricPathQuery) &&
                Arrays.equals(this.dimensionsPath, that.dimensionsPath) &&
                this.dimensions.equals(that.dimensions);
    }

    @Override
    public String toString() {
        return String.format("metricPathQuery:%s, dimensionsPath:%s, dimensions:%s",
                metricPathQuery, Arrays.toString(dimensionsPath), dimensions.toString());
    }
}
