package com.signalfx.appd.info;

import java.util.LinkedList;
import java.util.List;

public class AppInfo {
    public final String name;
    public final List<MetricInfo> metrics;

    public AppInfo(String name) {
        this.name = name;
        this.metrics = new LinkedList<>();
    }
}
