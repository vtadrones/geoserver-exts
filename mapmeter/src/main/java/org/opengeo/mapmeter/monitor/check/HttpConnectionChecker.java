package org.opengeo.mapmeter.monitor.check;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.geotools.util.logging.Logging;
import org.opengeo.mapmeter.monitor.config.MessageTransportConfig;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;

public class HttpConnectionChecker implements ConnectionChecker {

    private static final Logger LOGGER = Logging.getLogger(HttpConnectionChecker.class);

    private final MessageTransportConfig config;

    private final HttpConnectionManager connectionManager;

    private final HttpClient client;

    public HttpConnectionChecker(MessageTransportConfig config, int connectionTimeout) {
        this.config = config;
        connectionManager = new SimpleHttpConnectionManager();

        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setSoTimeout(connectionTimeout);
        clientParams.setConnectionManagerTimeout(connectionTimeout);
        client = new HttpClient(clientParams, connectionManager);
    }

    @Override
    public ConnectionResult checkConnection(String apiKey) {
        String checkUrl;
        synchronized (config) {
            checkUrl = config.getCheckUrl();
        }

        GetMethod getMethod = new GetMethod(checkUrl);
        getMethod.setQueryString(new NameValuePair[] { new NameValuePair("apikey", apiKey) });

        Optional<Integer> maybeStatusCode = Optional.absent();
        try {
            maybeStatusCode = Optional.of(client.executeMethod(getMethod));
            int statusCode = maybeStatusCode.get();
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return new ConnectionResult(statusCode, "Not Found");
            } else {
                Header contentTypeHeader = getMethod.getResponseHeader("Content-Type");
                if (isJsonResponse(contentTypeHeader)) {
                    String responseBodyAsString = getResponseBody(getMethod);
                    JSONObject jsonObject = JSONObject.fromObject(responseBodyAsString);
                    return createConnectionResultFromJson(statusCode, jsonObject);
                } else {
                    return createResponseWithError(statusCode, "Unexpected server response");
                }
            }
        } catch (JSONException e) {
            logException(e);
            if (maybeStatusCode.isPresent()) {
                return new ConnectionResult(maybeStatusCode.get(), "Invalid server response");
            } else {
                return new ConnectionResult("Invalid server response");
            }
        } catch (SocketTimeoutException e) {
            logException(e);
            return new ConnectionResult("The request timed out");
        } catch (HttpException e) {
            return logExceptionAndCreateErrorResult(e);
        } catch (IOException e) {
            return logExceptionAndCreateErrorResult(e);
        } finally {
            getMethod.releaseConnection();
            // this connection should close immediately
            connectionManager.closeIdleConnections(0);
        }
    }

    public String getResponseBody(GetMethod getMethod) throws IOException {
        Closer closer = Closer.create();
        try {
            InputStream responseBodyAsStream = closer.register(getMethod.getResponseBodyAsStream());
            InputStreamReader inputStreamReader = closer.register(new InputStreamReader(
                    responseBodyAsStream, Charsets.UTF_8));
            String responseBodyAsString = CharStreams.toString(inputStreamReader);
            return responseBodyAsString;
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    private ConnectionResult createConnectionResultFromJson(int statusCode, JSONObject jsonObject) {
        Object statusObject = jsonObject.get("status");
        if (statusObject == null) {
            return createResponseWithError(statusCode, "Invalid server response: missing status");
        }
        if (!(statusObject instanceof String)) {
            return createResponseWithError(statusCode, "Unexpected server response: status");
        }
        Object errorObject = jsonObject.get("error");
        String error;
        if (errorObject != null && !(errorObject instanceof String)) {
            return createResponseWithError(statusCode, "Unexpected server response: error");
        }
        error = (String) errorObject;

        String status = (String) statusObject;
        if (!"OK".equals(status)) {
            return createResponseWithError(statusCode, error != null ? error
                    : "Unexpected server response: error");
        }

        Object apiKeyStatusObject = jsonObject.get("api_key_status");
        if (apiKeyStatusObject == null) {
            return createResponseWithError(statusCode,
                    "Invalid server response: missing api_key_status");
        }
        if (!(apiKeyStatusObject instanceof String)) {
            return createResponseWithError(statusCode, "Unexpected server response: api_key_status");
        }
        String apiKeyStatus = (String) apiKeyStatusObject;
        if ("VALID".equals(apiKeyStatus)) {
            return new ConnectionResult(HttpStatus.SC_OK);
        } else if ("INVALID".equals(apiKeyStatus)) {
            return createResponseWithError(statusCode, "Invalid API Key");
        } else if ("EXPIRED".equals(apiKeyStatus)) {
            return createResponseWithError(statusCode, "API Key expired");
        } else {
            return createResponseWithError(statusCode, "Unexpected server response: api_key_status");
        }
    }

    private ConnectionResult createResponseWithError(int statusCode, String error) {
        return new ConnectionResult(statusCode, error);
    }

    private boolean isJsonResponse(Header contentTypeHeader) {
        if (contentTypeHeader == null) {
            return false;
        }
        String value = contentTypeHeader.getValue();
        if (value == null) {
            return false;
        }
        value = value.toLowerCase();
        return value.startsWith("application/json");
    }

    public ConnectionResult logExceptionAndCreateErrorResult(Exception e) {
        logException(e);
        return createErrorResult(e);
    }

    private ConnectionResult createErrorResult(Exception e) {
        return new ConnectionResult(e.getLocalizedMessage());
    }

    private void logException(Exception e) {
        LOGGER.severe("Connection check error: " + e.getLocalizedMessage());
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(Throwables.getStackTraceAsString(e));
        }
    }

}
