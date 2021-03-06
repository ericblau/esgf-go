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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.security.Security;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.KeyPair;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;


/**
 * Extension to the base client which supports reading PEM files using
 * Bouncy Castle, so the client cert/key don't have to be converted to
 * PKCS12. Uses JSON primarily, and allows console password retrieval.
 */
public class JGOTransferAPIClient extends BCTransferAPIClient
{
    private String path = null;
    private Options opts = null;

    private static class ConsolePasswordFinder implements PasswordFinder
    {
        private ConsolePasswordFinder() { }

        public char[] getPassword()
        {
            Console c = System.console();
            return c.readPassword("Enter PEM Pass phrase: ");
        }
    }

    public Options getOptions()
    {
        return this.opts;
    }

    /**
     * Create a client for the user.
     *
     * @param username  the Globus Online user to sign in to the API with.
     * @param alt  the content type to request from the server for responses.
     *             Use one of the FORMAT_ constants.
     * @param trustedCAFile path to a PEM file with a list of certificates
     *                      to trust for verifying the server certificate.
     *                      If null, just use the trust store configured by
     *                      property files and properties passed on the
     *                      command line.
     * @param certFile  path to a PEM file containing a client certificate
     *                  to use for authentication. If null, use the key
     *                  store configured by property files and properties
     *                  passed on the command line.
     * @param keyFile  path to a PEM file containing a client key
     *                 to use for authentication. If null, use the key
     *                 store configured by property files and properties
     *                 passed on the command line.
     * @param baseUrl  alternate base URL of the service; can be used to
     *                 connect to different versions of the API and instances
     *                 running on alternate servers. If null, the URL of
     *                 the latest version running on the production server
     *                 is used.
     */
    public JGOTransferAPIClient(Options opts)
        throws KeyManagementException, NoSuchAlgorithmException, Exception
    {
        super(opts.username, FORMAT_JSON, null, null, opts.baseUrl);

        this.opts = opts;

        if (this.opts.authToken != null)
        {
            if (this.opts.verbose)
            {
                System.out.println("Using authToken: " + this.opts.authToken);
            }
            Authenticator authenticator = new GoauthAuthenticator(this.opts.authToken);
            this.setAuthenticator(authenticator);
        }
        else
        {
            Security.addProvider(new BouncyCastleProvider());

            this.trustManagers = this.createTrustManagers(
                this.opts.cafile, this.opts.verbose);

            this.keyManagers = this.createKeyManagers(
                this.opts.certfile, this.opts.keyfile, this.opts.verbose);

            initSocketFactory(true);
        }
    }

    static TrustManager[] createTrustManagers(String trustedCAFile, boolean verbose)
                            throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);

        // Read the cert(s). The file must contain only certs, a cast
        // Exception will be thrown if it contains anything else.
        // TODO: wrap in friendly exception, it's a user error not a
        // programming error if the file contains a non-cert.
        FileReader fileReader = new FileReader(trustedCAFile);
        PEMReader r = new PEMReader(fileReader);
        X509Certificate cert = null;
        try {
            Object o = null;
            int i = 0;
            while ((o = r.readObject()) != null) {
                cert = (X509Certificate) o;

                if (verbose)
                {
                    System.out.println("trusted cert subject: "
                                       + cert.getSubjectX500Principal());
                    System.out.println("trusted cert issuer : "
                                       + cert.getIssuerX500Principal());
                }

                ks.setEntry("server-ca" + i,
                            new KeyStore.TrustedCertificateEntry(cert), null);
                i++;
            }
        } finally {
            r.close();
            fileReader.close();
        }

        // Shove the key store in a TrustManager.
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                                    TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        return tmf.getTrustManagers();
    }

    static KeyManager[] createKeyManagers(String certFile, String keyFile, boolean verbose)
        throws GeneralSecurityException, IOException, JGOTransferException
    {
        // Create a new empty key store.
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);

        // Read the key. Ignore any non-key data in the file, to
        // support PEM files containing both certs and keys.
        FileReader fileReader = new FileReader(keyFile);
        PEMReader r = new PEMReader(fileReader, new ConsolePasswordFinder());
        KeyPair keyPair = null;
        try {
            Object o = null;
            while ((o = r.readObject()) != null) {
                if (o instanceof KeyPair) {
                    keyPair = (KeyPair) o;
                }
            }
        } finally {
            r.close();
            fileReader.close();
        }

        // Read the cert(s). Ignore any non-cert data in the file, to
        // support PEM files containing both certs and keys.
        fileReader = new FileReader(certFile);
        r = new PEMReader(fileReader);
        X509Certificate cert = null;
        ArrayList<Certificate> chain = new ArrayList<Certificate>();
        try {
            Object o = null;
            int i = 0;
            while ((o = r.readObject()) != null) {
                if (!(o instanceof X509Certificate))
                    continue;
                cert = (X509Certificate) o;

                Date expiration = cert.getNotAfter();
                if (verbose)
                {
                    System.out.println("client cert subject: "
                                       + cert.getSubjectX500Principal());
                    System.out.println("client cert issuer : "
                                       + cert.getIssuerX500Principal());
                    System.out.println("client cert not valid after: " + expiration);
                }
                if (expiration.getTime() < System.currentTimeMillis())
                {
                    throw new JGOTransferException("Client certificate is expired.");
                }
                chain.add(cert);
            }
        } finally {
            r.close();
            fileReader.close();
        }

        // The KeyStore requires a password for key entries.
        char[] password = { ' ' };

        // Since we never write out the key store, we don't bother protecting
        // the key.
        ks.setEntry("client-key",
                    new KeyStore.PrivateKeyEntry(keyPair.getPrivate(),
                                         chain.toArray(new Certificate[0])),
                    new KeyStore.PasswordProtection(password));

        // Shove the key store in a KeyManager.
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                                    KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);
        return kmf.getKeyManagers();
    }

    public HttpsURLConnection request(String method, String path, String jsonData)
          throws Exception
    {
        if (! path.startsWith("/")) {
            path = "/" + path;
        }

        initSocketFactory(false);
        URL url = new URL(this.baseUrl + path);
        if (this.opts.verbose)
        {
            System.out.println("[***] Request Path: " + this.baseUrl + path);
        }
        HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
        c.setConnectTimeout(this.timeout);
        c.setSSLSocketFactory(this.socketFactory);
        c.setRequestMethod(method);
        c.setFollowRedirects(false);
        c.setRequestProperty("X-Transfer-API-X509-User", this.username);
        c.setRequestProperty("Accept", this.format);
        c.setUseCaches(false);
        c.setDoInput(true);

        if (this.authenticator != null)
        {
            this.authenticator.authenticateConnection(c);
        }
        if (jsonData != null)
        {
            c.setDoOutput(true); 
            c.setRequestProperty("Content-Type", this.format);
            c.setRequestProperty("Content-Length", "" + Integer.toString(jsonData.getBytes().length));
        }
        c.setRequestProperty("Content-Language", "en-US");
        c.connect();

        if (jsonData != null)
        {
            DataOutputStream wr = new DataOutputStream(c.getOutputStream());
            wr.writeBytes(jsonData);
            wr.flush ();
            wr.close ();
        }

        int statusCode = c.getResponseCode();
        if (statusCode >= 400)
        {
            String statusMessage = c.getResponseMessage();
            String errorHeader = null;
            Map<String, List<String>> headers = c.getHeaderFields();
            if (this.opts.verbose)
            {
                System.out.println("Error Headers Returned: " + headers);
            }
            if (headers.containsKey("X-Transfer-API-Error")) {
                errorHeader = ((List<String>)
                               headers.get("X-Transfer-API-Error")).get(0);
            }
            throw constructAPIError(statusCode, statusMessage, errorHeader,
                                    c.getErrorStream());
        }
        return c;
    }

    public JSONArray getResult(HttpsURLConnection c)
        throws IOException, MalformedURLException, GeneralSecurityException, APIError
    {
        JSONArray jArr = null;

        int statusCode = c.getResponseCode();
        if (statusCode >= 400) {
            String statusMessage = c.getResponseMessage();
            String errorHeader = null;
            Map<String, List<String>> headers = c.getHeaderFields();
            if (headers.containsKey("X-Transfer-API-Error")) {
                errorHeader = ((List<String>)
                               headers.get("X-Transfer-API-Error")).get(0);
            }
            throw constructAPIError(statusCode, statusMessage, errorHeader,
                                    c.getErrorStream());
        }

        InputStream inputStream = c.getInputStream();
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader in = new BufferedReader(reader);

        String inputLine = null;
        StringBuffer strbuf = new StringBuffer("[");

        while ((inputLine = in.readLine()) != null)
        {
            strbuf.append(inputLine);
        }
        strbuf.append("]");
        in.close();

        try
        {
            jArr = new JSONArray(strbuf.toString());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return jArr;
    }

    public String getUsername()
    {
        return this.opts.username;
    }

    public String getOperation()
    {
        return this.opts.operation;
    }

    public String[] getOperationArgs()
    {
        return this.opts.opArgs;
    }

    public String getPath() throws Exception
    {
        if (this.path == null)
        {
            this.path = JGOUtils.getPath(
                this.opts.username, this.opts.operation, this.opts.opArgs);
        }
        return this.path;
    }

    private static String readString(InputStream in) throws IOException {
        Reader reader = null;
        try {
            // TODO: add charset
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    protected APIError constructAPIError(int statusCode, String statusMessage,
                                         String errorCode, InputStream input)
    {
        APIError error = new APIError(statusCode, statusMessage, errorCode);
        try {
            JSONObject errorDocument = new JSONObject(readString(input));
            error.requestId = errorDocument.getString("request_id");
            error.resource = errorDocument.getString("resource");
            error.code = errorDocument.getString("code");
            error.message = errorDocument.getString("message");
        } catch (Exception e) {
            // Make sure the APIError gets thrown, even if we can't parse out
            // the details. If parsing fails, shove the exception in the
            // message fields, so the parsing error is not silently dropped.
            error.message = e.toString();
        }
        return error;
    }
}
