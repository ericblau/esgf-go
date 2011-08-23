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

public class EndpointRemoveResult extends JGOResult
{
    private String path = null;
    private String msg = null, code = null, req_id = null, resource = null, type = null;

    public EndpointRemoveResult(String path)
    {
        this.path = path;
    }

    public void removeEndpoint(String username, String endpointName, JGOTransferAPIClient client)
        throws Exception
    {
        HttpsURLConnection sConn = client.request("DELETE", this.path);
        JSONArray results = client.getResult(sConn);
        if (results != null)
        {
            this.type = JGOUtils.extractFromResults(results, "DATA_TYPE");
            if (this.type.equals("result"))
            {
                this.msg = JGOUtils.extractFromResults(results, "message");
                this.code = JGOUtils.extractFromResults(results, "code");
                this.resource = JGOUtils.extractFromResults(results, "resource");
                this.req_id = JGOUtils.extractFromResults(results, "request_id");
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
        strbuf.append(this.msg);
        strbuf.append("\n");
        return strbuf.toString();
    }
}