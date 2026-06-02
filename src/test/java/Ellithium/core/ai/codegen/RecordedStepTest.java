package Ellithium.core.ai.codegen;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class RecordedStepTest {

    private static LocatorCandidate cand(String expr) {
        return new LocatorCandidate(By.id("x"), expr, 0.9, "id", true, false);
    }

    @Test
    public void constructor_defaultsChosenIndexToZeroWhenCandidatesPresent() {
        RecordedStep s = new RecordedStep("id1", "click", null, "button", "Submit",
                List.of(cand("By.id(\"a\")"), cand("By.id(\"b\")")));
        Assert.assertEquals(s.getChosenIndex(), 0);
    }

    @Test
    public void constructor_defaultsChosenIndexToMinusOneWhenNoCandidates() {
        RecordedStep s = new RecordedStep("id1", "click", null, "button", "Submit", List.of());
        Assert.assertEquals(s.getChosenIndex(), -1);
    }

    @Test
    public void constructor_nullCandidateListTreatedAsEmpty() {
        RecordedStep s = new RecordedStep("id1", "click", null, "button", "Submit", null);
        Assert.assertEquals(s.getCandidates().size(), 0);
        Assert.assertEquals(s.getChosenIndex(), -1);
    }

    @Test
    public void constructor_nullFrameChainDefaultsToEmpty() {
        RecordedStep s = new RecordedStep("id1", "click", null, "button", "Submit", List.of(), null);
        Assert.assertTrue(s.getFrameChain().isEmpty());
    }

    @Test
    public void getters_returnConstructorValues() {
        List<LocatorCandidate> cands = List.of(cand("By.id(\"x\")"));
        List<Integer> frames = List.of(0, 2);
        RecordedStep s = new RecordedStep("abc", "input", "hello", "input", "Email", cands, frames);
        Assert.assertEquals(s.getId(), "abc");
        Assert.assertEquals(s.getActionType(), "input");
        Assert.assertEquals(s.getData(), "hello");
        Assert.assertEquals(s.getTagName(), "input");
        Assert.assertEquals(s.getElementName(), "Email");
        Assert.assertEquals(s.getCandidates(), cands);
        Assert.assertEquals(s.getFrameChain(), frames);
    }

    @Test
    public void chosen_returnsCorrectCandidate() {
        LocatorCandidate c0 = cand("By.id(\"a\")");
        LocatorCandidate c1 = cand("By.id(\"b\")");
        RecordedStep s = new RecordedStep("id1", "click", null, "button", "X", List.of(c0, c1));
        Assert.assertEquals(s.chosen(), c0);
        s.choose(1);
        Assert.assertEquals(s.chosen(), c1);
    }

    @Test
    public void chosen_returnsNullWhenNoCandidates() {
        RecordedStep s = new RecordedStep("id1", "click", null, "button", "X", List.of());
        Assert.assertNull(s.chosen());
    }

    @Test
    public void choose_outOfBounds_doesNotChange() {
        RecordedStep s = new RecordedStep("id1", "click", null, "button", "X",
                List.of(cand("By.id(\"a\")")));
        s.choose(99);
        Assert.assertEquals(s.getChosenIndex(), 0);
    }

    @Test
    public void choose_negativeIndex_doesNotChange() {
        RecordedStep s = new RecordedStep("id1", "click", null, "button", "X",
                List.of(cand("By.id(\"a\")")));
        s.choose(-1);
        Assert.assertEquals(s.getChosenIndex(), 0);
    }

    @Test
    public void generatorMethod_nullByDefault() {
        RecordedStep s = new RecordedStep("id1", "input", "val", "input", "Email", List.of());
        Assert.assertNull(s.getGeneratorMethod());
    }

    @Test
    public void generatorMethod_setAndGet() {
        RecordedStep s = new RecordedStep("id1", "input", "val", "input", "Email", List.of());
        s.setGeneratorMethod("getRandomEmail");
        Assert.assertEquals(s.getGeneratorMethod(), "getRandomEmail");
    }

    @Test
    public void generatorMethod_setToNull_clearsIt() {
        RecordedStep s = new RecordedStep("id1", "input", "val", "input", "Email", List.of());
        s.setGeneratorMethod("getRandomEmail");
        s.setGeneratorMethod(null);
        Assert.assertNull(s.getGeneratorMethod());
    }
}
