package Ellithium.core.API;

import Ellithium.core.execution.listener.APIListener;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;

public class APIFilterHelper {
    public static void applyFilter() {
        AllureRestAssured allureRestAssured = new AllureRestAssured();
        allureRestAssured.configureHttpExchange(builder ->
                builder.redactHeaders(APIListener.getSensitiveFields())
        );
        RestAssured.filters(allureRestAssured, new APIListener());
    }
}