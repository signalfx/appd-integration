/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process.info;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;

import org.junit.Test;

import com.signalfx.appd.process.model.MetricTimeSeries;

public class MetricInfoTest {

    @Test
    public void testMetricInfoParam() {
        try {
            new MetricInfo("A|B|C|D", "1|2|3|4", null);
            fail("Exception should have been thrown.");
        } catch (InvalidPropertiesFormatException e) {
            //expected
        }
    }

    @Test
    public void testGetMetricTimeSeries() throws InvalidPropertiesFormatException {
        MetricInfo metricInfo = new MetricInfo("A|B|C|D", "1|-|3", null);
        MetricTimeSeries actualMts = metricInfo.getMetricTimeSeries("W|X|Y|Z");

        Map<String, String> dimensions = new HashMap<>();
        // Always add metric_source as AppDynamics.
        dimensions.put("metric_source", "AppDynamics");

        dimensions.put("1", "W");
        // ignore the second position since it's -
        dimensions.put("3", "Y");
        MetricTimeSeries expectedMts = new MetricTimeSeries("Z", dimensions);

        assertEquals(expectedMts, actualMts);
    }

    @Test
    public void testGetMetricTimeSeriesWithExtraDimensions()
            throws InvalidPropertiesFormatException {
        Map<String, String> extraDimensions = new HashMap<>();
        extraDimensions.put("3", "M");
        extraDimensions.put("4", "N");
        MetricInfo metricInfo = new MetricInfo("A|B|C|D", "1|2|3", extraDimensions);
        MetricTimeSeries actualMts = metricInfo.getMetricTimeSeries("W|X|Y|Z");

        Map<String, String> dimensions = new HashMap<>();
        dimensions.putAll(extraDimensions);
        dimensions.put("1", "W");
        dimensions.put("2", "X");
        dimensions.put("3", "Y");
        MetricTimeSeries expectedMts = new MetricTimeSeries("Z", dimensions);

        assertEquals(expectedMts, actualMts);
    }
}
