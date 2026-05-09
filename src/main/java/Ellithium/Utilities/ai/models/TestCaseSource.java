package Ellithium.Utilities.ai.models;

import java.util.Objects;

/**
 * Data Transfer Object representing a source test case that needs
 * to be processed by the AI generation engine.
 */
public class TestCaseSource {

    private String testId;
    private String sourceFile;
    private String description;

    public TestCaseSource(String testId, String sourceFile, String description) {
        this.testId = testId;
        this.sourceFile = sourceFile;
        this.description = description;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCaseSource that = (TestCaseSource) o;
        return Objects.equals(testId, that.testId) && Objects.equals(sourceFile, that.sourceFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testId, sourceFile);
    }

    @Override
    public String toString() {
        return "TestCaseSource{" +
                "testId='" + testId + '\'' +
                ", sourceFile='" + sourceFile + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
