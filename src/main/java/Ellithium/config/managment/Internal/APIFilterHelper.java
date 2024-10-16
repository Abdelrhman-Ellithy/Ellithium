package Ellithium.config.managment.Internal;

import Ellithium.core.execution.listener.APIListener;
import io.restassured.RestAssured;

public class APIFilterHelper {
    public static void applyFilter(){
        RestAssured.filters(new APIListener());
    }
}
