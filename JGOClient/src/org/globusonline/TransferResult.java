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

import javax.net.ssl.HttpsURLConnection;

public class TransferResult extends JGOResult
{
    public String transferID = null, taskID = null, message = null;
    public JSONArray results = null, dataPairArr = null, dataArr = null;

    public TransferResult(JSONArray results)
    {
        this.results = results;
    }

    public void addSourceDestPathPair(String username, String sourcePath, String destPath)
        throws Exception
    {
        int pos = sourcePath.indexOf("/");
        if (pos == -1)
        {
            throw new Exception("Invalid Source Endpoint format: " + sourcePath);
        }
        String sourceEndpoint = sourcePath.substring(0, pos);
        sourcePath = sourcePath.substring(pos);

        pos = destPath.indexOf("/");
        if (pos == -1)
        {
            throw new Exception("Invalid Destination Endpoint format: " + destPath);
        }
        String destEndpoint = destPath.substring(0, pos);
        destPath = destPath.substring(pos);

        // if (opts.verbose)
        // {
        //     System.out.println("Source      Path          : " + sourcePath);
        //     System.out.println("Destination Path          : " + destPath);
        //     System.out.println("Using source      endpoint: " + sourceEndpoint);
        //     System.out.println("Using destination endpoint: " + destEndpoint);
        // }

        JSONObject jobj = this.results.getJSONObject(0);
        this.transferID = jobj.getString("value");
        // if (opts.verbose)
        // {
        // System.out.println("Retrieved TransferID: " + this.transferID);
        // }

        JSONObject data = new JSONObject();
        if (sourcePath.endsWith("/"))
        {
            data.put("recursive", true);
        }
        else
        {
            data.put("recursive", false);
        }
        data.put("source_path", sourcePath);
        data.put("source_endpoint", sourceEndpoint);
        data.put("destination_path", destPath);
        data.put("destination_endpoint", destEndpoint);
        data.put("DATA_TYPE", "transfer_item");

        // FIXME: Make this a member var to add to
        if (this.dataPairArr == null)
        {
            this.dataPairArr = new JSONArray();
        }
        this.dataPairArr.put(data);
    }

    public void postTransfer(JGOTransferAPIClient client, String timeout)
    {
        try
        {
            if (this.dataArr == null)
            {
                // FIXME: RESPECT PASSED IN TIMEOUT if not null
                //System.out.println("Timeout                   : " + timeout);
                // set deadline to 1 day and 6 hours (i.e. 30 hours) from current time
                long dateMS = System.currentTimeMillis() + (30*60*60*1000);
                Date deadline = new Date(dateMS);

                String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

                JSONObject jobj = new JSONObject();
                jobj.put("DATA", this.dataPairArr);
                jobj.put("length", this.dataPairArr.length());
                jobj.put("deadline", sdf.format(deadline));
                // v0.9
                //jobj.put("transfer_id", this.transferID);

                // v0.10
                jobj.put("submission_id", this.transferID);
                jobj.put("DATA_TYPE", "transfer");

                this.dataArr = new JSONArray();
                this.dataArr.put(jobj);
            }

            // System.out.println("DATA ARR:\n");
            // System.out.println(this.dataArr);

            // finally, we can post this information back to the server's transfer method
            String path = "/transfer";
            String jsonData = this.dataArr.toString();
            jsonData = jsonData.substring(1, jsonData.length() - 1);

            this.message = "Initiating Globus.org Transfer\n";
            HttpsURLConnection sConn = client.request("POST", path, jsonData);

            if (sConn.getResponseCode() == JGOConstants.JGO_TRANSFER_SUCCESS)
            {
                JSONArray results = client.getResult(sConn);
                JSONObject jobj = results.getJSONObject(0);

                this.message += jobj.getString("message");
                this.taskID = jobj.getString("task_id");
            }
            else
            {
                this.message += "Transfer FAILED (HTTP Error code " + sConn.getResponseCode()
                    + ").  This Transfer cannot be started.";
                BaseTransferAPIClient.printResult(sConn);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    public void createFromJSON(JSONObject jobj) throws Exception
    {
    }

    public String toString()
    {
        StringBuffer strbuf = new StringBuffer("\n");
        strbuf.append(this.message);
        strbuf.append("\nTask ID: " + this.taskID);
        strbuf.append("\n");
        return strbuf.toString();
    }
}