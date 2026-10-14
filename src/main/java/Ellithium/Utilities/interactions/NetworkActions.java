package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.NetworkInterceptor;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.Filter;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.Route;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern network interception and mocking utilities powered by Selenium 4 NetworkInterceptor.
 * Allows UI tests to stub REST API responses, mock network errors (500, 404, 403),
 * or inspect network traffic directly without external proxy dependencies.
 *
 * @param <T> WebDriver type
 */
public class NetworkActions<T extends WebDriver> extends BaseActions<T> implements AutoCloseable {

    private final List<NetworkInterceptor> activeInterceptors = new ArrayList<>();

    public NetworkActions(T driver) {
        super(driver);
    }

    /**
     * Mocks HTTP responses matching a specific URL substring or pattern.
     *
     * @param urlPattern URL substring to intercept
     * @param statusCode HTTP status code to return (e.g. 200, 404, 500)
     * @param contentType MIME type (e.g. "application/json")
     * @param responseBody Raw body string to return
     * @return this instance for fluent chaining
     */
    public NetworkActions<T> mockResponse(String urlPattern, int statusCode, String contentType, String responseBody) {
        Route route = Route.matching(req -> req.getUri() != null && req.getUri().contains(urlPattern))
                .to(() -> req -> new HttpResponse()
                        .setStatus(statusCode)
                        .addHeader("Content-Type", contentType)
                        .setContent(Contents.utf8String(responseBody != null ? responseBody : "")));

        NetworkInterceptor interceptor = new NetworkInterceptor(driver, route);
        activeInterceptors.add(interceptor);
        Reporter.log("Mocked network response for [" + urlPattern + "] -> HTTP " + statusCode, LogLevel.INFO_BLUE);
        return this;
    }

    /**
     * Mocks a JSON response matching a specific URL pattern with HTTP 200 OK.
     *
     * @param urlPattern URL substring to intercept
     * @param jsonBody JSON string to return
     * @return this instance for fluent chaining
     */
    public NetworkActions<T> mockJsonResponse(String urlPattern, String jsonBody) {
        return mockResponse(urlPattern, 200, "application/json; charset=utf-8", jsonBody);
    }

    /**
     * Mocks a JSON response matching a specific URL pattern with a custom status code.
     *
     * @param urlPattern URL substring to intercept
     * @param statusCode HTTP status code (e.g. 400, 401, 500)
     * @param jsonBody JSON string to return
     * @return this instance for fluent chaining
     */
    public NetworkActions<T> mockJsonResponse(String urlPattern, int statusCode, String jsonBody) {
        return mockResponse(urlPattern, statusCode, "application/json; charset=utf-8", jsonBody);
    }

    /**
     * Applies a custom Selenium Filter for advanced request/response manipulation.
     *
     * @param filter Selenium HTTP filter
     * @return this instance for fluent chaining
     */
    public NetworkActions<T> intercept(Filter filter) {
        NetworkInterceptor interceptor = new NetworkInterceptor(driver, filter);
        activeInterceptors.add(interceptor);
        Reporter.log("Applied custom network interceptor filter", LogLevel.INFO_BLUE);
        return this;
    }

    private final List<org.openqa.selenium.bidi.module.Network> activeBidiNetworks = new ArrayList<>();

    /**
     * Configures automatic HTTP Basic / Digest authentication for all requests requiring credentials.
     * Powered by WebDriver BiDi with seamless fallback to HasAuthentication.
     *
     * @param username The authentication username
     * @param password The authentication password
     * @return this instance for fluent chaining
     */
    public NetworkActions<T> authenticate(String username, String password) {
        return authenticate(uri -> true, username, password);
    }

    /**
     * Configures automatic HTTP Basic / Digest authentication for requests matching the given URI filter.
     * Powered by WebDriver BiDi with seamless fallback to HasAuthentication.
     *
     * @param urlFilter Predicate to match target URIs requiring authentication
     * @param username The authentication username
     * @param password The authentication password
     * @return this instance for fluent chaining
     */
    public NetworkActions<T> authenticate(java.util.function.Predicate<java.net.URI> urlFilter, String username, String password) {
        if (driver instanceof org.openqa.selenium.bidi.HasBiDi hasBiDi) {
            try {
                if (hasBiDi.getBiDi() != null) {
                    org.openqa.selenium.bidi.module.Network network = new org.openqa.selenium.bidi.module.Network(driver);
                    activeBidiNetworks.add(network);
                    network.onAuthRequired(response -> {
                        try {
                            String reqUrl = response.getRequest() != null ? response.getRequest().getUrl() : "";
                            String reqId = response.getRequest() != null ? response.getRequest().getRequestId() : null;
                            if (reqId == null) return;
                            java.net.URI uri = java.net.URI.create(reqUrl);
                            if (urlFilter == null || urlFilter.test(uri)) {
                                network.continueWithAuth(reqId, new org.openqa.selenium.UsernameAndPassword(username, password));
                                Reporter.log("BiDi: Authenticated request to [" + reqUrl + "]", LogLevel.INFO_GREEN);
                            } else {
                                network.continueWithAuthNoCredentials(reqId);
                            }
                        } catch (Exception e) {
                            if (response.getRequest() != null && response.getRequest().getRequestId() != null) {
                                network.continueWithAuth(response.getRequest().getRequestId(), new org.openqa.selenium.UsernameAndPassword(username, password));
                            }
                        }
                    });
                    Reporter.log("Registered BiDi HTTP authentication handler for user: " + username, LogLevel.INFO_BLUE);
                    return this;
                }
            } catch (Exception e) {
                Reporter.log("BiDi authentication setup deferred to HasAuthentication: " + e.getMessage(), LogLevel.WARN);
            }
        }
        if (driver instanceof org.openqa.selenium.HasAuthentication hasAuth) {
            hasAuth.register(urlFilter != null ? urlFilter : uri -> true, () -> new org.openqa.selenium.UsernameAndPassword(username, password));
            Reporter.log("Registered HasAuthentication handler for user: " + username, LogLevel.INFO_BLUE);
        } else {
            Reporter.log("Driver does not support BiDi or HasAuthentication", LogLevel.WARN);
        }
        return this;
    }

    /**
     * Closes and clears all active network interceptors and BiDi network handlers on this driver instance.
     */
    @Override
    public void close() {
        for (NetworkInterceptor interceptor : activeInterceptors) {
            try {
                interceptor.close();
            } catch (Exception ignored) {}
        }
        activeInterceptors.clear();
        for (org.openqa.selenium.bidi.module.Network network : activeBidiNetworks) {
            try {
                network.close();
            } catch (Exception ignored) {}
        }
        activeBidiNetworks.clear();
        Reporter.log("Cleared active network interceptors", LogLevel.DEBUG);
    }

    /**
     * Alias for {@link #close()}.
     */
    public void reset() {
        close();
    }
}
