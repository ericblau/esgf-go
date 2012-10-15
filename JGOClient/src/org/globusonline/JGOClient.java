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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;

import javax.net.ssl.HttpsURLConnection;

public class JGOClient
{
    public static void runJGOCommand(String[] args)
    {
        Options opts = new Options();
        try
        {
            opts.parseArguments(args);
            JGOResult ret = execJGOCommand(args);
            if (ret != null)
            {
                System.out.println(ret);
            }
        }
        catch (FileNotFoundException fnfe)
        {
            if (opts.operation.equals("activate"))
            {
                System.err.println("Error: No such endpoint '" + opts.opArgs[0] + "'");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static JGOResult execJGOCommand(String[] args) throws Exception
    {
        JGOResult ret = null;
        Options opts = new Options();

        opts.parseArguments(args);

        if (opts.verbose) opts.print();

        JGOTransferAPIClient client = new JGOTransferAPIClient(opts);
        client.setBaseUrl(opts.baseUrl);
        client.setVerbose(opts.verbose);
        HttpsURLConnection sConn = null;
        JSONArray results = null;
        String path = client.getPath();
        if (path != null)
        {
            // special case any operations that require anything other
            // than a GET (e.g. POST/PUT/DELETE) up front
            if (opts.operation.equals("endpoint-add"))
            {
                ret = JGOUtils.processEndpointAdd(opts, client);
            }
            else if (opts.operation.equals("endpoint-remove"))
            {
                ret = JGOUtils.processEndpointRemove(opts, client);
            }
            else
            {
                // Any GET method can follow this path, with a post processing of the results
                sConn = client.request("GET", path);
                results = client.getResult(sConn);

                if (opts.verbose)
                {
                    System.out.println(results);
                    System.out.println("");
                }
                ret = JGOUtils.processResult(results, opts, client);
                sConn.disconnect();
            }
        }
        else
        {
            System.err.println("Operation not supported: " + opts.operation);
        }
        return ret;
    }

    public static void main(String[] args)
    {
        JGOClient.runJGOCommand(args);
    }
}