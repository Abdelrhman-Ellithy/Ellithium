package Ellithium.Utilities;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;

public class CommandExecutor {

    // Method to execute command and wait for completion
    public static void executeCommand(String command) {
        Allure.step("Executing command: " + command, Status.PASSED);

        try {
            ProcessBuilder builder = getProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            int exitCode = process.waitFor();
            logsUtils.info(Colors.GREEN + "Command exited with code: " + exitCode + Colors.RESET);
            Allure.step("Command executed successfully with exit code: " + exitCode, Status.PASSED);
        } catch (IOException | InterruptedException e) {
            logsUtils.error(Colors.RED + "Failed to execute command: " + command + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Command execution failed: " + command, Status.FAILED);
        }
    }

    // Method to execute command in non-blocking manner and return the process object
    public static Process executeCommandNonBlocking(String command) {
        Allure.step("Executing command in non-blocking mode: " + command, Status.PASSED);

        try {
            ProcessBuilder builder = getProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            logsUtils.info(Colors.GREEN + "Non-blocking command executed: " + command + Colors.RESET);
            Allure.step("Non-blocking command started: " + command, Status.PASSED);

            return process;  // Return the running process
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to execute non-blocking command: " + command + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Non-blocking command execution failed: " + command, Status.FAILED);

            return null;
        }
    }

    // Helper method to construct the process builder based on the OS
    private static ProcessBuilder getProcessBuilder(String command) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            return new ProcessBuilder("/bin/bash", "-c", command);
        }
    }
}