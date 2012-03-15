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

    public ActivationRequirementResult()
    {
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

    public boolean activate(String myProxyServer, String myProxyEndpoint, String myProxyUser,
                            String myProxyPassword, String lifetimeInHours, JGOTransferAPIClient client) throws Exception
    {
        boolean ret = false;

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

        StringBuffer path = this.buildPath(myProxyEndpoint, false);

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

    public boolean autoActivate(String myProxyServer, String myProxyEndpoint,
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

        StringBuffer path = this.buildPath(myProxyEndpoint, true);

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

    private StringBuffer buildPath(String myProxyEndpoint, boolean autoActivate)
    {
        StringBuffer path = new StringBuffer("");
        String ep = myProxyEndpoint.replace("#", "%23");
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
}