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

import org.globus.myproxy.MyProxy;
import org.globusonline.EndpointListResult;
import org.globusonline.EndpointResult;
import org.globusonline.ActivationRequirementResult;
import org.globusonline.JGOClient;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.ietf.jgss.GSSCredential;

public class JGOTransfer
{
    protected boolean verbose = false;
    protected String goUsername = null;
    protected String certificate = null;
    protected String key = null;
    protected String myproxyServer = null;
    protected String myproxyUsername = null;
    protected String myproxyPassword = null;
    protected String caCertificate = null;
    protected String tmpFileDirectory = null;

    public JGOTransfer(String goUsername, String certificate, String key, String caCertificate)
    {
        this.goUsername = goUsername;
        this.certificate = certificate;
        this.key = key;
        this.caCertificate = caCertificate;
    }

    public JGOTransfer(String goUsername, String myproxyServer, String myproxyUsername, String myproxyPassword, String caCertificate)
    {
        this.goUsername = goUsername;
        this.myproxyServer = myproxyServer;
        this.myproxyUsername = myproxyUsername;
        this.myproxyPassword = myproxyPassword;
        this.caCertificate = caCertificate;
    }

    public void initialize() throws JGOTransferException, Exception
    {
        if ((this.certificate == null) || (this.key == null))
        {
            if ((this.myproxyServer == null) || (this.myproxyUsername == null) || (this.myproxyPassword == null))
            {
                dprint("MyProxy Server = " + this.myproxyServer);
                dprint("MyProxy Username = " + this.myproxyUsername);
                dprint("MyProxy Password = " + ((this.myproxyPassword == null) ? "NULL" : "******"));
                throw new JGOTransferException("All Myproxy information is required (server, username, password)");
            }
            retrieveMyproxyCredential();
        }
        dprint("Initialize complete");
    }

    public void dprint(String msg)
    {
        if (this.verbose)
        {
            System.out.println(msg);
        }
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    public void setTmpFileDirectory(String tmpFileDirectory)
    {
        this.tmpFileDirectory = tmpFileDirectory;
    }

    protected void retrieveMyproxyCredential() throws Exception
    {
        int myproxyPort = 7512;
        String[] pieces = this.myproxyServer.split(":");
        if (pieces.length == 2)
        {
            this.myproxyServer = pieces[0];
            myproxyPort = Integer.parseInt(pieces[1]);
        }
        dprint("Using MyProxy Server " + this.myproxyServer + " and port " + myproxyPort);

        MyProxy myproxy = new MyProxy(this.myproxyServer, myproxyPort);
        GSSCredential credential = myproxy.get(this.myproxyUsername, this.myproxyPassword, (12 * 3600));
        File f = null;
        if (this.tmpFileDirectory != null)
        {
            f = File.createTempFile("x509up_", ".pem", new File(this.tmpFileDirectory));
        }
        else
        {
            f = File.createTempFile("x509up_", ".pem");
        }

        OutputStream os = null;
        try
        {
            os = new FileOutputStream(f.getPath());
            byte [] data = ((ExtendedGSSCredential)credential).export(ExtendedGSSCredential.IMPEXP_OPAQUE);
            os.write(data);

            f.setReadOnly();
            f.deleteOnExit();

            this.certificate = f.getPath();
            this.key = f.getPath();

            dprint("Certificate stored to " + this.certificate);
        }
        catch(Exception e)
        {
        }
        finally
        {
            if (os != null)
            {
                try { os.close(); } catch(Exception e) {}
            }
        }
    }

    public Vector<EndpointInfo> listEndpoints() throws JGOTransferException
    {
        Vector<EndpointInfo> endpoints = null;

        int i = 0;
        String[] args = new String[10];

        args[i++] = "-cert";
        args[i++] = this.certificate;
        args[i++] = "-key";
        args[i++] = this.key;
        args[i++] = "-ca";
        args[i++] = this.caCertificate;
        args[i++] = "-u";
        args[i++] = this.goUsername;
        args[i++] = "endpoint-list";
        args[i++] = "-p";
    
        try
        {
            EndpointListResult endpointList = (EndpointListResult)JGOClient.execJGOCommand(args);

            endpoints = new Vector<EndpointInfo>();

            for(EndpointResult er : endpointList.results)
            {
                EndpointInfo eInfo = new EndpointInfo();
                eInfo.setEPName(er.canonical_name);
                eInfo.setHosts(er.hosts);
                eInfo.setMyproxyServer(er.myproxy_server);
                eInfo.setGlobusConnect(er.is_globus_connect);
                endpoints.add(eInfo);
            }
        }
        catch(Exception e)
        {
            throw new JGOTransferException("Endpoint List failure: " + e.toString());
        }
        return endpoints;
    }

    public void addEndpoint(String logicalEPName, String gsiftpURL, boolean isGlobusConnect)
        throws JGOTransferException
    {
        this.addEndpoint(logicalEPName, gsiftpURL, null, isGlobusConnect);
    }

    public void addEndpoint(String logicalEPName, String gsiftpURL, String myproxyServer, boolean isGlobusConnect)
        throws JGOTransferException
    {
        int i = 0;
        String[] args = null;

        if (isGlobusConnect == true)
        {
            args = new String[16];
        }
        else
        {
            args = new String[15];
        }

        args[i++] = "-ca";
        args[i++] = this.caCertificate;
        args[i++] = "-cert";
        args[i++] = this.certificate;
        args[i++] = "-key";
        args[i++] = this.key;
        args[i++] = "-u";
        args[i++] = this.goUsername;
        args[i++] = "endpoint-add";
        args[i++] = "-p";
        args[i++] = gsiftpURL;
        args[i++] = "-m";
        args[i++] = myproxyServer;
        args[i++] = "-P";
        if (isGlobusConnect == true)
        {
            args[i++] = "--gc";
        }
        args[i++] = logicalEPName;

        try
        {
            EndpointAddResult result = (EndpointAddResult)JGOClient.execJGOCommand(args);
            dprint(result.toString());
        }
        catch(Exception e)
        {
            throw new JGOTransferException("Endpoint Add failure: " + e.toString());
        }
    }

    public void removeEndpoint(String logicalEPName) throws JGOTransferException
    {
        int i = 0;
        String[] args = new String[10];

        args[i++] = "-ca";
        args[i++] = this.caCertificate;
        args[i++] = "-cert";
        args[i++] = this.certificate;
        args[i++] = "-key";
        args[i++] = this.key;
        args[i++] = "-u";
        args[i++] = this.goUsername;
        args[i++] = "endpoint-remove";
        args[i++] = logicalEPName;

        try
        {
            EndpointRemoveResult result = (EndpointRemoveResult)JGOClient.execJGOCommand(args);
            dprint(result.toString());
        }
        catch(Exception e)
        {
            throw new JGOTransferException("Endpoint Remove failure: " + e.toString());
        }
    }

    // useful for Globus Connect activation
    public void activateEndpoint(String logicalEPName) throws JGOTransferException
    {
        this.activateEndpoint(logicalEPName, null, null);
    }

    public void activateEndpoint(String logicalEPName, String myproxyUsername, String myproxyPassword)
        throws JGOTransferException
    {
        int i = 0;
        String[] args = null;
        if ((myproxyUsername == null) && (myproxyPassword == null))
        {
            args = new String[10];
        }
        else
        {
            args = new String[14];
        }
        args[i++] = "-cert";
        args[i++] = this.certificate;
        args[i++] = "-key";
        args[i++] = this.key;
        args[i++] = "-ca";
        args[i++] = this.caCertificate;
        args[i++] = "-u";
        args[i++] = this.goUsername;
        args[i++] = "activate";
        args[i++] = logicalEPName;

        if ((myproxyUsername != null) && (myproxyPassword != null))
        {
            args[i++] = "-U";
            args[i++] = myproxyUsername;
            args[i++] = "-P";
            args[i++] = myproxyPassword;
        }

        try
        {
            ActivationRequirementResult activation = (ActivationRequirementResult)JGOClient.execJGOCommand(args);
            dprint("Activation complete: " + activation);
        }
        catch(Exception e)
        {
            throw new JGOTransferException("Endpoint Activation failure: " + e.toString());
        }
    }

    public void checkTaskStatus(String taskID) throws JGOTransferException
    {
        int i = 0;
        String[] args = new String[10];

        args[i++] = "-ca";
        args[i++] = this.caCertificate;
        args[i++] = "-cert";
        args[i++] = this.certificate;
        args[i++] = "-key";
        args[i++] = this.key;
        args[i++] = "-u";
        args[i++] = this.goUsername;
        args[i++] = "task";
        args[i++] = taskID;

        try
        {
            TaskResult result = (TaskResult)JGOClient.execJGOCommand(args);
            dprint(result.toString());
        }
        catch(Exception e)
        {
            throw new JGOTransferException("Task failure: " + e.toString());
        }
    }

    public String transfer(String sourceEPName, String destEPName, Vector<String> sourceFileList)
        throws JGOTransferException
    {
        return this.transfer(sourceEPName, destEPName, sourceFileList, "");
    }

    public String transfer(String sourceEPName, String destEPName,
                         Vector<String> sourceFileList, String destPathTopLevel)
        throws JGOTransferException
    {
        dprint("transfer called with destPathTopLevel " + destPathTopLevel);
        if (destPathTopLevel == null)
        {
            destPathTopLevel = "";
        }
        else if ((destPathTopLevel.length() > 0) && destPathTopLevel.endsWith("/"))
        {
            destPathTopLevel = destPathTopLevel.substring(0, destPathTopLevel.length() - 1);
        }

        Vector<String> destFileList = new Vector<String>();

        String destFile = null;
        for(String file : sourceFileList)
        {
            if (file.startsWith("/"))
            {
                destFile = destPathTopLevel + file;
            }
            else
            {
                destFile = destPathTopLevel + "/" + file;
            }
            destFileList.add(destFile);
            dprint("MAPPED " + file + " ===> " + destFile);
        }

        return this.transfer(sourceEPName, destEPName, sourceFileList, destFileList);
    }

    // assumes endpoints are already activated
    // returns TaskID on success; null otherwise
    public String transfer(String sourceEPName, String destEPName,
                           Vector<String> sourceFileList, Vector<String> destFileList)
        throws JGOTransferException
    {
        String ret = null;
        if ((sourceFileList == null) || (destFileList == null) ||
            (sourceFileList.size() != destFileList.size()))
        {
            throw new JGOTransferException("Transfer requires valid source and destination file lists of matching sizes");
        }

        int index = 9;
        int totalSlots = (index + (sourceFileList.size() * 2));
        String[] args = new String[totalSlots];
        args[0] = "-cert";
        args[1] = this.certificate;
        args[2] = "-key";
        args[3] = this.key;
        args[4] = "-ca";
        args[5] = this.caCertificate;
        args[6] = "-u";
        args[7] = this.goUsername;
        args[8] = "transfer";

        int len = sourceFileList.size();
        String sourceURL = null, destURL = null;
        String curSource = null, curDest = null;

        for(int i = 0; i < len; i++)
        {
            curSource = sourceFileList.get(i);
            if (curSource.startsWith("/"))
            {
                sourceURL = sourceEPName + curSource;
            }
            else
            {
                // at this point anything not starting with a slash
                // must be a home dir references
                sourceURL = sourceEPName + "/~/" + curSource;
            }
            args[index++] = sourceURL;

            dprint("Source URL[" + i + "] = " + sourceURL);

            curDest = destFileList.get(i);
            if (curDest.startsWith("/"))
            {
                destURL = destEPName + destFileList.get(i);
            }
            else
            {
                destURL = destEPName + "/~/" + destFileList.get(i);
            }
            args[index++] = destURL;

            dprint("Dest   URL[" + i + "] = " + destURL);
        }

        try
        {
            TransferResult transfer = (TransferResult)JGOClient.execJGOCommand(args);
            String output = transfer.toString();
            dprint(output);
            int pos = output.indexOf("Task ID: ");
            if (pos != -1)
            {
                ret = output.substring(pos + 9);
                ret = ret.trim();
                dprint("Returning TaskID " + ret);
            }
        }
        catch(Exception e)
        {
            throw new JGOTransferException("Transfer failure: " + e.toString());
        }
        return ret;
    }
}