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

public class EndpointAddResult extends JGOResult
{
    private String path = null;
    private String msg = null, gc_key = null, req_id = null, resource = null;
    private String type = null, canonical_name = null;

    public EndpointAddResult(String path)
    {
        this.path = path;
    }

    public void addEndpoint(String username, String gridFTPServer, String myProxyServer, String serverDN,
                            boolean isGlobusConnect, boolean isPublic, String endpointName, JGOTransferAPIClient client)
        throws Exception
    {
        JSONObject jobj = new JSONObject();

        jobj.put("username", username);
        jobj.put("DATA_TYPE", "endpoint");
        jobj.put("activated", (Object)null);
        jobj.put("is_globus_connect", isGlobusConnect);

        // here, we must NOT include the full canonical name (i.e. user#epname)
        int pos = endpointName.indexOf("#");
        if (pos != -1)
        {
            endpointName = endpointName.substring(pos +1);
            jobj.put("name", endpointName);
            jobj.put("canonical_name", endpointName);
        }
        else
        { 
            jobj.put("name", endpointName);
            jobj.put("canonical_name", endpointName);
        }

        if (myProxyServer != null)
        {
            jobj.put("myproxy_server", myProxyServer);
        }
 
        JSONArray dataArr = new JSONArray();
        JSONObject dataObj = new JSONObject();
        String host = "";
        String port = "2811";
        String[] pieces = gridFTPServer.split(":");
        if (pieces != null)
        {
            host = pieces[0];
            if (pieces.length > 1)
            {
                port = pieces[1];
            }
        }

        dataObj.put("DATA_TYPE", "server");
        dataObj.put("hostname", host);
        dataObj.put("port", port);
        dataObj.put("uri", "gsiftp://" + host + ":" + port);
        dataObj.put("scheme", "gsiftp");
        if (serverDN != null)
        {
            dataObj.put("subject", serverDN);
        }
        dataArr.put(dataObj);
        jobj.put("DATA", dataArr);

        jobj.put("public", isPublic);


        String jsonData = jobj.toString();
        //System.out.println("SENDING POST: " + jsonData);
        HttpsURLConnection sConn = client.request("POST", this.path, jsonData);
        JSONArray results = client.getResult(sConn);
        if (results != null)
        {
            this.type = JGOUtils.extractFromResults(results, null, "DATA_TYPE");
            if (this.type.equals("endpoint_create_result"))
            {
                this.msg = JGOUtils.extractFromResults(results, null, "message");
                this.gc_key = JGOUtils.extractFromResults(results, null, "globus_connect_setup_key");
                this.req_id = JGOUtils.extractFromResults(results, null, "request_id");
                this.canonical_name = JGOUtils.extractFromResults(results, null, "canonical_name");
            }
            else
            {
                System.out.println("Got unknown result type: " + results);
            }
        }
    }

    public String toString()
    {
        StringBuffer strbuf = new StringBuffer("\n");
        if ((this.gc_key != null) && (!this.gc_key.equals("null")))
        {
            strbuf.append("Created the Globus Connect endpoint '");
            strbuf.append(this.canonical_name);
            strbuf.append("'.");
            strbuf.append("\n");
            strbuf.append("Use this setup key when installing Globus Connect:");
            strbuf.append("\n\t");
            strbuf.append(this.gc_key);
            strbuf.append("\n");
        }
        else
        {
            strbuf.append(this.msg);
            strbuf.append("\n");
        }
        return strbuf.toString();
    }
}