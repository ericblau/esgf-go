package org.globusonline;

import javax.net.ssl.HttpsURLConnection;

public interface Authenticator {
	/**
	 * @param c The connection that needs to be authenticated
	 */
	public void authenticateConnection(HttpsURLConnection c);
}
