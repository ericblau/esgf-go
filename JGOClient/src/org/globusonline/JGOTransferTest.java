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

public class JGOTransferTest
{
    // TODO: Update these to make sense for your environment
    private static String GO_USERNAME = "neillm78";

    private static String CERTIFICATE_FILE = "/tmp/x509up_u1000";
    private static String KEY_FILE = "/tmp/x509up_u1000";
    private static String CA_CERTIFICATE_FILE = "/home/neillm/.globus/certificates/gd_bundle.crt";

    private static String GRIDFTP_SERVER = "gridftpserver.com:2811";

    private static String MYPROXY_SERVER = "myproxyserver.com:7512";
    private static String MYPROXY_USERNAME = "testUser";
    private static String MYPROXY_PASSWORD = "testPass";

    private static String GO_ENDPOINT1_NAME = "testEP1";
    private static String GO_ENDPOINT2_NAME = "testEp2";
    private static String GO_ENDPOINT3_NAME = "testEp3";
    private static String GO_EP1_NAME       = "go#ep1";

    private static String AUTHTOKEN         = "AUTH TOKEN HERE";

    public static void print(String msg)
    {
        System.out.println("*** " + msg + " ***");
    }

    public static void main(String[] args)
    {
        try
        {
            //JGOTransfer transfer = new JGOTransfer(GO_USERNAME, CERTIFICATE_FILE, KEY_FILE, CA_CERTIFICATE_FILE);

            // NOTE: First get a credential, but then replace the object with a new one using the authToken
            // A later test tries to use the credential
            JGOTransfer transfer = new JGOTransfer(GO_USERNAME, MYPROXY_SERVER, MYPROXY_USERNAME, MYPROXY_PASSWORD, CA_CERTIFICATE_FILE);
            //JGOTransfer transfer = new JGOTransfer(GO_USERNAME, AUTHTOKEN, CA_CERTIFICATE_FILE);

            // setup transfer parameters here (optional)
            transfer.setVerbose(true);
            //transfer.setTmpFileDirectory("/home/neillm");
            transfer.setBaseUrl("https://transfer.test.api.globusonline.org/v0.10");

            // initialize transfer (required)
            transfer.initialize();

            // set authToken later to make sure the initialize method pulls down a credential
            transfer.setAuthToken(AUTHTOKEN);

            print("Listing endpoints");
            Vector<EndpointInfo> endpoints = transfer.listEndpoints();

            System.out.print("Retrieved information about " + endpoints.size() + " endpoints: [");
            for(EndpointInfo endpoint : endpoints)
            {
                System.out.print(endpoint.getEPName() + " ");
            }
            System.out.println("]");

            // proceed to use GO operations on a 'normal' test endpoint
            boolean isGlobusConnectEP = false;
            print("Adding endpoint " + GO_ENDPOINT1_NAME);
            transfer.addEndpoint(GO_ENDPOINT1_NAME, GRIDFTP_SERVER, MYPROXY_SERVER, isGlobusConnectEP);

            print("Attempting to auto-activate " + GO_EP1_NAME);
            transfer.activateEndpoint(GO_EP1_NAME);

            print("Attempting to activate " + GO_ENDPOINT1_NAME + " as " + MYPROXY_USERNAME);
            transfer.activateEndpoint(GO_ENDPOINT1_NAME, MYPROXY_USERNAME, MYPROXY_PASSWORD);

            print("Adding endpoint " + GO_ENDPOINT3_NAME);
            transfer.addEndpoint(GO_ENDPOINT3_NAME, GRIDFTP_SERVER, MYPROXY_SERVER, isGlobusConnectEP);

            print("Attempting to activate via delegation " + GO_ENDPOINT3_NAME);
            transfer.activateEndpoint(GO_ENDPOINT3_NAME, transfer.getUserCertificateFile());

            // build source file list
            Vector<String> fileList = new Vector<String>();
            fileList.add(".bashrc");
            fileList.add("/etc/group");
            fileList.add("/data/mydata/project/data_dir/");
            fileList.add("/data/mydata/project/foo.nc");
            fileList.add("/data/mydata/project/bar.nc");

            // start the transfer
            print("Starting Transfer from " + GO_EP1_NAME + " to " + GO_ENDPOINT1_NAME);
            String taskID = transfer.transfer(GO_EP1_NAME, GO_ENDPOINT1_NAME, fileList, "/tmp");

            print("Checking status of Task ID: " + taskID);
            boolean isRunning = transfer.isTaskRunning(taskID);
            print("Transfer is Running? " + isRunning);

            // if we don't sleep here, the EP is removed and the transfer won't complete
            // e-mailed error is:
            // Your Globus Online transfer task is now inactive because these endpoint(s) need credential reactivation: esg#testEp1 (Deleted 2011-08-17 18:08:31).
            // that's ok for this test, we don't require proper completion.
            //Thread.sleep(30000);

            print("Removing endpoint " + GO_ENDPOINT1_NAME);
            transfer.removeEndpoint(GO_ENDPOINT1_NAME);

            // do the same operations on a GlobusConnect test endpoint
            isGlobusConnectEP = true;

            print("Adding endpoint " + GO_ENDPOINT2_NAME);
            transfer.addEndpoint(GO_ENDPOINT2_NAME, GRIDFTP_SERVER, isGlobusConnectEP);

            print("Attempting to auto-activate " + GO_ENDPOINT2_NAME);
            transfer.activateEndpoint(GO_ENDPOINT2_NAME);

            print("Removing endpoint " + GO_ENDPOINT2_NAME);
            transfer.removeEndpoint(GO_ENDPOINT2_NAME);
        }
        catch(Exception e)
        {
            System.err.println("Unexpected failure: " + e);
        }
    }
}