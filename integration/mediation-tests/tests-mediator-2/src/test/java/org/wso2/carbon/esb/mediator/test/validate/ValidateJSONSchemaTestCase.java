/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.esb.mediator.test.validate;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ValidateJSONSchemaTestCase extends ESBIntegrationTest {
    private Map<String, String> httpHeaders = new HashMap();

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        httpHeaders.put("Content-Type", "application/json");

    }

    @Test(groups = "wso2.esb", description = "Validating the request payload against the JSON Schema"
            + " stored in local-entry")
    public void validRequestTest1() throws Exception {
        String payload =
                "{\"msg\":{" + "  \"getQuote\": {" + "    \"request\": { \"symbol\": \"WSO2\" }" + "  }" + "}" + "}";
        HttpResponse response = doPost(new URL(getProxyServiceURLHttp("validateMediatorJsonTestProxy")), payload,
                httpHeaders);
        Assert.assertTrue((!response.getData().equals("") && response.getData().contains("getQuote")),
                "Valid Request failed. Response: " + response.getData());
    }

    // TODO enable this test case once https://github.com/wso2/micro-integrator/issues/388 is fixed
    @Test(groups = "wso2.esb", description = "Validating the request payload against the JSON Schema "
            + "stored in config registry", enabled = false)
    public void validRequestTest2() throws Exception {
        String payload = "{" + "  \"getQuote\": {" + "    \"request\": { \"symbol\": \"WSO2\" }" + "  }" + "}";
        HttpResponse response = doPost(new URL(getProxyServiceURLHttp("validateMediatorJsonSchemaFromRegTestProxy")),
                payload, httpHeaders);
        Assert.assertTrue((!response.getData().equals("") && response.getData().contains("getQuote")),
                "Valid Request failed. " + response.getData());
    }

    @Test(groups = "wso2.esb", description = "Validating the request payload against the JSON Schema"
            + " stored in local-entry")
    public void inValidRequestTest1() throws Exception {
        String payload =
                "{\"msg\":{" + "  \"getQuote\": {" + "    \"request\": { \"symbol1\": \"WSO2\" }" + "  }" + "}" + "}";
        HttpResponse response = doPost(new URL(getProxyServiceURLHttp("validateMediatorJsonTestProxy")), payload,
                httpHeaders);
        Assert.assertEquals(response.getResponseCode(), 500, "Response Code mismatched");
        Assert.assertTrue((!response.getData().equals("") && response.getData().contains("Invalid Request")),
                "Validation must be failed. Response: " + response.getData());
    }

    // TODO enable this test case once https://github.com/wso2/micro-integrator/issues/388 is fixed
    @Test(groups = "wso2.esb", description = "Validating the invalid request payload having invalid "
            + "element against the JSON Schema stored in config registry", enabled = false)
    public void inValidRequestTest2() throws Exception {
        String payload = "{" + "  \"getQuote\": {" + "    \"request\": { \"symbol1\": \"WSO2\" }" + "  }" + "}";
        HttpResponse response = doPost(new URL(getProxyServiceURLHttp("validateMediatorJsonSchemaFromRegTestProxy")),
                payload, httpHeaders);
        Assert.assertEquals(response.getResponseCode(), 500, "Response Code mismatched");
        Assert.assertTrue((!response.getData().equals("") && response.getData().contains("Invalid Request")),
                "Validation must be failed. Response:" + response.getData());
    }

    @Test(groups = "wso2.esb", description = "Validating the invalid request payload with Source path "
            + "against the JSON Schema stored in local-entry")
    public void inValidRequestTest3() throws Exception {
        String payload =
                "{\"body\":{" + "  \"getQuote\": {" + "    \"request\": { \"symbol\": \"WSO2\" }" + "  }" + "}" + "}";
        HttpResponse response = doPost(new URL(getProxyServiceURLHttp("validateMediatorJsonTestProxy")), payload,
                httpHeaders);
        Assert.assertEquals(response.getResponseCode(), 500, "Response Code mismatched");
        Assert.assertTrue((!response.getData().equals("") && response.getData().contains("Invalid Request")),
                "Validation must be failed. Response: " + response.getData());
    }

    @Test(groups = "wso2.esb", description = "Validating the empty request payload({}) against the JSON Schema"
            + " stored in local-entry")
    public void inValidRequestEmptyJSONMessageTest() throws Exception {
        String payload = "{}";
        HttpResponse response = doPost(new URL(getProxyServiceURLHttp("validateMediatorJsonTestProxy")), payload,
                httpHeaders);
        Assert.assertEquals(response.getResponseCode(), 500, "Response Code mismatched");
        Assert.assertTrue((!response.getData().equals("") && response.getData().contains("Invalid Request")),
                "Validation must be failed. Response: " + response.getData());
    }

    // TODO enable this test case once https://github.com/wso2/micro-integrator/issues/388 is fixed

    /**
     * Send a series of valid and invalid requests and see if
     * validation works in the expected way
     *
     * @throws Exception on a test exception
     */
    @Test(groups = "wso2.esb", description = "Validating the valid and invalid requests series against the JSON " +
            "Schema", enabled = false)
    public void validAndInvalidRequestTest() throws Exception {
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendValidRequestAndAssert();
        sendValidRequestAndAssert();
    }

    /**
     * Send a valid request and check the response
     *
     * @throws IOException                  on a communication exception
     * @throws AutomationFrameworkException on a test framework exception
     */
    private void sendValidRequestAndAssert() throws IOException, AutomationFrameworkException {
        String goodRequest = readFile(
                getESBResourceLocation() + File.separator + "mediatorconfig" + File.separator + "validate"
                        + File.separator + "validRequest.json");
        HttpResponse response = doPost(new URL(getApiInvocationURL("validateMediatorJson")), goodRequest, httpHeaders);
        Assert.assertTrue((response.getData().contains("success")),
                "Validation must be success. Response: " + response.getData());
    }

    /**
     * Send an invalid request and check the response
     *
     * @throws IOException                  on a communication exception
     * @throws AutomationFrameworkException on a test framework exception
     */
    private void sendInvalidRequestAndAssert() throws IOException, AutomationFrameworkException {
        String badRequest = readFile(
                getESBResourceLocation() + File.separator + "mediatorconfig" + File.separator + "validate"
                        + File.separator + "invalidRequest.json");
        HttpResponse response = doPost(new URL(getApiInvocationURL("validateMediatorJson")), badRequest, httpHeaders);
        Assert.assertTrue((response.getData().contains("fail")),
                "Validation must be fail. Response: " + response.getData());
    }

    /**
     * Read a file as an string
     *
     * @param path file path
     * @return content of the file as a String
     * @throws IOException on a read communication issue
     */
    private String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    @AfterClass(alwaysRun = true)
    public void clear() throws Exception {
        super.cleanup();
    }

    private static HttpResponse doPost(URL endpoint, String postBody, Map<String, String> headers)
            throws AutomationFrameworkException, IOException {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                throw new AutomationFrameworkException(
                        "Shouldn't happen: HttpURLConnection doesn't support POST?? " + e.getMessage(), e);
            }
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            for (Map.Entry<String, String> e : headers.entrySet()) {
                urlConnection.setRequestProperty((String) e.getKey(), (String) e.getValue());
            }
            OutputStream out = urlConnection.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                writer.write(postBody);
                writer.close();
            } catch (IOException e) {
                throw new AutomationFrameworkException("IOException while posting data " + e.getMessage(), e);
            } finally {
            }
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream(), Charset.defaultCharset()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException ignored) {
                //ignore
            }
            Iterator<String> itr = urlConnection.getHeaderFields().keySet().iterator();
            Object responseHeaders = new HashMap();
            String key;
            while (itr.hasNext()) {
                key = (String) itr.next();
                if (key != null) {
                    ((Map) responseHeaders).put(key, urlConnection.getHeaderField(key));
                }
            }
            return new HttpResponse(sb.toString(), urlConnection.getResponseCode(), (Map) responseHeaders);
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            rd = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream(), Charset.defaultCharset()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            return new HttpResponse(sb.toString(), urlConnection.getResponseCode());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}
