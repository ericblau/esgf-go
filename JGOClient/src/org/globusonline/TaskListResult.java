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

public class TaskListResult extends JGOResult
{
    public Vector<TaskResult> results = null;

    public TaskListResult()
    {
        this.results = new Vector<TaskResult>();
    }

    public void add(TaskResult tr)
    {
        results.add(tr);
    }

    public String toString()
    {
        int i = 0, len = this.results.size();
        StringBuffer strbuf = new StringBuffer("");
        TaskResult tr = null;
        for(i = 0; i < len; i++)
        {
            tr = this.results.get(i);

            strbuf.append("\n=== Task Details === [" + tr.index + "/" + tr.limit + "]");
            strbuf.append("\nTask ID          : " + tr.taskID);
            strbuf.append("\nTask Type        : " + tr.type);
            strbuf.append("\nParent Task ID   : n/a"); // FIXME: Support?
            strbuf.append("\nStatus           : " + tr.status);
            strbuf.append("\nRequest Time     : " + tr.requestTime);
            strbuf.append("\nDeadline         : " + tr.deadline);
            strbuf.append("\nCompletion Time  : " + tr.completionTime);
            strbuf.append("\nTotal Tasks      : " + tr.numTasks);
            strbuf.append("\nTasks Successful : " + tr.subtasks_succeeded);
            strbuf.append("\nTasks Canceled   : " + tr.subtasks_canceled);
            strbuf.append("\nTasks Failed     : " + tr.subtasks_failed);
            strbuf.append("\nTasks Pending    : " + tr.subtasks_pending);
            strbuf.append("\nTasks Retrying   : " + tr.subtasks_retrying);
            strbuf.append("\nCommand          : " + tr.command);
            strbuf.append("\nFiles            : " + tr.files);
            strbuf.append("\nDirectories      : " + tr.directories);
            strbuf.append("\nBytes Transferred: " + tr.bytesTransferred);
            strbuf.append("\nMBits/sec        : " + tr.MBitsPerSec);
            strbuf.append("\n");
        }
        return strbuf.toString();
    }
}