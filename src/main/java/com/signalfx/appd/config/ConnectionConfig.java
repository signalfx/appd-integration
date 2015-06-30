package com.signalfx.appd.config;

public class ConnectionConfig {
    public final String adUsername;
    public final String adPassword;
    public final String adURL;
    public final String fxToken;

    public ConnectionConfig(String adUsername, String adPassword, String adURL,
                            String fxToken) {
        this.adUsername = adUsername;
        this.adPassword = adPassword;
        this.adURL = adURL;
        this.fxToken = fxToken;
    }
}
