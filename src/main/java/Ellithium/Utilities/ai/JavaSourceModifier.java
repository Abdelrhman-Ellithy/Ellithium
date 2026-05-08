package Ellithium.Utilities.ai;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Utility for programmatically modifying Java source files using AST parsing.
 * Essential for the AI self-healing mechanism to write fixed locators directly
 * back into the Page Object Model (POM) source code.
 */
public class JavaSourceModifier {

    /**
     * Updates the value of a specific 'By' locator field in a given Java class file.
     * For example, it can change:
     * private final By loginButton = By.id("old-id");
     * into:
     * private final By loginButton = By.cssSelector("#new-id");
     *
     * @param filePath  Absolute path to the .java file
     * @param fieldName The variable name of the locator (e.g., "loginButton")
     * @param newByString The new By declaration as a string (e.g., "By.cssSelector(\"#new-id\")")
     * @return true if successfully modified and saved, false otherwise
     */
    public static boolean updateLocatorValue(String filePath, String fieldName, String newByString) {
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
                    .findFirst();

            if (fieldOpt.isPresent()) {
                VariableDeclarator field = fieldOpt.get();
                
                // Parse the new expression string
                Expression newExpression = StaticJavaParser.parseExpression(newByString);
                
                // Replace the old initialization expression with the new one
                field.setInitializer(newExpression);

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
        }
    }
}
