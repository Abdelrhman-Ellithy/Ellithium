package Ellithium.core.execution.listener;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

public class APIListener implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        Response response = ctx.next(requestSpec, responseSpec);
        try {
            Reporter.logReportOnly("Request Method: ", LogLevel.INFO_BLUE, requestSpec.getMethod());
            Reporter.logReportOnly("Request URI: ", LogLevel.INFO_BLUE, requestSpec.getURI());
            Reporter.logReportOnly("Request Headers: ", LogLevel.INFO_BLUE,
                    requestSpec.getHeaders() != null ? requestSpec.getHeaders().toString() : "No Headers");
            Reporter.logReportOnly("Request Parameters: ", LogLevel.INFO_BLUE,
                    requestSpec.getQueryParams() != null ? requestSpec.getQueryParams().toString() : "No Query Params");
            Reporter.logReportOnly("Request Base Path: ", LogLevel.INFO_BLUE,
                    requestSpec.getBasePath() != null ? requestSpec.getBasePath() : "No Base Path");
            Reporter.logReportOnly("Request Proxy Specification: ", LogLevel.INFO_BLUE,
                    requestSpec.getProxySpecification() != null ? requestSpec.getProxySpecification().toString() : "No Proxy");
            Reporter.logReportOnly("Request ContentType: ", LogLevel.INFO_BLUE,
                    requestSpec.getContentType() != null ? requestSpec.getContentType() : "No Content Type");
            Reporter.logReportOnly("Request Cookies: ", LogLevel.INFO_BLUE,
                    requestSpec.getCookies() != null ? requestSpec.getCookies().toString() : "No Cookies");
            Reporter.logReportOnly("Request Params: ", LogLevel.INFO_BLUE,
                    requestSpec.getRequestParams() != null ? requestSpec.getRequestParams().toString() : "No Request Params");
            Reporter.logReportOnly("Request Body: ", LogLevel.INFO_BLUE,
                    requestSpec.getBody() != null ? requestSpec.getBody().toString() : "No Body");
            Reporter.logReportOnly("Response Status Code: ", LogLevel.INFO_BLUE,
                    String.valueOf(response.getStatusCode()));
            Reporter.logReportOnly("Response ContentType: ", LogLevel.INFO_BLUE,
                    response.getContentType() != null ? response.getContentType() : "No Content Type");
            Reporter.logReportOnly("Response Status Line: ", LogLevel.INFO_BLUE, response.getStatusLine());
            Reporter.logReportOnly("Detailed Response Cookies: ", LogLevel.INFO_BLUE, response.detailedCookies().toString());
            Reporter.logReportOnly("Response Content-Length: ", LogLevel.INFO_BLUE, response.getHeader("Content-Length"));
            Reporter.logReportOnly("Response Body: ", LogLevel.INFO_BLUE,
                    response.getBody() != null ? response.getBody().asString() : "No Body");
            Reporter.logReportOnly("Response Cookies: ", LogLevel.INFO_BLUE,
                    response.getCookies() != null ? response.getCookies().toString() : "No Cookies");
            Reporter.logReportOnly("Response Headers: ", LogLevel.INFO_BLUE,
                    response.getHeaders() != null ? response.getHeaders().toString() : "No Headers");
            Reporter.logReportOnly("Response Session ID: ", LogLevel.INFO_BLUE,
                    response.getSessionId() != null ? response.getSessionId() : "No Session ID");
            Reporter.logReportOnly("Response Time (ms): ", LogLevel.INFO_BLUE,
                    String.valueOf(response.getTime()));
        }catch (Exception e){
        }
        try {
            Reporter.logReportOnly("Response JSON Path Data: ", LogLevel.INFO_BLUE, response.jsonPath().getString("data.id"));
        } catch (Exception e) {
            Reporter.logReportOnly("Response does not contain JSON Path data.", LogLevel.WARN);
        }
        try {
            Reporter.logReportOnly("Response XML Path Data: ", LogLevel.INFO_BLUE, response.xmlPath().getString("data/id"));
        } catch (Exception e) {
            Reporter.logReportOnly("Response does not contain XML Path data.", LogLevel.WARN);
        }
        return response;
    }
}