package Ellithium.core.ai;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility for programmatically modifying Java source files using AST parsing.
 * Essential for the AI self-healing mechanism to write fixed locators directly
 * back into the Page Object Model (POM) source code.
 *
 * <p>Thread-safe: uses per-file locking to prevent corruption during parallel execution.</p>
 *
 * <p>Supports two locator patterns:</p>
 * <ul>
 *   <li><b>Field-based:</b> {@code private final By loginBtn = By.id("old");}</li>
 *   <li><b>Inline:</b> {@code driver.click(By.id("old"));}</li>
 * </ul>
 *
 * <p>The inline strategy matches by <b>content</b> (the broken locator's value),
 * not by line number. This eliminates the line-shift bug that occurs when
 * JavaParser reformats the file on the first write.</p>
 */
public class JavaSourceModifier {

    // Per-file lock registry to prevent concurrent writes to the same POM file
    private static final ConcurrentHashMap<String, ReentrantLock> fileLocks = new ConcurrentHashMap<>();

    private static ReentrantLock getFileLock(String filePath) {
        return fileLocks.computeIfAbsent(filePath, k -> new ReentrantLock());
    }

    /**
     * Updates the value of a specific 'By' locator field in a given Java class file.
     * For example, it can change:
     * {@code private final By loginButton = By.id("old-id");}
     * into:
     * {@code private final By loginButton = By.cssSelector("#new-id");}
     *
     * <p>This method is thread-safe. Concurrent calls targeting the same file
     * will be serialized via per-file locking.</p>
     *
     * @param filePath    Absolute path to the .java file
     * @param fieldName   The variable name of the locator (e.g., "loginButton")
     * @param newByString The new By declaration as a string (e.g., "By.cssSelector(\"#new-id\")")
     * @return true if successfully modified and saved, false otherwise
     */
    public static boolean updateLocatorValue(String filePath, String fieldName, String newByString) {
        ReentrantLock lock = getFileLock(filePath);
        lock.lock();
        try {
            File javaFile = new File(filePath);
            if (!javaFile.exists()) {
                Reporter.log("Java file not found for AST modification: " + filePath, LogLevel.ERROR);
                return false;
            }

            // Parse the Java file into an Abstract Syntax Tree (AST)
            CompilationUnit cu = StaticJavaParser.parse(javaFile);

            // Find the specific field declaration
            Optional<VariableDeclarator> fieldOpt = cu.findAll(VariableDeclarator.class).stream()
                    .filter(v -> v.getNameAsString().equals(fieldName))
                    .filter(v -> {
                        String t = v.getType().asString();
                        return t.equals("By") || t.equals("AppiumBy");
                    })
                    .findFirst();

            if (fieldOpt.isPresent()) {
                VariableDeclarator field = fieldOpt.get();

                // Parse the new expression string
                Expression newExpression = StaticJavaParser.parseExpression(newByString);

                // Replace the old initialization expression with the new one
                field.setInitializer(newExpression);

                if (newByString.startsWith("AppiumBy.")) {
                    cu.addImport("io.appium.java_client.AppiumBy");
                }

                // Write the updated AST back to the file
                Files.writeString(Paths.get(filePath), cu.toString());
                Reporter.log("Successfully healed locator '" + fieldName + "' in file: " + filePath, LogLevel.INFO_GREEN);
                return true;
            } else {
                Reporter.log("Could not find locator field '" + fieldName + "' in file: " + filePath, LogLevel.ERROR);
                return false;
            }

        } catch (FileNotFoundException e) {
            Reporter.log("File not found: " + filePath, LogLevel.ERROR);
            return false;
        } catch (IOException e) {
            Reporter.log("Failed to write updated AST to file: " + filePath, LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("AST parsing error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates a locator by matching its OLD value content in the AST, regardless of line number.
     *
     * <p>This is the primary method for healing inline locators like:
     * {@code driverActions.elements().sendData(By.id("test"), username);}
     * It finds the By.xxx("test") call anywhere in the file by matching the By method name
     * and argument value, then replaces it with the new expression.</p>
     *
     * <p>This approach is immune to line-number shifts caused by JavaParser reformatting.</p>
     *
     * @param filePath       Path to the .java file
     * @param oldByMethod    The By method name (e.g., "id", "cssSelector", "tagName", "xpath")
     * @param oldByValue     The old locator value string (e.g., "test", "lpl", "bt")
     * @param newByString    The new By declaration as a string (e.g., "By.id(\"username\")")
     * @return true if successfully modified and saved, false otherwise
     */
    public static boolean updateLocatorByOldValue(String filePath, String oldByMethod, String oldByValue, String newByString) {
        ReentrantLock lock = getFileLock(filePath);
        lock.lock();
        try {
            File javaFile = new File(filePath);
            if (!javaFile.exists()) {
                Reporter.log("Java file not found for AST modification: " + filePath, LogLevel.ERROR);
                return false;
            }

            CompilationUnit cu = StaticJavaParser.parse(javaFile);

            // Find ALL MethodCallExpr where scope is "By" or "AppiumBy"
            // and the method name + argument value match the broken locator
            List<MethodCallExpr> allByCalls = cu.findAll(MethodCallExpr.class);

            List<MethodCallExpr> targets = new java.util.ArrayList<>();
            for (MethodCallExpr call : allByCalls) {
                if (!call.getScope().isPresent()) continue;

                String scopeName = call.getScope().get().toString();
                if (!scopeName.equals("By") && !scopeName.equals("AppiumBy")) continue;

                // Check the method name matches (e.g., "id", "cssSelector", "tagName")
                if (!call.getNameAsString().equals(oldByMethod)) continue;

                // Check the argument value matches
                if (call.getArguments().isEmpty()) {
                    if (oldByValue == null || oldByValue.isEmpty()) {
                        targets.add(call);
                    }
                    continue;
                }

                Expression arg = call.getArgument(0);
                if (arg instanceof StringLiteralExpr) {
                    String argValue = ((StringLiteralExpr) arg).getValue();
                    if (argValue.equals(oldByValue)) {
                        targets.add(call);
                    }
                }
            }

            if (!targets.isEmpty()) {
                for (MethodCallExpr targetCall : targets) {
                    targetCall.replace(StaticJavaParser.parseExpression(newByString));
                }

                if (newByString.startsWith("AppiumBy.")) {
                    cu.addImport("io.appium.java_client.AppiumBy");
                }

                Files.writeString(Paths.get(filePath), cu.toString());
                Reporter.log("Successfully healed " + targets.size() + " inline locator(s) By." + oldByMethod
                        + "(\"" + oldByValue + "\") → " + newByString + " in file: " + filePath, LogLevel.INFO_GREEN);
                return true;
            } else {
                // AST search failed (can happen after JavaParser reformats the file).
                // Fallback: direct text-based search and replace on the raw file content.
                String rawContent = Files.readString(Paths.get(filePath));
                String oldPattern = "By." + oldByMethod + "(\"" + oldByValue + "\")";
                if (rawContent.contains(oldPattern)) {
                    String updatedContent = rawContent.replace(oldPattern, newByString);
                    Files.writeString(Paths.get(filePath), updatedContent);
                    Reporter.log("Successfully healed inline locator (text fallback) " + oldPattern
                            + " → " + newByString + " in file: " + filePath, LogLevel.INFO_GREEN);
                    return true;
                }
                Reporter.log("Could not find inline locator By." + oldByMethod + "(\"" + oldByValue
                        + "\") in file (AST + text fallback both failed): " + filePath, LogLevel.ERROR);
                return false;
            }

        } catch (FileNotFoundException e) {
            Reporter.log("File not found: " + filePath, LogLevel.ERROR);
            return false;
        } catch (IOException e) {
            Reporter.log("Failed to write updated AST to file: " + filePath, LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("AST parsing error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        } finally {
            lock.unlock();
        }
    }
}
