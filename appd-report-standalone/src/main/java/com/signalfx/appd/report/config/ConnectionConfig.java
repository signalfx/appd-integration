/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.report.config;

public class ConnectionConfig {

    public final String appdUsername;
    public final String appdPassword;
    public final String appdURL;
    public final String fxToken;

    public ConnectionConfig(String appdUsername, String appdPassword, String appdURL,
                            String fxToken) {
        this.appdUsername = appdUsername;
        this.appdPassword = appdPassword;
        this.appdURL = appdURL;
        this.fxToken = fxToken;
    }
}
