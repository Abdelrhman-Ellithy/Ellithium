package Ellithium.Utilities.ai.models;

import java.util.Date;

/**
 * Data Transfer Object representing a single mapping entry between
 * a source test case and its generated assets.
 */
public class TraceabilityRecord {

    private TestCaseSource source;
    private GeneratedAssets assets;
    private Date generatedAt;

    public TraceabilityRecord() {
        this.generatedAt = new Date();
    }

    public TraceabilityRecord(TestCaseSource source, GeneratedAssets assets) {
        this.source = source;
        this.assets = assets;
        this.generatedAt = new Date();
    }

    public TestCaseSource getSource() {
        return source;
    }

    public void setSource(TestCaseSource source) {
        this.source = source;
    }

    public GeneratedAssets getAssets() {
        return assets;
    }

    public void setAssets(GeneratedAssets assets) {
        this.assets = assets;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }

    @Override
    public String toString() {
        return "TraceabilityRecord{" +
                "source=" + source +
                ", assets=" + assets +
                ", generatedAt=" + generatedAt +
                '}';
    }
}
