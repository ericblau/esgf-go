/*
 * Copyright 2011 University of Chicago
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

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import javax.net.ssl.HttpsURLConnection;

public class ActivationRequirementResult extends JGOResult
{
    public int length;
    JSONObject data;
    JSONArray results;
    public String activationMessage;
    public String activated, auto_activation_supported, expire_time, subject;
    public String description, name, value, ui_name, type, required, DATA_TYPE;
    private String MKPROXY_PATH = "/usr/local/bin/mkproxy";

    public ActivationRequirementResult()
    {
    }

    public void setMkProxyPath(String mkProxyPath)
    {
        this.MKPROXY_PATH = mkProxyPath;
    }

    public void createFromJSONArray(JSONArray results)
        throws Exception
    {
        JSONObject jobj = results.getJSONObject(0);
        JSONArray dataArr = jobj.getJSONArray("DATA");

        this.results = results;

        this.activated = jobj.getString("activated");
        this.length = jobj.getInt("length");
        this.auto_activation_supported = jobj.getString("auto_activation_supported");
        this.expire_time = jobj.getString("expire_time");

        if ((dataArr != null) && (dataArr.length() > 0))
        {
            this.data = dataArr.getJSONObject(0);

            this.description = this.data.getString("description");
            this.name = this.data.getString("name");
            this.value = this.data.getString("value");
            this.ui_name = this.data.getString("ui_name");
            this.type = this.data.getString("type");
            this.required = this.data.getString("required");
            this.DATA_TYPE = this.data.getString("DATA_TYPE");
        }
    }

    public boolean activateViaDelegation(String goEndpoint, String certFile, String lifetimeInHours, JGOTransferAPIClient client) throws Exception
    {
        boolean ret = false;

        String publicKey = null;
        JSONObject proxyChainObj = null;
        JSONObject jobj = this.results.getJSONObject(0);
        JSONArray dataArr = jobj.getJSONArray("DATA");

        if (lifetimeInHours == null)
        {
            lifetimeInHours = "12";
        }
        for(int i = 0; i < dataArr.length(); i++)
        {
            this.data = dataArr.getJSONObject(i);

            if ((this.data.get("name") != null) && (this.data.get("name").equals("lifetime_in_hours")))
            {
                this.data.put("value", lifetimeInHours);
            }
            else if ((this.data.get("name") != null) && (this.data.get("name").equals("public_key")))
            {
                publicKey = this.data.getString("value");
            }
            else if ((this.data.get("name") != null) && (this.data.get("name").equals("proxy_chain")))
            {
                proxyChainObj = this.data;
            }
        }

        if (!new File(MKPROXY_PATH).exists())
        {
            throw new Exception(
                "Delegation was requested, but JGOClient cannot locate mkproxy at configured location: " +
                MKPROXY_PATH);
        }
        if ((publicKey == null) || (proxyChainObj == null))
        {
            throw new Exception("Delegation was requested but cannot be completed by the Endpoint");
        }
        if (client.getOptions().verbose)
        {
            System.out.println("public key: " + publicKey);
        }

        String cred = this.readEntireFile(certFile);
        String certChain = createProxyCertificate(
            MKPROXY_PATH, publicKey, cred, Integer.parseInt(lifetimeInHours));

        if (client.getOptions().verbose)
        {
            System.out.println("cert chain: " + certChain);
        }

        proxyChainObj.put("value", certChain);

        StringBuffer path = this.buildPath(goEndpoint, false);

        String jsonData = this.results.toString();
        jsonData = jsonData.substring(1, jsonData.length() - 1);
        HttpsURLConnection sConn = client.request("POST", path.toString(), jsonData);
        this.results = client.getResult(sConn);

        jobj = this.results.getJSONObject(0);
        this.activationMessage = jobj.getString("message");
        if (this.activationMessage.indexOf("activated successfully") != -1)
        {
            ret = true;
        }

        this.subject = jobj.getString("subject");
        this.expire_time = jobj.getString("expire_time");

        return ret;
    }

    public boolean activate(String myProxyServer, String goEndpoint, String myProxyUser,
                            String myProxyPassword, String lifetimeInHours, JGOTransferAPIClient client) throws Exception
    {
        boolean ret = false;

        String publicKey = null;
        JSONObject proxyChainObj = null;
        JSONObject jobj = this.results.getJSONObject(0);
        JSONArray dataArr = jobj.getJSONArray("DATA");

        if (lifetimeInHours == null)
        {
            lifetimeInHours = "12";
        }
        for(int i = 0; i < dataArr.length(); i++)
        {
            this.data = dataArr.getJSONObject(i);

            if ((this.data.get("name") != null) && (this.data.get("name").equals("hostname")))
            {
                this.data.put("value", myProxyServer);
            }
            else if ((this.data.get("name") != null) && (this.data.get("name").equals("username")))
            {
                this.data.put("value", myProxyUser);
            }
            else if ((this.data.get("name") != null) && (this.data.get("name").equals("passphrase")))
            {
                this.data.put("value", myProxyPassword);
            }
            else if ((this.data.get("name") != null) && (this.data.get("name").equals("lifetime_in_hours")))
            {
                this.data.put("value", lifetimeInHours);
            }
        }

        StringBuffer path = this.buildPath(goEndpoint, false);

        String jsonData = this.results.toString();
        jsonData = jsonData.substring(1, jsonData.length() - 1);
        HttpsURLConnection sConn = client.request("POST", path.toString(), jsonData);
        this.results = client.getResult(sConn);

        jobj = this.results.getJSONObject(0);
        this.activationMessage = jobj.getString("message");
        if (this.activationMessage.indexOf("activated successfully") != -1)
        {
            ret = true;
        }

        this.subject = jobj.getString("subject");
        this.expire_time = jobj.getString("expire_time");

        return ret;
    }

    public boolean autoActivate(String myProxyServer, String goEndpoint,
                                JGOTransferAPIClient client) throws Exception
    {
        boolean ret = false;

        JSONObject jobj = this.results.getJSONObject(0);
        JSONArray dataArr = jobj.getJSONArray("DATA");

        for(int i = 0; i < dataArr.length(); i++)
        {
            this.data = dataArr.getJSONObject(i);

            if ((this.data.get("name") != null) && (this.data.get("name").equals("hostname")))
            {
                this.data.put("value", myProxyServer);
            }
        }

        StringBuffer path = this.buildPath(goEndpoint, true);

        HttpsURLConnection sConn = client.request("POST", path.toString(), "");
        this.results = client.getResult(sConn);

        jobj = this.results.getJSONObject(0);
        this.activationMessage = jobj.getString("message");
        if (this.activationMessage.indexOf("activated successfully") != -1)
        {
            ret = true;
        }

        this.subject = jobj.getString("subject");
        this.expire_time = jobj.getString("expire_time");

        return ret;
    }

    private StringBuffer buildPath(String goEndpoint, boolean autoActivate)
    {
        StringBuffer path = new StringBuffer("");
        String ep = goEndpoint.replace("#", "%23");
        path.append("endpoint/");
        path.append(ep);
        if (autoActivate == true)
        {
            path.append("/autoactivate");
        }
        else
        {
            path.append("/activate");
        }

        return path;
    }

    public String toString()
    {
        if (this.activationMessage != null)
        {
            StringBuffer strbuf = new StringBuffer(this.activationMessage);
            strbuf.append("\n");
            return strbuf.toString();
        }
        return "";
    }

    /**
     * Create a proxy certificate using the provided public key and signed
     * by the provided credential.
     *
     * Appends the certificate chain to proxy certificate, and returns as PEM.
     * Uses an external program to construct the certificate; see
     * https://github.com/globusonline/transfer-api-client-python/tree/master/mkproxy
     * @param mkproxyPath Absolute path of mkproxy program.
     * @param publicKeyPem String containing a PEM encoded RSA public key
     * @param credentialPem String containing a PEM encoded credential, with
     * certificate, private key, and trust chain.
     * @param hours Hours the certificate will be valid for.
     */
    public static String createProxyCertificate(String mkproxyPath,
                                                String publicKeyPem,
                                                String credentialPem,
                                                int hours)
        throws IOException, InterruptedException {
        Process p = new ProcessBuilder(mkproxyPath, "" + hours).start();

        DataOutputStream out = new DataOutputStream(p.getOutputStream());
        out.writeBytes(publicKeyPem);
        out.writeBytes(credentialPem);
        out.close();

        p.waitFor();
        InputStreamReader in = new InputStreamReader(p.getInputStream());
        String certChain = readEntireStream(in);
        return certChain;
    }

    private static String readEntireFile(String filename) throws IOException {
        FileReader in = new FileReader(filename);
        return readEntireStream(in);
    }

    private static String readEntireStream(InputStreamReader in)
        throws IOException {
        StringBuilder contents = new StringBuilder();
        char[] buffer = new char[4096];
        int read = 0;
        do {
            contents.append(buffer, 0, read);
            read = in.read(buffer);
        } while (read >= 0);
        return contents.toString();
    }
}