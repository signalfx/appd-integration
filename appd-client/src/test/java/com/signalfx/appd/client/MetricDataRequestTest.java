/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.appd.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.signalfx.appd.client.exception.RequestException;
import com.signalfx.appd.client.exception.UnauthorizedException;
import com.signalfx.appd.client.model.MetricData;
import com.signalfx.appd.client.model.MetricValue;

public class MetricDataRequestTest {

    private Server server;
    private AppDTestHandler appDTestHandler;

    @Before
    public void setUp() throws Exception {
        appDTestHandler = new AppDTestHandler();

        server = new Server(0);
        server.setHandler(appDTestHandler);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        server = null;
    }

    @Test
    public void testGetMetric() throws Exception {
        MetricDataRequest metricDataRequest = getMetricDataRequest();
        List<MetricData> metricDataList = metricDataRequest.get();
        assertEquals(1, metricDataList.size());

        MetricData metricData = metricDataList.get(0);
        assertEquals("End User Experience|Device|Computer|AJAX Requests per Minute",
                metricData.metricPath);
        assertEquals(4, metricData.metricValues.size());

        MetricValue metricValue = metricData.metricValues.get(0);
        assertEquals(1435686360000L, metricValue.startTimeInMillis);
        assertEquals(57L, metricValue.value);
    }

    @Test
    public void testGetMetricUnauthorized() throws Exception {
        appDTestHandler.setStatus(HttpStatus.UNAUTHORIZED_401);
        MetricDataRequest metricDataRequest = getMetricDataRequest();
        try {
            metricDataRequest.get();
            fail("Unauthorized Exception Expected");
        } catch (UnauthorizedException e) {
            // Expected
        }
    }

    @Test
    public void testGetMetricUnhandled() throws Exception {
        appDTestHandler.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
        MetricDataRequest metricDataRequest = getMetricDataRequest();
        try {
            metricDataRequest.get();
            fail("Request Exception Expected");
        } catch (RequestException e) {
            // Expected
        }
    }

    @Test
    public void testServerNotAvailable() throws Exception {
        MetricDataRequest metricDataRequest = getMetricDataRequest();
        server.stop();
        try {
            metricDataRequest.get();
            fail("Request Exception Expected");
        } catch (RequestException e) {
            //Expected
        }
    }

    private MetricDataRequest getMetricDataRequest() {
        final int port = server.getConnectors()[0].getLocalPort();
        MetricDataRequest metricDataRequest = new MetricDataRequest("http://localhost:" + port,
                "user", "pass");
        metricDataRequest.setAppName("Any");
        metricDataRequest.setMetricPath("DontCare");
        metricDataRequest.setTimeParams(MetricDataRequest.TimeParams.beforeNow(2));
        return metricDataRequest;
    }

    @Test
    public void testTimeParams() throws Exception {
        assertEquals(new MetricDataRequest.TimeParams("AFTER_TIME", 100, 200, 0),
                MetricDataRequest.TimeParams.afterTime(100, 200));
        assertEquals(new MetricDataRequest.TimeParams("BEFORE_NOW", 100, 0, 0),
                MetricDataRequest.TimeParams.beforeNow(100));
        assertEquals(new MetricDataRequest.TimeParams("BEFORE_TIME", 100, 0, 200),
                MetricDataRequest.TimeParams.beforeTime(100, 200));
        assertEquals(new MetricDataRequest.TimeParams("BETWEEN_TIMES", 0, 100, 200),
                MetricDataRequest.TimeParams.betweenTime(100, 200));
    }

    private class AppDTestHandler extends AbstractHandler {

        private int status = HttpStatus.OK_200;

        public void setStatus(int status) {
            this.status = status;
        }

        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response)
                throws IOException, ServletException {
            String responseString =
                    IOUtils.toString(
                            getClass().getResourceAsStream(
                                    String.format("/metric_response_%d.json", status)));
            response.setStatus(status);
            response.getWriter().write(responseString);
            baseRequest.setHandled(true);
        }
    }
}
