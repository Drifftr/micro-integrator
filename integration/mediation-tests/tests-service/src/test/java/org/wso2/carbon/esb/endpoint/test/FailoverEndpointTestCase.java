/**
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.endpoint.test;

import java.io.IOException;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.esb.integration.common.clients.endpoint.EndPointAdminClient;
import org.wso2.esb.integration.common.clients.registry.ResourceAdminServiceClient;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.ESBTestConstant;
import org.wso2.esb.integration.common.utils.clients.LoadbalanceFailoverClient;
import org.wso2.esb.integration.common.utils.clients.axis2client.AxisServiceClientUtils;
import org.wso2.esb.integration.common.utils.servers.axis2.SampleAxis2Server;

public class FailoverEndpointTestCase extends ESBIntegrationTest {

    private EndPointAdminClient endPointAdminClient;
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private SampleAxis2Server axis2Server1;
    private SampleAxis2Server axis2Server2;
    private SampleAxis2Server axis2Server3;
    private LoadbalanceFailoverClient lbClient;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();

        axis2Server1 = new SampleAxis2Server("test_axis2_server_9001.xml");
        axis2Server2 = new SampleAxis2Server("test_axis2_server_9002.xml");
        axis2Server3 = new SampleAxis2Server("test_axis2_server_9003.xml");

        axis2Server1.deployService(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE);
        axis2Server1.deployService(SampleAxis2Server.LB_SERVICE_1);
        axis2Server1.start();

        axis2Server2.deployService(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE);
        axis2Server2.deployService(SampleAxis2Server.LB_SERVICE_2);
        axis2Server2.start();

        axis2Server3.deployService(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE);
        axis2Server3.start();

        //Test weather all the axis2 servers are up and running
        OMElement response = axis2Client
                .sendSimpleStockQuoteRequest(getBackEndServiceUrl(ESBTestConstant.SIMPLE_STOCK_QUOTE_SERVICE), null,
                                             "WSO2");
        Assert.assertTrue(response.toString().contains("WSO2 Company"));
        response = axis2Client
                .sendSimpleStockQuoteRequest("http://localhost:9001/services/SimpleStockQuoteService", null, "WSO2");
        Assert.assertTrue(response.toString().contains("WSO2 Company"));
        response = axis2Client
                .sendSimpleStockQuoteRequest("http://localhost:9002/services/SimpleStockQuoteService", null, "WSO2");
        Assert.assertTrue(response.toString().contains("WSO2 Company"));
        response = axis2Client
                .sendSimpleStockQuoteRequest("http://localhost:9003/services/SimpleStockQuoteService", null, "WSO2");
        Assert.assertTrue(response.toString().contains("WSO2 Company"));

        lbClient = new LoadbalanceFailoverClient();
    }

    @AfterClass(alwaysRun = true)
    public void close() throws Exception {
        log.info("Tests Are Completed");
        if (axis2Server1.isStarted()) {
            axis2Server1.stop();
        }
        if (axis2Server2.isStarted()) {
            axis2Server2.stop();
        }
        if (axis2Server3.isStarted()) {
            axis2Server3.stop();
        }
        axis2Server1 = null;
        axis2Server2 = null;
        axis2Server3 = null;
        resourceAdminServiceClient = null;
        endPointAdminClient = null;
        lbClient = null;
        super.cleanup();
    }

    @AfterMethod(groups = "wso2.esb")
    public void startServersA() throws InterruptedException, IOException {
        if (!axis2Server1.isStarted()) {
            axis2Server1.start();
        }
        if (!axis2Server2.isStarted()) {
            axis2Server2.start();
        }
        if (!axis2Server3.isStarted()) {
            axis2Server3.start();
        }
        Thread.sleep(1000);
    }

    @BeforeMethod(groups = "wso2.esb")
    public void startServersB() throws InterruptedException, IOException {
        if (!axis2Server1.isStarted()) {
            axis2Server1.start();
        }
        if (!axis2Server2.isStarted()) {
            axis2Server2.start();
        }
        if (!axis2Server3.isStarted()) {
            axis2Server3.start();
        }
        Thread.sleep(1000);
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @Test(groups = "wso2.esb", description = "Test sending request to Fail Over Endpoint")
    public void testSendingFailOverEndpoint() throws IOException, InterruptedException {

        OMElement response = axis2Client
                .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("failoverEndPoint"), null, "WSO2");
        Assert.assertTrue(response.toString().contains("WSO2 Company"));

        axis2Server1.stop();
        response = axis2Client.sendSimpleStockQuoteRequest(getProxyServiceURLHttp("failoverEndPoint"), null, "WSO2");
        Assert.assertTrue(response.toString().contains("WSO2 Company"));

        axis2Server1.start();
        axis2Server2.stop();

        Thread.sleep(1000);

        int counter = 0;
        while (!AxisServiceClientUtils.isServiceAvailable("http://localhost:9001/services/SimpleStockQuoteService")) {
            if (counter > 100) {
                break;
            }
            counter++;
        }

        if (counter > 100) {
            throw new AssertionError("Axis2 Server didn't started with in expected time period.");
        } else {

            response = axis2Client
                    .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("failoverEndPoint"), null, "WSO2");
            Assert.assertTrue(response.toString().contains("WSO2 Company"));

            axis2Server2.start();
            axis2Server3.stop();

            Thread.sleep(1000);

            counter = 0;
            while (!AxisServiceClientUtils
                    .isServiceAvailable("http://localhost:9002/services/SimpleStockQuoteService")) {
                if (counter > 100) {
                    break;
                }
                counter++;
            }

            if (counter > 100) {
                throw new AssertionError("Axis2 Server didn't started with in expected time period.");
            } else {
                response = axis2Client
                        .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("failoverEndPoint"), null, "WSO2");
                Assert.assertTrue(response.toString().contains("WSO2 Company"));
            }

        }
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @Test(groups = "wso2.esb", description = "Test sending request to Fail Over Endpoint in Config Registry")
    public void testSendingFailOverEndpoint_ConfigReg() throws IOException, InterruptedException {

        OMElement response = axis2Client
                .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("failoverEndPoint_Config_Reg"), null, "WSO2");
        Assert.assertTrue(response.toString().contains("WSO2 Company"));

        axis2Server1.stop();
        response = axis2Client
                .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("failoverEndPoint_Config_Reg"), null, "WSO2");
        Assert.assertTrue(response.toString().contains("WSO2 Company"));

        axis2Server1.start();
        axis2Server2.stop();

        Thread.sleep(1000);

        int counter = 0;
        while (!AxisServiceClientUtils.isServiceAvailable("http://localhost:9001/services/SimpleStockQuoteService")) {
            if (counter > 100) {
                break;
            }
            counter++;
        }

        if (counter > 100) {
            throw new AssertionError("Axis2 Server didn't started with in expected time period.");
        } else {

            response = axis2Client
                    .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("failoverEndPoint_Config_Reg"), null, "WSO2");
            Assert.assertTrue(response.toString().contains("WSO2 Company"));

            axis2Server2.start();
            axis2Server3.stop();

            Thread.sleep(1000);

            counter = 0;
            while (!AxisServiceClientUtils
                    .isServiceAvailable("http://localhost:9002/services/SimpleStockQuoteService")) {
                if (counter > 100) {
                    break;
                }
                counter++;
            }

            if (counter > 100) {
                throw new AssertionError("Axis2 Server didn't started with in expected time period.");
            } else {
                response = axis2Client
                        .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("failoverEndPoint_Config_Reg"), null,
                                                     "WSO2");
                Assert.assertTrue(response.toString().contains("WSO2 Company"));
            }

        }
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @Test(groups = {"wso2.esb"})
    public void testFailOverEndpoint() throws Exception {
        checkEndpointExistence("failoverEndpoint");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @Test(groups = "wso2.esb",
          description = "Test sending request to Fail Over Endpoint which Suspend Endpoints to Specific Errors")
    public void testSendingFailOverEndpoint_With_Specific_Errors() throws IOException, InterruptedException {
        //Check the fail over endpoint is functioning well
        String response = lbClient
                .sendLoadBalanceRequest(getProxyServiceURLHttp("failoverEndPoint_Specific_Errors"), null);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.toString().contains("Response from server: Server_1"),
                          "Failover endpoint is working fine, reply is coming from endpoint one");

        //Stop one server to generate a failure in one endpoint
        //But the suspend cant be triggered here. because suspend the endpoint happen only for Timeouts 101504,101508
        axis2Server1.stop();
        //since retriesBeforeSuspension is 3
        for (int i = 0; i < 4; i++) {
            try {
                lbClient.sendLoadBalanceRequest(getProxyServiceURLHttp("failoverEndPoint_Specific_Errors"), null,
                                                "5000");
            } catch (AxisFault e) {
                Assert.assertFalse(e.getMessage().equalsIgnoreCase(ESBTestConstant.READ_TIME_OUT),
                                   "The client was timeout due to Failover Logic doesn't retry for connection refused");
            }

        }
        //3 attempt is over and 4th attempt made endpoint suspend
        //now the end point 9001 Server is suspend.
        axis2Server1.start();

        int counter = 0;
        while (!AxisServiceClientUtils.isServiceAvailable("http://localhost:9001/services/LBService1")) {
            if (counter > 100) {
                break;
            }
            counter++;
        }

        if (counter > 100) {
            throw new AssertionError("Axis2 Server didn't started with in expected time period.");
        } else {
            //Checaxiaxis2Server1.start()s2Server1.stop()k that the endpoint one is suspended.
            //If reply comes that means not suspended.
            //Because suspend duration of the endpoint is 20 seconds. So reply cant come in this time.
            response = lbClient
                    .sendLoadBalanceRequest(getProxyServiceURLHttp("failoverEndPoint_Specific_Errors"), null);
            Assert.assertNotNull(response);
            Assert.assertFalse(response.toString().contains("Response from server: Server_1"),
                               "Endpoint 1 is not suspended so reply coming from endpoint 1");

            //Invoke a web service method which will invoke a time out and cause the endpoint to suspend.
            response = lbClient
                    .sendSleepRequest(getProxyServiceURLHttp("failoverEndPoint_Specific_Errors"), "3000", "1000000");

            //Invoke a web service method which will invoke a time out and cause the endpoint to suspend.
            response = lbClient
                    .sendLoadBalanceRequest(getProxyServiceURLHttp("failoverEndPoint_Specific_Errors"), null);
            Assert.assertNotNull(response);
            Assert.assertTrue(response.toString().contains("Response from server: Server_2"),
                              "Endpoint 1 is suspended so reply coming from endpoint 2");
        }
    }
}
