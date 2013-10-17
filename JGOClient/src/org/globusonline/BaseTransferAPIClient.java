/*
 * Copyright 2010 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.globusonline;

import java.io.*;

import java.util.Map;
import java.util.List;
import java.util.Iterator;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

/**
 * Basic client for interacting with the Globus Online Transfer API as a single
 * user, using x509 authentication and/or OAuth.
 *
 * Does not make any assumptions about how the application will parse data
 * or handle key and trust stores.
 */
public class BaseTransferAPIClient {
    protected String username;
    protected String baseUrl;
    protected String format;
    protected Authenticator authenticator;

    protected boolean verbose = false;

    protected int timeout = 30 * 1000; // 30 seconds, in milliseconds.

    protected KeyManager[] keyManagers;
    protected TrustManager[] trustManagers;
    protected SSLSocketFactory socketFactory;

    static final String CLIENT_VERSION = "JGOClient/1.0.4";

    static final String VERSION = "v0.10";
    static final String DEFAULT_BASE_URL =
        "https://transfer.api.globusonline.org/" + VERSION;
    static final String DEFAULT_TEST_BASE_URL =
        "https://transfer.test.api.globusonline.org/" + VERSION;

    static final String FORMAT_JSON = "application/json";
    static final String FORMAT_XML = "application/xml";
    static final String FORMAT_HTML = "application/xhtml+xml";
    static final String FORMAT_DEFAULT = FORMAT_JSON;

    public static void main(String[] args) {
        BaseTransferAPIClient c = new BaseTransferAPIClient(args[0],
                                        BaseTransferAPIClient.FORMAT_JSON);
        try {
            HttpsURLConnection r = c.request("GET", "/tasksummary");
            BaseTransferAPIClient.printResult(r);
            r.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BaseTransferAPIClient(String username, String format) {
        this(username, format, null, null, null);
    }

    public BaseTransferAPIClient(String username, String format,
                                 String baseUrl) {
        this(username, format, null, null, baseUrl);
    }

    /**
     * Create a client for the specified user.
     *
     * @param username  the Globus Online user to sign in to the API with.
     * @param format  the content type to request from the server for responses.
     *             Use one of the FORMAT_ constants.
     * @param trustManagers trust managers to use for the HTTPS connection
     *                      for validating the server certificate,
     *                      or null to use the default configured trust
     *                      managers.
     * @param keyManagers key managers to use for the HTTPS connection
     *                    for providing the client key and cert needed for
     *                    authentication, or null to use the default
     *                    configured key managers.
     * @param baseUrl  alternate base URL of the service; can be used to
     *                 connect to different versions of the API and instances
     *                 running on alternate servers. If null, the URL of
     *                 the latest version running on the production server
     *                 is used.
     */
    public BaseTransferAPIClient(String username, String format,
                                 TrustManager[] trustManagers,
                                 KeyManager[] keyManagers, String baseUrl)
    {
        this.username = username;
        this.format = format;
        if (baseUrl == null) {
            this.baseUrl = BaseTransferAPIClient.DEFAULT_BASE_URL;
        } else {
            this.baseUrl = baseUrl;
        }

        this.trustManagers = trustManagers;
        this.keyManagers = keyManagers;

        this.socketFactory = null;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public void setVerbose(boolean v)
    {
        this.verbose = v;
    }

    public void setAuthenticator(Authenticator authenticator)
    {
        this.authenticator = authenticator;
    }

    public HttpsURLConnection request(String method, String path)
          throws IOException, MalformedURLException, GeneralSecurityException,
                 APIError {
        return request(method, path, null, null);
    }

    public HttpsURLConnection request(String method, String path, String data,
                                      Map<String, String> queryParams)
          throws IOException, MalformedURLException, GeneralSecurityException,
                 APIError {
        if (! path.startsWith("/")) {
            path = "/" + path;
        }
        initSocketFactory(false);

        if (queryParams != null) {
            path += "?" + buildQueryString(queryParams);
        }

        URL url = new URL(this.baseUrl + path);
        if (this.verbose == true)
        {
            System.out.println("[***] Request Path: " + this.baseUrl + path);
        }
        HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
        c.setConnectTimeout(this.timeout);
        c.setSSLSocketFactory(this.socketFactory);
        c.setRequestMethod(method);
        c.setFollowRedirects(false);
        c.setUseCaches(false);
        c.setDoInput(true);
        c.setRequestProperty("X-Transfer-API-X509-User", this.username);
        c.setRequestProperty("X-Transfer-API-Client", this.CLIENT_VERSION);
        c.setRequestProperty("Accept", this.format);

        if (this.authenticator != null)
        {
            this.authenticator.authenticateConnection(c);
        }
        if (data != null)
        {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", this.format);
            c.setRequestProperty("Content-Length", "" + data.length());
        }
        c.connect();

        if (data != null) {
            DataOutputStream out = new DataOutputStream(c.getOutputStream());
            out.writeBytes(data);
            out.flush();
            out.close();
        }

        int statusCode = c.getResponseCode();
        if (statusCode >= 400) {
            String statusMessage = c.getResponseMessage();
            String errorHeader = null;
            Map<String, List<String>> headers = c.getHeaderFields();
            if (headers.containsKey("X-Transfer-API-Error")) {
                errorHeader = ((List<String>)
                               headers.get("X-Transfer-API-Error")).get(0);
            }
            throw constructAPIError(statusCode, statusMessage, errorHeader,
                                    c.getErrorStream());
        }
        return c;
    }

    /**
     * Parse an error response and return an APIError instance containing
     * the error data.
     *
     * Subclasses should override this with a method that parses the
     * response body according to the format used and fills in all the fields
     * of APIError. See JSONTransferAPIClient for an example.
     */
    protected APIError constructAPIError(int statusCode, String statusMessage,
                                         String errorCode, InputStream input) {
        return new APIError(statusCode, statusMessage, errorCode);
    }

    public void setConnectTimeout(int milliseconds) {
        this.timeout = milliseconds;
    }

    protected void initSocketFactory(boolean force)
                    throws KeyManagementException, NoSuchAlgorithmException {
        if (this.socketFactory == null || force) {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(this.keyManagers, this.trustManagers, null);
            this.socketFactory = context.getSocketFactory();
        }
    }

    public static void printResult(HttpsURLConnection c)
                    throws IOException, GeneralSecurityException, APIError {
        int code = c.getResponseCode();
        String message = c.getResponseMessage();
        System.out.println(code + " " + message);
        Map<String, List<String>> headers = c.getHeaderFields();
        Iterator headerIt = headers.entrySet().iterator();
        while (headerIt.hasNext()) {
            Map.Entry pair = (Map.Entry)headerIt.next();
            String key = (String)pair.getKey();
            if (key == null)
                continue;
            List<String> valueList = (List<String>) pair.getValue();
            Iterator valuesIt = valueList.iterator();
            while (valuesIt.hasNext()) {
                System.out.println(pair.getKey() + ": " + valuesIt.next());
            }
        }

        InputStream inputStream = null;
        if (code < 400)
            inputStream = c.getInputStream();
        else
            inputStream = c.getErrorStream();
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader in = new BufferedReader(reader);

        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
        }

        in.close();
    }

    public String getUsername() { return this.username; }
    public String getBaseUrl() { return this.baseUrl; }
    public String getFormat() { return this.format; }

    public static String buildQueryString(Map<String, String> map)
    throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (first)
                first = false;
            else
                builder.append("&");
            builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return builder.toString();
    }

    public static String endpointPath(String endpointName)
    throws UnsupportedEncodingException {
        return "/endpoint/" + URLEncoder.encode(endpointName);
    }
}
