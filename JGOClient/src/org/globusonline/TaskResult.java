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

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class TaskResult extends JGOResult
{
    public double MBitsPerSec;
    public long bytesTransferred;
    public String requestTime, completionTime;
    public String taskID, type, status, deadline, command;
    public int numTasks, index, limit, files, directories;
    public int subtasks_succeeded, subtasks_canceled, subtasks_failed, subtasks_pending, subtasks_retrying;

    public TaskResult()
    {
    }

    public void createFromJSON(JSONObject jobj, int index, int limit, int total) throws Exception
    {
        JSONArray dataArr = null, linkArr = null;
        JSONObject data = null, link = null;

        this.MBitsPerSec = 0;
        this.requestTime = jobj.getString("request_time");
        this.completionTime = jobj.getString("completion_time");
        this.bytesTransferred = jobj.getLong("bytes_transferred");

        if ((this.requestTime != null) && (!this.requestTime.equals("null")) &&
            (this.completionTime != null) && (!this.completionTime.equals("null")))
        {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Date d1 = df.parse(this.requestTime);
            Date d2 = df.parse(this.completionTime);

            long timeDiffSecs = ((d2.getTime() - d1.getTime()) / 1000);
            double MBitsTransferred = (double)((this.bytesTransferred / (1024L * 1024L)) * 8);
            this.MBitsPerSec = (double)((timeDiffSecs > 0) ? (MBitsTransferred / timeDiffSecs) : 0);
        }

        this.taskID = jobj.getString("task_id");
        this.type = jobj.getString("type");
        this.status = jobj.getString("status");
        this.deadline = jobj.getString("deadline");
        this.index = index;
        this.limit = limit;
        this.numTasks = total;
        this.subtasks_succeeded = jobj.getInt("subtasks_succeeded");
        this.subtasks_canceled = jobj.getInt("subtasks_canceled");
        this.subtasks_failed = jobj.getInt("subtasks_failed");
        this.subtasks_pending = jobj.getInt("subtasks_pending");
        this.subtasks_retrying = jobj.getInt("subtasks_retrying");
        this.command = jobj.getString("command");
        this.files = jobj.getInt("files");
        this.directories = jobj.getInt("directories");

        // if (verbose)
        // {
        //     System.out.println("subtasks_expired " + jobj.get("subtasks_expired"));
        //     System.out.println("username            = " + jobj.get("username"));
        //     System.out.println("DATA_TYPE           = " + jobj.get("DATA_TYPE"));

        //     linkArr = jobj.getJSONArray("LINKS");
        //     for(int i = 0; i < linkArr.length(); i++)
        //     {
        //         link = linkArr.getJSONObject(i);
        //         System.out.println("LINK[" + i + "] = (resource=" + link.get("resource") +
        //                            ", rel=" + link.get("rel") + "href=" + link.get("href") + ")");
        //     }
        // }
    }

    public String toString()
    {
        StringBuffer strbuf = new StringBuffer("\n=== Task Details === [" + this.index + "/" + this.limit + "]");
        strbuf.append("\nTask ID          : " + this.taskID);
        strbuf.append("\nTask Type        : " + this.type);
        strbuf.append("\nParent Task ID   : n/a"); // FIXME: Support?
        strbuf.append("\nStatus           : " + this.status);
        strbuf.append("\nRequest Time     : " + this.requestTime);
        strbuf.append("\nDeadline         : " + this.deadline);
        strbuf.append("\nCompletion Time  : " + this.completionTime);
        strbuf.append("\nTotal Tasks      : " + this.numTasks);
        strbuf.append("\nTasks Successful : " + this.subtasks_succeeded);
        strbuf.append("\nTasks Canceled   : " + this.subtasks_canceled);
        strbuf.append("\nTasks Failed     : " + this.subtasks_failed);
        strbuf.append("\nTasks Pending    : " + this.subtasks_pending);
        strbuf.append("\nTasks Retrying   : " + this.subtasks_retrying);
        strbuf.append("\nCommand          : " + this.command);
        strbuf.append("\nFiles            : " + this.files);
        strbuf.append("\nDirectories      : " + this.directories);
        strbuf.append("\nBytes Transferred: " + this.bytesTransferred);
        strbuf.append("\nMBits/sec        : " + this.MBitsPerSec);
        strbuf.append("\n");
        return strbuf.toString();
    }
}