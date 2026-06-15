package Ellithium.core.ai.healing;

import Ellithium.core.ai.config.AIConfigLoader;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class HealingPromptBuilderTest {

    @BeforeClass
    public void init() {
        AIConfigLoader.initialize();
    }

    // ── buildSystemPrompt (web) ───────────────────────────────────────────────

    @Test
    public void buildSystemPrompt_web_containsRoleIntroduction() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(false);
        Assert.assertTrue(prompt.contains("expert Selenium"),
                "System prompt must establish role: " + prompt.substring(0, 100));
    }

    @Test
    public void buildSystemPrompt_web_containsWebLocatorStrategies() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(false);
        Assert.assertTrue(prompt.contains("By.id"),
                "Web prompt must reference By.id strategy");
        Assert.assertTrue(prompt.contains("By.cssSelector"),
                "Web prompt must reference By.cssSelector strategy");
    }

    @Test
    public void buildSystemPrompt_web_doesNotMentionAppiumBy() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(false);
        Assert.assertFalse(prompt.contains("AppiumBy"),
                "Web prompt must not mention AppiumBy");
    }

    @Test
    public void buildSystemPrompt_web_containsJsonResponseFormat() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(false);
        Assert.assertTrue(prompt.contains("candidates"),
                "System prompt must specify JSON candidates format");
        Assert.assertTrue(prompt.contains("confidence"),
                "System prompt must include confidence field");
    }

    @Test
    public void buildSystemPrompt_web_referencesMaxCandidates() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(false);
        int maxCandidates = AIConfigLoader.getMaxCandidates();
        Assert.assertTrue(prompt.contains(String.valueOf(maxCandidates)),
                "Prompt must embed maxCandidates=" + maxCandidates + " in response instructions");
    }

    @Test
    public void buildSystemPrompt_web_preferenceOrderMentioned() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(false);
        Assert.assertTrue(prompt.contains("id") && prompt.contains("name"),
                "System prompt must state locator preference order (id > name)");
    }

    @Test
    public void buildSystemPrompt_web_containsMethodNameHint() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(false);
        Assert.assertTrue(prompt.contains("METHOD NAME") || prompt.contains("method name"),
                "Prompt must instruct LLM to use method name as hint");
    }

    // ── buildSystemPrompt (mobile) ────────────────────────────────────────────

    @Test
    public void buildSystemPrompt_mobile_mentionsAppiumBy() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(true);
        Assert.assertTrue(prompt.contains("AppiumBy"),
                "Mobile prompt must mention AppiumBy locators");
    }

    @Test
    public void buildSystemPrompt_mobile_strategyLine_usesAppiumBy_notCssSelector() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(true);
        String[] lines = prompt.split("\n");
        boolean foundUseLine = false;
        for (String line : lines) {
            if (line.startsWith("Use ") && line.contains("AppiumBy")) {
                foundUseLine = true;
                Assert.assertFalse(line.contains("cssSelector"),
                        "Mobile strategy instruction line must not list cssSelector: " + line);
                break;
            }
        }
        Assert.assertTrue(foundUseLine, "Mobile prompt must have a 'Use AppiumBy...' instruction line");
    }

    @Test
    public void buildSystemPrompt_mobile_mentionsAccessibilityId() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(true);
        Assert.assertTrue(prompt.contains("accessibilityId"),
                "Mobile prompt must reference accessibilityId strategy");
    }

    @Test
    public void buildSystemPrompt_mobile_mentionsIosClassChain() {
        String prompt = HealingPromptBuilder.buildSystemPrompt(true);
        Assert.assertTrue(prompt.contains("iOSClassChain"),
                "Mobile prompt must reference iOSClassChain strategy");
    }

    @Test
    public void buildSystemPrompt_returnsNonBlankString() {
        Assert.assertFalse(HealingPromptBuilder.buildSystemPrompt(false).isBlank());
        Assert.assertFalse(HealingPromptBuilder.buildSystemPrompt(true).isBlank());
    }

    // ── buildUserPrompt ───────────────────────────────────────────────────────

    @Test
    public void buildUserPrompt_includesBrokenLocator() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"old-login-btn\")";
        String prompt = HealingPromptBuilder.buildUserPrompt(ctx);
        Assert.assertTrue(prompt.contains("By.id(\"old-login-btn\")"),
                "User prompt must embed the broken locator");
    }

    @Test
    public void buildUserPrompt_includesMethodName_whenPresent() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"x\")";
        ctx.methodName = "clickLoginButton";
        String prompt = HealingPromptBuilder.buildUserPrompt(ctx);
        Assert.assertTrue(prompt.contains("clickLoginButton"),
                "User prompt must embed the method name");
    }

    @Test
    public void buildUserPrompt_includesPageClass_whenPresent() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"x\")";
        ctx.pageClassName = "LoginPage";
        String prompt = HealingPromptBuilder.buildUserPrompt(ctx);
        Assert.assertTrue(prompt.contains("LoginPage"),
                "User prompt must embed the page class name");
    }

    @Test
    public void buildUserPrompt_includesActionType_whenKnown() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"x\")";
        ctx.actionType = "clickOnElement";
        String prompt = HealingPromptBuilder.buildUserPrompt(ctx);
        Assert.assertTrue(prompt.contains("clickOnElement"),
                "User prompt must embed the action type");
    }

    @Test
    public void buildUserPrompt_excludesUnknownActionType() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"x\")";
        ctx.actionType = "unknown";
        String prompt = HealingPromptBuilder.buildUserPrompt(ctx);
        Assert.assertFalse(prompt.contains("Action: unknown"),
                "User prompt must skip 'unknown' action type");
    }

    @Test
    public void buildUserPrompt_includesSemanticQuery_whenPresent() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"x\")";
        ctx.semanticQuery = "login button submit";
        String prompt = HealingPromptBuilder.buildUserPrompt(ctx);
        Assert.assertTrue(prompt.contains("login button submit"),
                "User prompt must embed the semantic intent");
    }

    @Test
    public void buildUserPrompt_wrapsLongDomWithUntrustedTags() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"x\")";
        ctx.minimizedDom = "<html><body><button id=\"login\">Login</button></body></html>";
        String prompt = HealingPromptBuilder.buildUserPrompt(ctx);
        Assert.assertTrue(prompt.contains("[BEGIN UNTRUSTED DOM"),
                "DOM section must be wrapped with prompt-injection guard");
        Assert.assertTrue(prompt.contains("[END UNTRUSTED DOM]"),
                "DOM section must be closed with end tag");
    }

    @Test
    public void buildUserPrompt_truncatesDomAtMaxChars() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"x\")";
        ctx.minimizedDom = "X".repeat(HealingPromptBuilder.MAX_DOM_CHARS + 5000);
        String prompt = HealingPromptBuilder.buildUserPrompt(ctx);
        Assert.assertTrue(prompt.contains("truncated"),
                "Prompt must indicate DOM was truncated when over max chars");
    }

    @Test
    public void buildUserPrompt_omitsDom_whenNull() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"x\")";
        ctx.minimizedDom = null;
        String prompt = HealingPromptBuilder.buildUserPrompt(ctx);
        Assert.assertFalse(prompt.contains("UNTRUSTED DOM"),
                "Prompt must not add DOM section when DOM is null");
    }

    @Test
    public void buildUserPrompt_minimalContext_returnsNonBlank() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        ctx.brokenLocatorStr = "By.id(\"any\")";
        Assert.assertFalse(HealingPromptBuilder.buildUserPrompt(ctx).isBlank());
    }
}
