package Ellithium.core.ai.models;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestCaseSourceTest {

    @Test
    public void constructor_threeArg_setsFields() {
        TestCaseSource s = new TestCaseSource("tc-1", "login.txt", "Login flow");
        Assert.assertEquals(s.getTestId(), "tc-1");
        Assert.assertEquals(s.getSourceFile(), "login.txt");
        Assert.assertEquals(s.getDescription(), "Login flow");
        Assert.assertNull(s.getTargetUrl());
    }

    @Test
    public void constructor_fourArg_setsUrl() {
        TestCaseSource s = new TestCaseSource("tc-2", "f.txt", "desc", "http://localhost");
        Assert.assertEquals(s.getTargetUrl(), "http://localhost");
    }

    @Test
    public void hasTargetUrl_withUrl_returnsTrue() {
        TestCaseSource s = new TestCaseSource("x", "f", "d", "http://x.com");
        Assert.assertTrue(s.hasTargetUrl());
    }

    @Test
    public void hasTargetUrl_noUrl_returnsFalse() {
        TestCaseSource s = new TestCaseSource("x", "f", "d");
        Assert.assertFalse(s.hasTargetUrl());
    }

    @Test
    public void hasTargetUrl_blankUrl_returnsFalse() {
        TestCaseSource s = new TestCaseSource("x", "f", "d", "  ");
        Assert.assertFalse(s.hasTargetUrl());
    }

    @Test
    public void setters_updateFields() {
        TestCaseSource s = new TestCaseSource("a", "b", "c");
        s.setTestId("new-id");
        s.setSourceFile("new-file.txt");
        s.setDescription("new desc");
        s.setTargetUrl("http://new.com");
        Assert.assertEquals(s.getTestId(), "new-id");
        Assert.assertEquals(s.getSourceFile(), "new-file.txt");
        Assert.assertEquals(s.getDescription(), "new desc");
        Assert.assertEquals(s.getTargetUrl(), "http://new.com");
    }

    @Test
    public void equals_sameIdAndFile_returnsTrue() {
        TestCaseSource a = new TestCaseSource("id1", "file.txt", "desc1");
        TestCaseSource b = new TestCaseSource("id1", "file.txt", "different desc");
        Assert.assertEquals(a, b);
    }

    @Test
    public void equals_differentId_returnsFalse() {
        TestCaseSource a = new TestCaseSource("id1", "file.txt", "desc");
        TestCaseSource b = new TestCaseSource("id2", "file.txt", "desc");
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void hashCode_equalObjects_sameHash() {
        TestCaseSource a = new TestCaseSource("id", "f.txt", "d1");
        TestCaseSource b = new TestCaseSource("id", "f.txt", "d2");
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void toString_containsTestId() {
        TestCaseSource s = new TestCaseSource("tc-99", "f.txt", "d");
        Assert.assertTrue(s.toString().contains("tc-99"));
    }
}
