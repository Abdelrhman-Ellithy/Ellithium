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
            Reporter.log("Request Method: ", LogLevel.INFO_BLUE, requestSpec.getMethod());
            Reporter.log("Request URI: ", LogLevel.INFO_BLUE, requestSpec.getURI());
            Reporter.log("Request Headers: ", LogLevel.INFO_BLUE,
                    requestSpec.getHeaders() != null ? requestSpec.getHeaders().toString() : "No Headers");
            Reporter.log("Request Parameters: ", LogLevel.INFO_BLUE,
                    requestSpec.getQueryParams() != null ? requestSpec.getQueryParams().toString() : "No Query Params");
            Reporter.log("Request Base Path: ", LogLevel.INFO_BLUE,
                    requestSpec.getBasePath() != null ? requestSpec.getBasePath() : "No Base Path");
            Reporter.log("Request Proxy Specification: ", LogLevel.INFO_BLUE,
                    requestSpec.getProxySpecification() != null ? requestSpec.getProxySpecification().toString() : "No Proxy");
            Reporter.log("Request ContentType: ", LogLevel.INFO_BLUE,
                    requestSpec.getContentType() != null ? requestSpec.getContentType() : "No Content Type");
            Reporter.log("Request Cookies: ", LogLevel.INFO_BLUE,
                    requestSpec.getCookies() != null ? requestSpec.getCookies().toString() : "No Cookies");
            Reporter.log("Request Params: ", LogLevel.INFO_BLUE,
                    requestSpec.getRequestParams() != null ? requestSpec.getRequestParams().toString() : "No Request Params");
            Reporter.log("Request Body: ", LogLevel.INFO_BLUE,
                    requestSpec.getBody() != null ? requestSpec.getBody().toString() : "No Body");
            Reporter.log("Response Status Code: ", LogLevel.INFO_BLUE,
                    String.valueOf(response.getStatusCode()));
            Reporter.log("Response ContentType: ", LogLevel.INFO_BLUE,
                    response.getContentType() != null ? response.getContentType() : "No Content Type");
            Reporter.log("Response Status Line: ", LogLevel.INFO_BLUE, response.getStatusLine());
            Reporter.log("Detailed Response Cookies: ", LogLevel.INFO_BLUE, response.detailedCookies().toString());
            Reporter.log("Response Content-Length: ", LogLevel.INFO_BLUE, response.getHeader("Content-Length"));
            Reporter.log("Response Body: ", LogLevel.INFO_BLUE,
                    response.getBody() != null ? response.getBody().asString() : "No Body");
            Reporter.log("Response Cookies: ", LogLevel.INFO_BLUE,
                    response.getCookies() != null ? response.getCookies().toString() : "No Cookies");
            Reporter.log("Response Headers: ", LogLevel.INFO_BLUE,
                    response.getHeaders() != null ? response.getHeaders().toString() : "No Headers");
            Reporter.log("Response Session ID: ", LogLevel.INFO_BLUE,
                    response.getSessionId() != null ? response.getSessionId() : "No Session ID");
            Reporter.log("Response Time (ms): ", LogLevel.INFO_BLUE,
                    String.valueOf(response.getTime()));
        }catch (Exception e){
        }
        try {
            Reporter.log("Response JSON Path Data: ", LogLevel.INFO_BLUE, response.jsonPath().getString("data.id"));
        } catch (Exception e) {
            Reporter.log("Response does not contain JSON Path data.", LogLevel.WARN);
        }
        try {
            Reporter.log("Response XML Path Data: ", LogLevel.INFO_BLUE, response.xmlPath().getString("data/id"));
        } catch (Exception e) {
            Reporter.log("Response does not contain XML Path data.", LogLevel.WARN);
        }
        return response;
    }
}