package Ellithium.core.ai.models;

import java.util.Objects;

/**
 * Data Transfer Object representing a source test case that needs
 * to be processed by the AI generation engine.
 *
 * <p>Supports an optional {@code targetUrl} for live DOM grounding.
 * When a URL is specified, the AI engine will open a headless browser,
 * navigate to the URL, capture the live DOM, and send it to the LLM
 * alongside the test description — preventing hallucinated locators.</p>
 */
public class TestCaseSource {

    private String testId;
    private String sourceFile;
    private String description;
    private String targetUrl;    // Optional URL for live DOM grounding

    public TestCaseSource(String testId, String sourceFile, String description) {
        this(testId, sourceFile, description, null);
    }

    public TestCaseSource(String testId, String sourceFile, String description, String targetUrl) {
        this.testId = testId;
        this.sourceFile = sourceFile;
        this.description = description;
        this.targetUrl = targetUrl;
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

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public boolean hasTargetUrl() {
        return targetUrl != null && !targetUrl.isBlank();
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
                ", targetUrl='" + targetUrl + '\'' +
                '}';
    }
}
