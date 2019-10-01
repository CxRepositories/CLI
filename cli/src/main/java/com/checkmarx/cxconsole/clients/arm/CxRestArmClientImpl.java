package com.checkmarx.cxconsole.clients.arm;

import com.checkmarx.cxconsole.clients.arm.dto.Policy;
import com.checkmarx.cxconsole.clients.arm.exceptions.CxRestARMClientException;
import com.checkmarx.cxconsole.clients.arm.utils.ArmResourceUriBuilder;
import com.checkmarx.cxconsole.clients.exception.CxValidateResponseException;
import com.checkmarx.cxconsole.clients.login.CxRestLoginClient;
import com.checkmarx.cxconsole.clients.utils.RestClientUtils;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static com.checkmarx.cxconsole.clients.utils.RestClientUtils.parseJsonListFromResponse;

/**
 * Created by eyala on 7/9/2018.
 */
public class CxRestArmClientImpl implements CxRestArmClient {

    private static Logger log = Logger.getLogger(CxRestArmClientImpl.class);

    private HttpClient apacheClient;
    private String hostName;
    private static final Header CLI_CONTENT_TYPE_AND_VERSION_HEADER = new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType() + ";v=1.0");
    private static final Header CLI_ACCEPT_HEADER_AND_VERSION_HEADER = new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType() + ";v=1.0");

    private static Header authHeader;

    public CxRestArmClientImpl(CxRestLoginClient restClient, String hostName) {
        authHeader = restClient.getAuthHeader();
        this.apacheClient = restClient.getClient();
        this.hostName = hostName;
    }

    @Override
    public List<Policy> getProjectViolations(int projectId, String provider) throws CxRestARMClientException {
        HttpUriRequest getRequest;
        HttpResponse response = null;

        try {
            getRequest = RequestBuilder.get()
                    .setUri(String.valueOf(ArmResourceUriBuilder.buildGetViolationsURL(new URL(hostName), projectId, provider)))
                    .setHeader(authHeader)
                    .setHeader(CLI_ACCEPT_HEADER_AND_VERSION_HEADER)
                    .setHeader(CLI_CONTENT_TYPE_AND_VERSION_HEADER)
                    .build();
            response = apacheClient.execute(getRequest);
            RestClientUtils.validateClientResponse(response, 200, "fail to get CXArm violations");
            return parseJsonListFromResponse(response, TypeFactory.defaultInstance().constructCollectionType(List.class, Policy.class));
        } catch (IOException | CxValidateResponseException e) {
            log.error("Failed to get CXArm violations: " + e.getMessage());
            throw new CxRestARMClientException("Failed to get CXArm violations:  " + e.getMessage());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    @Override
    public void close() {
        HttpClientUtils.closeQuietly(apacheClient);
    }

}
