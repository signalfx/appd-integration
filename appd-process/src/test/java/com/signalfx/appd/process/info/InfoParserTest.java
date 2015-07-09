/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process.info;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.google.common.collect.Lists;

public class InfoParserTest {

    @Test
    public void testParseInfo() throws Exception {
        AppInfo app1 = new AppInfo("Sample");

        // "metric_source": "AppDynamics" dimensions is always added as default.
        app1.metrics.add(new MetricInfo(
                "Application Infrastructure Performance|Tier2|Individual Nodes|*|Hardware Resources|*|*",
                "-|tier|-|node|resource_type|component_type",
                getDimensions()));

        // default dimensions and extra in the configurations.
        Map<String, String> dimensions = getDimensions();
        dimensions.put("a", "b");
        app1.metrics.add(new MetricInfo(
                "Application Infrastructure Performance|Tier2|Individual Nodes|*|Hardware Resources|*|*|*",
                "-|tier|-|node|resource_type|component_type|component_instance",
                dimensions));

        // Overriding metric_source dimensions from the configurations with the default
        app1.metrics.add(new MetricInfo("Application Infrastructure Performance|Tier2|Individual Nodes|*|Agent|*|*",
                "-|tier|-|node|-|category", getDimensions()));

        AppInfo app2 = new AppInfo("Two");

        dimensions = getDimensions();
        dimensions.put("c", "bla bla");
        app2.metrics.add(new MetricInfo("1|2", "3", dimensions));

        List<AppInfo> expectedConfig = Lists.newArrayList(app1, app2);

        String jsonString = IOUtils.toString(getClass().getResourceAsStream("/metrics.json"));
        List<AppInfo> actualConfig = InfoParser.parseInfo(jsonString);

        assertEquals(expectedConfig, actualConfig);
    }

    private Map<String, String> getDimensions() {
        Map<String, String> dimensions = new HashMap<>();
        // Always add metric_source AppDynamics.
        dimensions.put("metric_source", "AppDynamics");
        return dimensions;
    }
}
