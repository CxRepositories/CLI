package com.checkmarx.cxconsole.clients.login;

import com.checkmarx.cxconsole.clients.login.exceptions.CxRestLoginClientException;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;

/**
 * Created by nirli on 14/03/2018.
 */
public interface CxRestLoginClient {

    void credentialsLogin(boolean isCheckedPolicy) throws CxRestLoginClientException;

    void tokenLogin() throws CxRestLoginClientException;

    void ssoLogin() throws CxRestLoginClientException;

    HttpClient getClient();

    String getHostName();

    boolean isLoggedIn();

    boolean isCredentialsLogin();

    boolean isTokenLogin();

    Header getAuthHeader();

}
