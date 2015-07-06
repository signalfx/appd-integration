package com.signalfx.appd.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionConfig {

    protected static final Logger log = LoggerFactory.getLogger(ConnectionConfig.class);

    public final String appdUsername;
    public final String appdPassword;
    public final String appdURL;
    public final String fxToken;

    public ConnectionConfig(String appdUsername, String appdPassword, String appdURL, String fxToken) {
        this.appdUsername = appdUsername;
        this.appdPassword = appdPassword;
        this.appdURL = appdURL;
        this.fxToken = fxToken;
    }

    public static ConnectionConfig get() {
        boolean isValid = true;
        String appdUsername = getPropertyOrEnv("com.signalfx.appd.username", "APPD_USERNAME");
        if (StringUtils.isEmpty(appdUsername)) {
            log.error("AppDynamics username not specified.");
            isValid = false;
        }
        String appdPassword = getPropertyOrEnv("com.signalfx.appd.password", "APPD_PASSWORD");
        if (StringUtils.isEmpty(appdPassword)) {
            log.error("AppDynamics password not specified.");
            isValid = false;
        }
        String appdURL = getPropertyOrEnv("com.signalfx.appd.host", "APPD_HOST");
        if (StringUtils.isEmpty(appdURL)) {
            log.error("AppDynamics host not specified.");
            isValid = false;
        }
        String fxToken = getPropertyOrEnv("com.signalfx.api.token", "SIGNALFX_TOKEN");
        if (StringUtils.isEmpty(fxToken)) {
            log.error("SignalFx token not specified.");
            isValid = false;
        }
        if (isValid) {
            return new ConnectionConfig(appdUsername, appdPassword, appdURL, fxToken);
        } else {
            return null;
        }
    }

    private static String getPropertyOrEnv(String propertyName, String envName) {
        return System.getProperty(propertyName, System.getenv(envName));
    }
}
