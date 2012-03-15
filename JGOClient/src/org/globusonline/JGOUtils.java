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

import java.io.*;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import javax.net.ssl.HttpsURLConnection;

public class JGOUtils
{
    public static boolean opArgHasValue(String[] args, String value)
    {
        boolean found = false;
        if (args != null)
        {
            for (String tmp: args)
            {
                if (tmp.equals(value))
                {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    public static String opArgGetValue(String[] args, String key)
    {
        String value = null;
        if (args != null)
        {
            for(int i = 0; i < args.length; i++)
            {
                if (args[i].equals(key))
                {
                    if ((i+1) < args.length)
                    {
                        value = args[++i];
                    }
                    break;
                }
            }
        }
        return value;
    }

    public static String getPath(String username, String op, String[] opArgs)
        throws Exception
    {
        if ((op == null) || (username == null))
        {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        if (op.equals("tasksummary"))
        {
            sb.append("/tasksummary");
        }
        else if (op.equals("task"))
        {
            if (opArgs != null)
            {
                sb.append("/task/");
                sb.append(opArgs[0]);
            }
            else
            {
                sb.append("/task");
            }
        }
        else if (op.equals("endpoint-list"))
        {
            // v0.9
            // sb.append("/user(");
            // sb.append(username);
            // sb.append(")/endpoint?limit=100");

            // v0.10
            sb.append("/endpoint_list?limit=100");
        }
        else if (op.equals("endpoint-add"))
        {
            if ((opArgs != null) && (opArgs.length > 2))
            {
                sb.append("/endpoint");
            }
            else
            {
                throw new Exception("endpoint-add requires a gridftp and an endpoint-name [-p GRIDFTP:PORT [-m MYPROXY:PORT] endpoint-name]");
            }
        }
        else if (op.equals("endpoint-remove"))
        {
            if (opArgs != null)
            {
                String ep = opArgs[0];
                String newep = ep.replace("#", "%23");
                sb.append("/endpoint/");
                sb.append(newep);
            }
            else
            {
                throw new Exception("endpoint-remove requires an endpoint-name");
            }
        }
        else if (op.equals("activate"))
        {
            if (opArgs != null)
            {
                String ep = opArgs[0];
                String newep = ep.replace("#", "%23");
                sb.append("endpoint/");
                sb.append(newep);
                sb.append("/activation_requirements");
            }
            else
            {
                throw new Exception("Activate requires an endpoint [see Usage]");
            }
        }
        else if (op.equals("transfer"))
        {
            if ((opArgs != null) && (opArgs.length > 1))
            {
                // v0.9
                //sb.append("/transfer/generate_id");

                // v0.10
                sb.append("/transfer/submission_id");
            }
            else
            {
                throw new Exception("transfer requires both a source and destination path");
            }
        }
        else if (op.equals("__internal-endpoint-list"))
        {
            if (opArgs != null)
            {
                String ep = opArgs[0];
                int pos = ep.indexOf("#");
                if (pos == -1)
                {
                    sb.append("/user(");
                    sb.append(username);
                    sb.append(")/endpoint(");
                    sb.append(opArgs[0]);
                    sb.append(")");
                }
                else
                {
                    String user = ep.substring(0, pos);
                    String newep = ep.substring(pos + 1);
                    sb.append("/user(");
                    sb.append(user);
                    sb.append(")/endpoint(");
                    sb.append(newep);
                    sb.append(")");
                }
            }
            else
            {
                throw new Exception("__internal-endpoint- requires an endpoint");
            }
        }
        else
        {
            return null;
        }
        return sb.toString();
    }

    public static String extractFromResults(JSONArray results, String name, String parameter)
    {
        String value = null;
        if (results != null)
        {
            try
            {
                if (name == null)
                {
                    JSONObject jobj = results.getJSONObject(0);
                    if (jobj.get(parameter) != null)
                    {
                        value = jobj.get(parameter).toString();
                    }
                }
                else
                {
                    for(int i = 0; i < results.length(); i++)
                    {
                        JSONObject jobj = results.getJSONObject(i);
                        if (name.equals(jobj.get("name").toString()))
                        {
                            if (jobj.get(parameter) != null)
                            {
                                value = jobj.get(parameter).toString();
                            }
                        }
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        return value;
    }

    // v0.9
    // returns the myProxyServer of the endpoint specified
    // IF the endpoint is a GlobusConnect endpoint, we codify that by pre-pending a "*" to the endpoint
    // public static String fetchMyProxyServerOfEndpoint(String username, String[] opArgs, JGOTransferAPIClient client)
    // {
    //     String myProxyServer = null, path = null;
    //     try
    //     {
    //         path = getPath(username, "__internal-endpoint-list", opArgs);

    //         HttpsURLConnection sConn = client.request("GET", path);
    //         JSONArray results = client.getResult(sConn);

    //         myProxyServer = extractFromResults(results, "myproxy_server");
    //         String isGlobusConnect = extractFromResults(results, "is_globus_connect");
    //         if ((isGlobusConnect != null) && (isGlobusConnect.equals("true")))
    //         {
    //             myProxyServer = "*" + myProxyServer;
    //         }
    //     }
    //     catch(Exception e)
    //     {
    //         e.printStackTrace();
    //     }
    //     return myProxyServer;
    // }

    // v0.10
    public static String fetchMyProxyServerOfEndpoint(JSONArray results)
    {
        String myProxyServer = null;
        try
        {
            JSONObject jobj = results.getJSONObject(0);
            if (jobj.get("DATA") != null)
            {
                JSONArray dataArr = jobj.getJSONArray("DATA");
                myProxyServer = extractFromResults(dataArr, "hostname", "value");
            }
            // if (jobj.get("DATA") != null)
            // {
            //     JSONArray dataArr = jobj.getJSONArray("DATA");
            //     myProxyServer = extractFromResults(dataArr, "value");
            // }
        }
        catch(Exception e)
        {
        }
        return myProxyServer;
    }

    public static TaskSummaryResult processTaskSummary(JSONArray results, Options opts)
    {
        TaskSummaryResult ret = null;
        JSONObject jobj = null;
        try
        {
            jobj = results.getJSONObject(0);
            ret = new TaskSummaryResult();
            ret.createFromJSON(jobj);
            if (opts.verbose)
            {
                System.out.println(ret);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return ret;
    }

    // can either return a TaskResult or TaskListResult
    public static JGOResult processTask(JSONArray results, Options opts)
    {
        JGOResult ret = null;
        TaskResult tr = null;
        JSONArray dataArr = null, linkArr = null;
        JSONObject jobj = null, data = null, link = null;
        int count = 0, total = 0, limit = 0, length = 0;
        try
        {
            for(int i = 0; i < results.length(); i++)
            {
                jobj = results.getJSONObject(i);
                if (opts.opArgs == null)
                {
                    total = jobj.getInt("total");
                    limit = jobj.getInt("limit");
                    length = jobj.getInt("length");

                    System.out.println("Task Total       : " + total);
                    System.out.println("Task Limit       : " + limit);

                    dataArr = jobj.getJSONArray("DATA");
                    for(int j = 0; j < dataArr.length(); j++)
                    {
                        data = dataArr.getJSONObject(j);

                        ++count;
                        tr = new TaskResult();
                        tr.createFromJSON(data, count, limit, total);
                        if (ret == null)
                        {
                            ret = new TaskListResult();
                        }
                        ((TaskListResult)ret).add(tr);
                    }
                }
                else
                {
                    ret = new TaskResult();
                    ((TaskResult)ret).createFromJSON(jobj, 1, 1, 1);
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return ret;
    }

    public static EndpointListResult processEndpointList(JSONArray results, Options opts)
    {
        EndpointListResult ret = null;
        JSONObject endpoints = null;
        try
        {
            boolean list_public = opArgHasValue(opts.opArgs, "-p");
            boolean list_verbose = opArgHasValue(opts.opArgs, "-v");

            for(int i = 0; i < results.length(); i++)
            {
                endpoints = results.getJSONObject(i);

                ret = new EndpointListResult();
                ret.createFromJSON(endpoints, opts.username, list_public, list_verbose);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return ret;
    }

    public static EndpointAddResult processEndpointAdd(Options opts, JGOTransferAPIClient client)
    {
        EndpointAddResult ret = null;
        JSONObject endpoints = null;
        try
        {
            String gridFTPServer = opArgGetValue(opts.opArgs, "-p");
            String myProxyServer = opArgGetValue(opts.opArgs, "-m");
            String serverDN = opArgGetValue(opts.opArgs, "-s");
            boolean isGlobusConnect = opArgHasValue(opts.opArgs, "--gc");
            boolean isPublic = opArgHasValue(opts.opArgs, "-P");
            String endpointName = opts.opArgs[opts.opArgs.length-1];

            String path = getPath(opts.username, opts.operation, opts.opArgs);
            ret = new EndpointAddResult(path);
            ret.addEndpoint(opts.username, gridFTPServer, myProxyServer,
                            serverDN, isGlobusConnect, isPublic, endpointName, client);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return ret;
    }

    public static EndpointRemoveResult processEndpointRemove(Options opts, JGOTransferAPIClient client)
    {
        EndpointRemoveResult ret = null;
        JSONObject endpoints = null;
        try
        {
            String path = getPath(opts.username, opts.operation, opts.opArgs);

            ret = new EndpointRemoveResult(path);
            String endpointName = opts.opArgs[opts.opArgs.length-1];
            ret.removeEndpoint(opts.username, endpointName, client);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return ret;
    }

    public static ActivationRequirementResult processActivationRequirements(
        JSONArray results, Options opts, JGOTransferAPIClient client)
        throws Exception
    {
        ActivationRequirementResult ret = null;
        boolean activated = false;
        boolean globusConnect = false;

        // v0.9
        // we have the endpoint so we need to pull the myproxy server for that endpoint
        //String myProxyServer = fetchMyProxyServerOfEndpoint(opts.username, opts.opArgs, client);

        // v0.10
        String myProxyServer = fetchMyProxyServerOfEndpoint(results);
        if ((myProxyServer == null) || (myProxyServer.equals("null")))
        {
            throw new FileNotFoundException("Error: No default myproxy server for '" + opts.opArgs[0] + "'");
        }

        if (myProxyServer.charAt(0) == '*')
        {
            myProxyServer = myProxyServer.substring(1);
            globusConnect = true;
        }

        if (opts.verbose)
        {
            System.out.println("Retrieved MyProxy Server: " + myProxyServer);
        }

        if ((opts.opArgs == null) || (opts.opArgs[0] == null))
        {
            throw new Exception("Activation requires an endpoint [see Usage]");
        }

        ret = new ActivationRequirementResult();
        ret.createFromJSONArray(results);
        if (!ret.activated.equals("true"))
        {
            String myProxyUser = opArgGetValue(opts.opArgs, "-U");

            // if it's a globusConnect endpoint, OR no username was provided, attempt to auto-activate it
            if (ret.auto_activation_supported.equals("true") &&
                ((globusConnect == true) || (myProxyUser == null)))
            {
                activated = ret.autoActivate(myProxyServer, opts.opArgs[0], client);
            }
            else
            {
                String myProxyPassword = opArgGetValue(opts.opArgs, "-P");
                if (myProxyPassword == null)
                {
                    Console c = System.console();
                    myProxyPassword = new String(c.readPassword("Enter MyProxy pass phrase: "));
                }
                String lifetimeInHours = opArgGetValue(opts.opArgs, "-l");
                activated = ret.activate(myProxyServer, opts.opArgs[0], myProxyUser, myProxyPassword, lifetimeInHours, client);
            }
        }
        return ret;
    }

    public static TransferResult processTransfer(JSONArray results, Options opts, JGOTransferAPIClient client)
        throws Exception
    {
        TransferResult ret = null;
        JSONArray dataArr = null;
        JSONObject jobj = null, data = null;
        try
        {
            ret = new TransferResult(results);

            String timeout = opArgGetValue(opts.opArgs, "-d");
            String sourcePath = null, destPath = null;
            for(int i = 0; i < opts.opArgs.length; i+=2)
            {
                sourcePath = opts.opArgs[i];
                destPath = opts.opArgs[i+1];

                if (opts.verbose)
                {
                    System.out.println("Adding source path: " + sourcePath);
                    System.out.println("Adding dest path: " + destPath);
                }
                ret.addSourceDestPathPair(opts.username, sourcePath, destPath);
            }

            ret.postTransfer(client, timeout);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return ret;
    }

    public static JGOResult processResult(JSONArray results, Options opts, JGOTransferAPIClient client)
        throws Exception
    {
        JGOResult ret = null;
        if (opts.operation.equals("tasksummary"))
        {
            ret = processTaskSummary(results, opts);
        }
        else if (opts.operation.equals("task"))
        {
            ret = processTask(results, opts);
        }
        else if (opts.operation.equals("endpoint-list"))
        {
            ret = processEndpointList(results, opts);
        }
        else if (opts.operation.equals("activate"))
        {
            ret = processActivationRequirements(results, opts, client);
        }
        else if (opts.operation.equals("transfer"))
        {
            ret = processTransfer(results, opts, client);
        }
        return ret;
    }
}