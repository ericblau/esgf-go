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
import java.util.Vector;

public class Options
{
    public String[] opArgs = null;
    public String operation = null;
    public String username = null;
    public String cafile = null;
    public String certfile = null;
    public String keyfile = null;
    public String baseUrl = null;
    public String authToken = null;
    public boolean verbose = false;

    public Options() { }

    public void parseArguments(String[] args) throws FileNotFoundException, Exception
    {
        // construct defaults
        String home = System.getenv("HOME");
        String fSep = System.getProperty("file.separator");
        if (this.cafile == null)
        {
            this.cafile = home + fSep + ".globus" + fSep + "certificates" + fSep + "gd_bundle.crt";
        }
        if (this.certfile == null)
        {
            this.certfile = home + fSep + ".globus" + fSep + "usercert.pem";
        }
        if (this.keyfile == null)
        {
            this.keyfile = home + fSep + ".globus" + fSep + "userkey.pem";
        }

        if (args.length < 3)
        {
            System.err.println("Usage: java org.globusonline.JGOClient "
                               + "-u username [-ca cafile -cert certfile -key keyfile -base baseurl -verbose] operation [operation args]");
            System.err.println("\tThe \"-u\" and \"operation\" arguments are REQUIRED");
            System.err.println("\tIf \"-authToken\" is not specified, the -cert and -key options are required");
            System.err.println("\tIf \"-ca\" is not specified, " + this.cafile + " will be used");
            System.err.println("\tIf \"-cert\" is not specified, " + this.certfile + " will be used");
            System.err.println("\tIf \"-key\" is not specified, " + this.keyfile + " will be used");
            System.err.println("\tIf \"-base\" is not specified, " + BaseTransferAPIClient.DEFAULT_BASE_URL + " will be used");
            System.err.println("\tSupported operations are: \"tasksummary\", \"endpoint-list [[-v -p]]\", \"activate [endpoint -U username [-P password] [-l MyProxyLifetimeHours]]\", ");
            System.err.println("\t\t\"transfer [source destination [-d timeout]]\", \"task [Task ID]\",");
            System.err.println("\t\t\"endpoint-add [-p GRIDFTP:PORT -m MYPROXY:PORT [-s SERVER_DN] [--gc (for Globus Connect)] [-P (public)] endpoint-name]\",");
            System.err.println("\t\t\"endpoint-remove endpoint-name\"");
            System.err.println("");
            System.exit(1);
        }

        int i = 0, j = 0;
        Vector<String> tmpArgs = new Vector<String>();

        for(i = 0; i < args.length; i++)
        {
            if ((args[i] == null) || (args[i].equals("")))
            {
                continue;
            }
            if (args[i].equals("-u"))
            {
                this.username = args[++i];
            }
            else if (args[i].equals("-verbose"))
            {
                this.verbose = true;
            }
            else if (args[i].equals("-authToken"))
            {
                this.authToken = args[++i];
            }
            else if (args[i].equals("-ca"))
            {
                this.cafile = args[++i];
            }
            else if (args[i].equals("-cert"))
            {
                this.certfile = args[++i];
            }
            else if (args[i].equals("-key"))
            {
                this.keyfile = args[++i];
            }
            else if (args[i].equals("-base"))
            {
                this.baseUrl = args[++i];
            }
            else if (this.operation == null)
            {
                this.operation = args[i];
            }
            else
            {
                if ((args[i] != null) && (!args[i].equals("")))
                {
                    tmpArgs.add(args[i]);
                }
            }
        }

        if (tmpArgs.size() > 0)
        {
            this.opArgs = new String[tmpArgs.size()];
            for(j = 0; j < tmpArgs.size(); j++)
            {
                this.opArgs[j] = tmpArgs.get(j);
            }
        }

        if (this.baseUrl == null)
        {
            this.baseUrl = BaseTransferAPIClient.DEFAULT_BASE_URL;
        }

        if (!new File(this.cafile).exists())
        {
            throw new FileNotFoundException("CA File " + this.cafile + " does not exist.  Use \"-ca\" option to specify a valid file");
        }
        if (this.authToken == null)
        {
            if (!new File(this.certfile).exists())
            {
                throw new FileNotFoundException("CA File " + this.certfile + " does not exist.  Use \"-cert\" option to specify a valid file");
            }
            if (!new File(this.keyfile).exists())
            {
                throw new FileNotFoundException("CA File " + this.keyfile + " does not exist.  Use \"-key\" option to specify a valid file");
            }
        }
    }

    public void print()
    {
        System.out.println("Verbose  : " + verbose);
        System.out.println("Username : " + username);
        System.out.println("CA File  : " + cafile);
        System.out.println("Cert File: " + certfile);
        System.out.println("Key File : " + keyfile);
        System.out.println("Base URL : " + baseUrl);
        System.out.println("Operation: " + operation);

        if (opArgs != null)
        {
            for(int i = 0; i < opArgs.length; i++)
            {
                System.out.println("Arguments: " + opArgs[i]);
            }
        }
    }
}
