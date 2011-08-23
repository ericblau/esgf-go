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

// A convenience class that encapsulates the most important aspects of Endpoints
public class EndpointInfo
{
    private String epName = null;
    private String hosts = null;
    private String myproxyServer = null;
    private boolean isGlobusConnect = false;

    public EndpointInfo()
    {
    }

    public void setEPName(String epName)
    {
        this.epName = epName;
    }

    public void setHosts(String hosts)
    {
        this.hosts = hosts;
    }

    public void setMyproxyServer(String myproxyServer)
    {
        this.myproxyServer = myproxyServer;
    }

    public void setGlobusConnect(boolean isGlobusConnect)
    {
        this.isGlobusConnect = isGlobusConnect;
    }

    public String getEPName()
    {
        return this.epName;
    }

    public String getHosts()
    {
        return this.hosts;
    }

    public String getMyproxyServer()
    {
        return this.myproxyServer;
    }

    public boolean isGlobusConnect()
    {
        return this.isGlobusConnect;
    }
}
