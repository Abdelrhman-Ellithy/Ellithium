package Ellithium.core.ai.models;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class GeneratedAssetsTest {

    @Test
    public void defaultConstructor_pomMethodsIsEmptyList() {
        GeneratedAssets a = new GeneratedAssets();
        Assert.assertNotNull(a.getPomMethods());
        Assert.assertTrue(a.getPomMethods().isEmpty());
    }

    @Test
    public void fullConstructor_setsAllFields() {
        List<String> methods = List.of("login()", "logout()");
        GeneratedAssets a = new GeneratedAssets("com.example", "LoginTest", "testLogin",
                "LoginPage", methods, "login.feature");
        Assert.assertEquals(a.getTargetPackage(), "com.example");
        Assert.assertEquals(a.getTestClass(), "LoginTest");
        Assert.assertEquals(a.getTestMethod(), "testLogin");
        Assert.assertEquals(a.getPomClass(), "LoginPage");
        Assert.assertEquals(a.getPomMethods(), methods);
        Assert.assertEquals(a.getFeatureFile(), "login.feature");
    }

    @Test
    public void fullConstructor_nullPomMethods_defaultsToEmptyList() {
        GeneratedAssets a = new GeneratedAssets("pkg", "Class", "method", "Pom", null, "f.feature");
        Assert.assertNotNull(a.getPomMethods());
        Assert.assertTrue(a.getPomMethods().isEmpty());
    }

    @Test
    public void setters_updateFields() {
        GeneratedAssets a = new GeneratedAssets();
        a.setTargetPackage("com.test");
        a.setTestClass("MyTest");
        a.setTestMethod("run");
        a.setPomClass("MyPage");
        a.setPomMethods(List.of("click()"));
        a.setFeatureFile("my.feature");
        Assert.assertEquals(a.getTargetPackage(), "com.test");
        Assert.assertEquals(a.getTestClass(), "MyTest");
        Assert.assertEquals(a.getTestMethod(), "run");
        Assert.assertEquals(a.getPomClass(), "MyPage");
        Assert.assertEquals(a.getPomMethods(), List.of("click()"));
        Assert.assertEquals(a.getFeatureFile(), "my.feature");
    }

    @Test
    public void toString_containsTestClass() {
        GeneratedAssets a = new GeneratedAssets("pkg", "AuthTest", "m", "p", null, "f");
        Assert.assertTrue(a.toString().contains("AuthTest"));
    }
}
