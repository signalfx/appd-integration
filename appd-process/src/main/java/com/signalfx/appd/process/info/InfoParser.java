/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process.info;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InfoParser {

    /**
     * Parse JSON String as configurations that the task should query from AppDynamics.
     *
     * @param jsonString
     *         String of JSON configuration.
     * @return list of {@link AppInfo} configurations that the task should query from
     * AppDynamics.
     */
    public static List<AppInfo> parseInfo(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<List<AppInfo>>() {});
    }
}
