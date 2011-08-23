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

public class TaskSummaryResult extends JGOResult
{
    public int succeeded, failed, active, inactive;

    public TaskSummaryResult()
    {
    }

    public void createFromJSON(JSONObject jobj) throws Exception
    {
        this.succeeded = jobj.getInt("succeeded");
        this.failed = jobj.getInt("failed");
        this.active = jobj.getInt("active");
        this.inactive = jobj.getInt("inactive");
    }

    public String toString()
    {
        StringBuffer strbuf = new StringBuffer("=== Task Summary ===");
        strbuf.append("\nsucceeded: " + this.succeeded);
        strbuf.append("\nfailed   : " + this.failed);
        strbuf.append("\nactive   : " + this.active);
        strbuf.append("\ninactive : " + this.inactive);
        strbuf.append("\n");
        return strbuf.toString();
    }
}