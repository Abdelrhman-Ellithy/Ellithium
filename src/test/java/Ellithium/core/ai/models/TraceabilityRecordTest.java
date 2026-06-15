package Ellithium.core.ai.models;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

public class TraceabilityRecordTest {

    @Test
    public void defaultConstructor_generatedAtIsNotNull() {
        TraceabilityRecord r = new TraceabilityRecord();
        Assert.assertNotNull(r.getGeneratedAt());
    }

    @Test
    public void fullConstructor_setsSourceAndAssets() {
        TestCaseSource source = new TestCaseSource("t1", "f.txt", "desc");
        GeneratedAssets assets = new GeneratedAssets();
        TraceabilityRecord r = new TraceabilityRecord(source, assets);
        Assert.assertSame(r.getSource(), source);
        Assert.assertSame(r.getAssets(), assets);
    }

    @Test
    public void setters_updateSourceAndAssets() {
        TraceabilityRecord r = new TraceabilityRecord();
        TestCaseSource source = new TestCaseSource("t2", "g.txt", "d");
        GeneratedAssets assets = new GeneratedAssets();
        r.setSource(source);
        r.setAssets(assets);
        Assert.assertSame(r.getSource(), source);
        Assert.assertSame(r.getAssets(), assets);
    }

    @Test
    public void setGeneratedAt_updatesDate() {
        TraceabilityRecord r = new TraceabilityRecord();
        Date d = new Date(0);
        r.setGeneratedAt(d);
        Assert.assertEquals(r.getGeneratedAt(), d);
    }

    @Test
    public void toString_containsSource() {
        TestCaseSource source = new TestCaseSource("tc-5", "file.txt", "d");
        TraceabilityRecord r = new TraceabilityRecord(source, new GeneratedAssets());
        Assert.assertTrue(r.toString().contains("tc-5"));
    }
}
