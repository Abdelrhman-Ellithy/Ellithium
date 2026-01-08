package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;

public class Sleep {
    /**
     * Sleeps for a specified number of milliseconds.
     * @param millis Number of milliseconds to sleep
     */
    public  void sleepMillis(long millis) {
        try {
            Reporter.log("Sleeping for " + millis + " milliseconds", LogLevel.INFO_BLUE);
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Reporter.log("Sleep interrupted: " + e.getMessage(), LogLevel.ERROR);
            Logger.logException( e);
        }
    }

    /**
     * Sleeps for a specified number of seconds.
     * @param seconds Number of seconds to sleep
     */
    public  void sleepSeconds(long seconds) {
        sleepMillis(seconds * 1000);
    }

    /**
     * Sleeps for a specified number of minutes.
     * @param minutes Number of minutes to sleep
     */
    public  void sleepMinutes(long minutes) {
        sleepSeconds(minutes * 60);
    }
}
