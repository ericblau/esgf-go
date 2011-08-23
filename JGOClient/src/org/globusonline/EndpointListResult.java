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

import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class EndpointListResult extends JGOResult
{
    public int total, limit, length, offset;
    public boolean list_verbose, list_public;
    public Vector<EndpointResult> results = null;

    public EndpointListResult()
    {
        results = new Vector<EndpointResult>();
    }

    public void add(EndpointResult er)
    {
        this.results.add(er);
    }

    public void createFromJSON(JSONObject jobj, String username, boolean list_public, boolean list_verbose)
        throws Exception
    {
        this.total = jobj.getInt("total");
        this.limit = jobj.getInt("limit");
        this.length = jobj.getInt("length");
        this.offset = jobj.getInt("offset");

        this.list_public = list_public;
        this.list_verbose = list_verbose;

        JSONArray dataArr = jobj.getJSONArray("DATA"), dataArr2 = null;
        for(int i = 0; i < dataArr.length(); i++)
        {
            JSONObject data = dataArr.getJSONObject(i);

            EndpointResult er = new EndpointResult();
            er.createFromJSON(data, username, list_public, list_verbose);

            this.add(er);
        }
    }

    public String toString()
    {
        StringBuffer strbuf = new StringBuffer("");
        for(EndpointResult er : this.results)
        {
            strbuf.append(er.toString());
        }
        return strbuf.toString();
    }
}