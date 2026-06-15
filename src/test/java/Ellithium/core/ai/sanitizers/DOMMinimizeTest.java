package Ellithium.core.ai.sanitizers;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests DOMMinimizer.minimize() directly against HTML strings —
 * no WebDriver needed for the pure-text minimization path.
 */
public class DOMMinimizeTest {

    // ── Phase 1: noise tag stripping ─────────────────────────────────────────

    @Test
    public void minimize_stripsScriptTags() {
        String html = "<body><script>alert('xss');</script><button id=\"go\">Go</button></body>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertFalse(result.contains("alert('xss')"), "Script content must be removed");
        Assert.assertTrue(result.contains("go") || result.contains("Go"),
                "Button must survive minimization");
    }

    @Test
    public void minimize_stripsStyleTags() {
        String html = "<html><style>body { color: red; }</style><button>Click</button></html>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertFalse(result.contains("color: red"), "Style content must be removed");
    }

    @Test
    public void minimize_stripsHtmlComments() {
        String html = "<div><!-- This is a comment --><input id=\"user\"></div>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertFalse(result.contains("This is a comment"), "HTML comments must be removed");
    }

    @Test
    public void minimize_stripsSvgTags() {
        String html = "<div><svg width=\"100\"><circle r=\"10\"/></svg><button>OK</button></div>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertFalse(result.contains("<circle"), "SVG internals must be removed");
    }

    // ── Phase 2: attribute noise stripping ───────────────────────────────────

    @Test
    public void minimize_stripsInlineStyleAttributes() {
        String html = "<button style=\"color:red;font-size:14px\" id=\"btn\">Submit</button>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertFalse(result.contains("color:red"), "Inline style attributes must be stripped");
    }

    @Test
    public void minimize_stripsEventHandlerAttributes() {
        String html = "<button onclick=\"doSomething()\" id=\"btn\">Click</button>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertFalse(result.contains("onclick"), "Event handler attributes must be stripped");
    }

    @Test
    public void minimize_preservesIdAttribute() {
        String html = "<form id=\"login-form\"><input id=\"username\" placeholder=\"User\"></form>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertTrue(result.contains("username") || result.contains("login-form"),
                "Stable identity attributes (id) must be preserved");
    }

    @Test
    public void minimize_preservesAriaLabel() {
        String html = "<button aria-label=\"Submit form\" id=\"sub\">Go</button>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertTrue(result.contains("Submit form"), "aria-label must be preserved");
    }

    @Test
    public void minimize_preservesPlaceholder() {
        String html = "<input placeholder=\"Enter your email\" type=\"email\">";
        String result = DOMMinimizer.minimize(html);
        Assert.assertTrue(result.contains("Enter your email"), "placeholder must be preserved");
    }

    // ── Output size reduction ────────────────────────────────────────────────

    @Test
    public void minimize_outputShorterThanInput_onNoisyHtml() {
        // Build a page where script/style noise dominates so the minimized output is smaller.
        // The minimizer adds comment headers (~200 chars), so the noise must be much larger.
        StringBuilder script = new StringBuilder("<script>");
        for (int i = 0; i < 100; i++) script.append("var v").append(i).append("=").append(i).append(";");
        script.append("</script>");
        StringBuilder style = new StringBuilder("<style>");
        for (int i = 0; i < 80; i++) style.append(".cls").append(i).append("{color:red;margin:").append(i).append("px}");
        style.append("</style>");

        String html = "<html><head>" + script + style + "</head>"
                + "<body><form id=\"auth\" action=\"/login\">"
                + "<input id=\"user\" placeholder=\"Username\" type=\"text\">"
                + "<input id=\"pass\" placeholder=\"Password\" type=\"password\">"
                + "<button type=\"submit\">Login</button>"
                + "</form></body></html>";

        String result = DOMMinimizer.minimize(html);
        Assert.assertTrue(result.length() < html.length(),
                "Minimized DOM must be shorter than noisy raw HTML, got: " + result.length() + " vs " + html.length());
    }

    @Test
    public void minimize_nullInput_returnsEmpty() {
        Assert.assertEquals(DOMMinimizer.minimize(null), "");
    }

    @Test
    public void minimize_emptyInput_returnsEmpty() {
        Assert.assertEquals(DOMMinimizer.minimize(""), "");
    }

    // ── Semantic grouping output ─────────────────────────────────────────────

    @Test
    public void minimize_formLandmark_groupsInputsUnderForm() {
        String html = "<form id=\"login\" action=\"/auth\">"
                + "<input id=\"user\" type=\"text\" placeholder=\"Username\">"
                + "<button type=\"submit\">Login</button>"
                + "</form>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertTrue(result.contains("--- ") && result.contains("form"),
                "Semantic grouping should create a '--- <form ...' section");
    }

    @Test
    public void minimize_interactiveElementComment_present() {
        String html = "<div><button>A</button><button>B</button></div>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertTrue(result.contains("INTERACTIVE ELEMENTS"),
                "Output must contain the interactive elements header comment");
    }

    @Test
    public void minimize_ungroupedInteractives_appearsWhenNoLandmark() {
        // Buttons outside any landmark go to the Ungrouped section
        String html = "<div><button id=\"floating-btn\">Float</button></div>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertTrue(result.contains("Ungrouped") || result.contains("floating-btn"),
                "Ungrouped interactive elements must appear in output");
    }

    @Test
    public void minimize_pageTitle_extractedToStructuralContext() {
        String html = "<html><head><title>My Login Page</title></head>"
                + "<body><button>Go</button></body></html>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertTrue(result.contains("My Login Page"),
                "Page title should appear in structural context section");
    }

    @Test
    public void minimize_navLandmark_groupsLinksUnderNav() {
        String html = "<nav id=\"main-nav\"><a href=\"/login\">Login</a><a href=\"/signup\">Signup</a></nav>";
        String result = DOMMinimizer.minimize(html);
        Assert.assertTrue(result.contains("nav"), "Nav landmark must be identified");
        Assert.assertTrue(result.contains("/login") || result.contains("Login"),
                "Links inside nav must appear in output");
    }

    // ── Safety cap ───────────────────────────────────────────────────────────

    @Test
    public void minimize_veryLargeInput_truncatedAt15000Chars() {
        // Build a very large HTML page that exceeds the 15,000 char cap
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 0; i < 2000; i++) {
            sb.append("<button id=\"btn").append(i).append("\">Button ").append(i).append("</button>");
        }
        sb.append("</body></html>");

        String result = DOMMinimizer.minimize(sb.toString());
        // 15,000 + length of truncation marker
        Assert.assertTrue(result.length() <= 15_100,
                "Output must be capped near 15,000 chars, was: " + result.length());
    }
}
