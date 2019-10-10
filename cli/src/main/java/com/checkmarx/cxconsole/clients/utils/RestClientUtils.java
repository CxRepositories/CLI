package com.checkmarx.cxconsole.clients.utils;

import com.checkmarx.cxconsole.clients.exception.CxRestClientException;
import com.checkmarx.cxconsole.clients.exception.CxValidateResponseException;
import com.checkmarx.cxconsole.clients.sast.dto.ScanSettingDTO;
import com.checkmarx.cxconsole.clients.sast.dto.ScanSettingDTODeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by nirli on 20/02/2018.
 */
public class RestClientUtils {
    private static Logger log = Logger.getLogger(RestClientUtils.class);

    private static String HTTP_HOST = System.getProperty("http.proxyHost");
    private static String HTTP_PORT = System.getProperty("http.proxyPort");

    private static String HTTPS_HOST = System.getProperty("https.proxyHost");
    private static String HTTPS_PORT = System.getProperty("https.proxyPort");

    private RestClientUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static JSONObject parseJsonObjectFromResponse(HttpResponse response) throws IOException {
        String responseInString = createStringFromResponse(response).toString();
        return new JSONObject(responseInString);
    }

    public static <ResponseObj> ResponseObj parseJsonFromResponse(HttpResponse response, Class<ResponseObj> dtoClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(createStringFromResponse(response).toString(), dtoClass);
    }

    public static <ResponseObj> List<ResponseObj> parseJsonListFromResponse(HttpResponse response, CollectionType dtoClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(createStringFromResponse(response).toString(), dtoClass);
    }

    private static StringBuilder createStringFromResponse(HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        return result;
    }

    public static ScanSettingDTO parseScanSettingResponse(HttpResponse response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ScanSettingDTO.class, new ScanSettingDTODeserializer());
        mapper.registerModule(module);

        return mapper.readValue(createStringFromResponse(response).toString(), ScanSettingDTO.class);
    }


    public static void validateClientResponse(HttpResponse response, int status, String message) throws CxValidateResponseException {
        try {
            if (response.getStatusLine().getStatusCode() != status) {
                String responseBody = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                if (responseBody.contains("<!DOCTYPE html PUBLIC \"")) {
                    responseBody = "No error message";
                }
                throw new CxValidateResponseException(message + ": " + "status code: " + response.getStatusLine().getStatusCode() + ". Error message:" + responseBody);
            }
        } catch (IOException e) {
            throw new CxValidateResponseException("Error parse REST response body: " + e.getMessage());
        }
    }

    public static void validateTokenResponse(HttpResponse response, int status, String message) throws CxValidateResponseException {
        try {
            if (response.getStatusLine().getStatusCode() != status) {
                String responseBody = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                responseBody = responseBody.replace("{", "").replace("}", "").replace(System.getProperty("line.separator"), " ").replace("  ", "");
                if (responseBody.contains("<!DOCTYPE html>")) {
                    throw new CxValidateResponseException(message + ": " + "status code: 500. Error message: Internal Server Error");
                } else if (responseBody.contains("\"error\":\"invalid_grant\"")) {
                    throw new CxValidateResponseException(message);
                } else {
                    throw new CxValidateResponseException(message + ": " + "status code: " + response.getStatusLine() + ". Error message:" + responseBody);
                }
            }
        } catch (IOException e) {
            throw new CxValidateResponseException("Error parse REST response body: " + e.getMessage());
        }
    }

    public static void setClientProxy(HttpClientBuilder clientBuilder, String proxyHost, int proxyPort) {
        log.debug(String.format("Setting proxy to %s:%s", proxyHost, proxyPort));
        HttpHost proxyObject = new HttpHost(proxyHost, proxyPort);
        clientBuilder
                .setProxy(proxyObject)
                .setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
    }

    public static SSLConnectionSocketFactory getSSLSF() throws CxRestClientException {
        TrustStrategy acceptingTrustStrategy = new TrustAllStrategy();
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new CxRestClientException("Fail to set trust all certificate, 'SSLConnectionSocketFactory'", e);
        }
        return new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
    }

}