/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process.info;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AppInfo representing AppDynamics App.
 *
 * @author 9park
 */
public class AppInfo {
    public final String name;
    public final List<MetricInfo> metrics;

    public AppInfo(String name) {
        this.name = name;
        this.metrics = new LinkedList<>();
    }

    @JsonCreator
    public AppInfo(@JsonProperty("name") String name, @JsonProperty("metrics") List<MetricInfo> metrics) {
        this.name = name;
        this.metrics = metrics;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (name != null ? name.hashCode() : 0);
        result = prime * result + metrics.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof AppInfo && equals((AppInfo) that);
    }

    public boolean equals(AppInfo that) {
        return (this.name == null ? that.name == null : this.name.equals(that.name)) &&
                this.metrics.equals(that.metrics);
    }

    @Override
    public String toString() {
        return String.format("name:%s, metrics:%s", name, metrics.toString());
    }
}
