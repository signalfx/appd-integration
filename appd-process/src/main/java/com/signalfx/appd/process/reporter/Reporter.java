/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.process.reporter;

import java.util.List;

import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * Reporter handles reporting data points to SignalFx
 *
 * @author 9park
 */
public interface Reporter {

    /**
     * Report data points to SignalFx.
     *
     * @param dataPoints
     *         list of data points.
     * @throws ReportException
     *         when error occurs while sending dada points.
     */
    void report(List<SignalFxProtocolBuffers.DataPoint> dataPoints) throws ReportException;

    class ReportException extends Exception {
        public ReportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
