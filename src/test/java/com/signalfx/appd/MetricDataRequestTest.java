package com.signalfx.appd;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

import com.signalfx.appd.model.MetricData;
import com.signalfx.appd.request.MetricDataRequest;
import com.signalfx.appd.config.ConnectionConfig;

public class MetricDataRequestTest {

    @Test
    public void testGetMetric() throws Exception {
        Server server = new Server(0);
        server.setHandler(new AppDTestHandler());
        server.start();
        final int port = server.getConnectors()[0].getLocalPort();
        ConnectionConfig connectionConfig = new ConnectionConfig("user1", "pass1",
                "http://localhost:" + port, "fxtoken");
        MetricDataRequest metricDataRequest = new MetricDataRequest(connectionConfig);
        metricDataRequest.setAppName("Any");
        metricDataRequest.setMetricPath("DontCare");
        metricDataRequest.setTimeParams(MetricDataRequest.TimeParams.beforeNow(2));
        List<MetricData> metricDataList = metricDataRequest.get();
        server.stop();

        assertEquals(1, metricDataList.size());
        MetricData metricData = metricDataList.get(0);
        assertEquals("End User Experience|Device|Computer|AJAX Requests per Minute", metricData.metricPath);
        assertEquals(4, metricData.metricValues.size());
    }

    private class AppDTestHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response)
                throws IOException, ServletException {
            String appdJsonResponse =
                    IOUtils.toString(getClass().getResourceAsStream("/appd_metric_data_response.json"));
            response.setStatus(HttpStatus.OK_200);
            response.getWriter().write(appdJsonResponse);
            baseRequest.setHandled(true);
        }
    }
}
