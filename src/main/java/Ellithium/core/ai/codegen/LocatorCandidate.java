package Ellithium.core.ai.codegen;

import org.openqa.selenium.By;

public record LocatorCandidate(By by, String javaExpression, double score, String tier,
                               boolean unique, boolean parameterizable) {

    public static LocatorCandidate of(By by, String javaExpression, double score, String tier,
                                      boolean unique, boolean parameterizable) {
        return new LocatorCandidate(by, javaExpression, score, tier, unique, parameterizable);
    }
}
