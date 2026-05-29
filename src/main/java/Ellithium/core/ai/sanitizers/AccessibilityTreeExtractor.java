package Ellithium.core.ai.sanitizers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Universal Accessibility Tree extraction via JavaScript injection.
 *
 * <p>Instead of using Chrome DevTools Protocol (CDP), which only works on Chromium,
 * this extractor injects a JavaScript function via {@link JavascriptExecutor} that
 * walks the DOM and builds an accessibility-like tree. This approach works on
 * <b>all browsers</b>: Chrome, Firefox, Safari, Edge, and Appium WebViews.</p>
 *
 * <p>For each visible, interactive element it captures:</p>
 * <ul>
 *   <li><b>role</b> — from ARIA role or implicit HTML role mapping</li>
 *   <li><b>name</b> — from aria-label, label association, placeholder, title, alt, or visible text</li>
 *   <li><b>id, name attribute, data-testid</b> — for locator reconstruction</li>
 *   <li><b>type, value, checked, selected, disabled, required</b> — element state</li>
 *   <li><b>depth</b> — nesting level for spatial context</li>
 * </ul>
 *
 * <p>The output is a compact, human-readable text tree that is 10-50x smaller than
 * raw HTML and far more semantically meaningful for LLMs.</p>
 *
 * <p>For native Appium (no JavaScript support), falls back to
 * {@code DOMMinimizer.minimize(driver.getPageSource())}.</p>
 */
public class AccessibilityTreeExtractor {

    /** Maximum output length in characters (~2000 tokens for most LLMs) */
    private static final int MAX_OUTPUT_LENGTH = 8_000;

    /**
     * The JavaScript function injected into the page to walk the DOM and build
     * an accessibility tree representation.
     *
     * <p>This script:</p>
     * <ol>
     *   <li>Defines implicit ARIA role mappings for standard HTML elements</li>
     *   <li>Computes the accessible name for each element (aria-label, label, placeholder, text, alt)</li>
     *   <li>Filters out invisible elements (display:none, visibility:hidden, aria-hidden)</li>
     *   <li>Groups elements by semantic landmark regions (header, nav, main, form, footer)</li>
     *   <li>Returns a compact text representation</li>
     * </ol>
     */
    private static final String AX_TREE_SCRIPT = """
        return (function() {
            var ROLE_MAP = {
                'A': 'link', 'BUTTON': 'button', 'INPUT': function(el) {
                    var t = (el.type || 'text').toLowerCase();
                    if (t === 'checkbox') return 'checkbox';
                    if (t === 'radio') return 'radio';
                    if (t === 'submit' || t === 'button' || t === 'reset') return 'button';
                    if (t === 'search') return 'searchbox';
                    if (t === 'email' || t === 'tel' || t === 'url' || t === 'number') return 'textbox';
                    if (t === 'password') return 'textbox';
                    if (t === 'hidden') return null;
                    return 'textbox';
                },
                'TEXTAREA': 'textbox', 'SELECT': 'combobox', 'OPTION': 'option',
                'IMG': 'img', 'TABLE': 'table', 'FORM': 'form',
                'NAV': 'navigation', 'MAIN': 'main', 'HEADER': 'banner',
                'FOOTER': 'contentinfo', 'ASIDE': 'complementary',
                'SECTION': 'region', 'DIALOG': 'dialog', 'H1': 'heading',
                'H2': 'heading', 'H3': 'heading', 'H4': 'heading',
                'UL': 'list', 'OL': 'list', 'LI': 'listitem',
                'LABEL': 'label'
            };

            var LANDMARK_TAGS = ['HEADER','NAV','MAIN','ASIDE','FOOTER','SECTION','FORM','DIALOG'];
            var INTERACTIVE_TAGS = ['INPUT','BUTTON','SELECT','TEXTAREA','A','LABEL'];

            function getRole(el) {
                var explicit = el.getAttribute('role');
                if (explicit) return explicit.toLowerCase();
                var mapped = ROLE_MAP[el.tagName];
                if (typeof mapped === 'function') return mapped(el);
                return mapped || null;
            }

            function getName(el) {
                var label = el.getAttribute('aria-label');
                if (label) return label.trim();

                var labelledBy = el.getAttribute('aria-labelledby');
                if (labelledBy) {
                    var parts = labelledBy.split(/\\s+/).map(function(id) {
                        var ref = document.getElementById(id);
                        return ref ? ref.textContent.trim() : '';
                    }).filter(Boolean);
                    if (parts.length) return parts.join(' ');
                }

                if (el.id) {
                    var labelEl = document.querySelector('label[for="' + el.id + '"]');
                    if (labelEl) return labelEl.textContent.trim();
                }

                if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                    if (el.placeholder) return el.placeholder.trim();
                    if (el.title) return el.title.trim();
                }

                if (el.tagName === 'IMG') {
                    return (el.alt || el.title || '').trim();
                }

                var text = '';
                for (var i = 0; i < el.childNodes.length; i++) {
                    if (el.childNodes[i].nodeType === 3) {
                        text += el.childNodes[i].textContent;
                    }
                }
                text = text.trim();
                if (text.length > 0 && text.length <= 80) return text;
                return '';
            }

            function isVisible(el) {
                if (el.getAttribute('aria-hidden') === 'true') return false;
                if (el.hidden) return false;
                var style = window.getComputedStyle(el);
                if (style.display === 'none' || style.visibility === 'hidden') return false;
                if (el.offsetWidth === 0 && el.offsetHeight === 0) return false;
                return true;
            }

            function getAttrs(el) {
                var attrs = [];
                if (el.id) attrs.push('id=' + el.id);
                if (el.name) attrs.push('name=' + el.name);
                if (el.getAttribute('data-testid')) attrs.push('data-testid=' + el.getAttribute('data-testid'));
                if (el.type && el.tagName === 'INPUT') attrs.push('type=' + el.type);
                if (el.href && el.tagName === 'A') {
                    var href = el.getAttribute('href');
                    if (href && href.length <= 60) attrs.push('href=' + href);
                }
                if (el.value && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') && el.type !== 'password') {
                    var v = el.value.length > 30 ? el.value.substring(0, 27) + '...' : el.value;
                    attrs.push('value="' + v + '"');
                }
                if (el.checked) attrs.push('checked');
                if (el.selected) attrs.push('selected');
                if (el.disabled) attrs.push('disabled');
                if (el.required) attrs.push('required');
                if (el.readOnly) attrs.push('readonly');
                if (el.getAttribute('class')) {
                    var cls = el.getAttribute('class').trim();
                    if (cls.length <= 50) attrs.push('class="' + cls + '"');
                }
                return attrs.join(' ');
            }

            var output = [];
            var landmarkStack = [];

            function walk(node, depth) {
                if (node.nodeType !== 1) return;
                if (!isVisible(node)) return;

                var tag = node.tagName;
                var role = getRole(node);
                var isLandmark = LANDMARK_TAGS.indexOf(tag) !== -1 || 
                    ['navigation','main','banner','contentinfo','complementary','region','dialog','form'].indexOf(role) !== -1;
                var isInteractive = INTERACTIVE_TAGS.indexOf(tag) !== -1 || 
                    node.getAttribute('role') === 'button' || node.getAttribute('role') === 'link' ||
                    node.getAttribute('role') === 'textbox' || node.getAttribute('role') === 'checkbox' ||
                    node.getAttribute('role') === 'tab' || node.getAttribute('role') === 'menuitem' ||
                    node.getAttribute('data-testid') != null;
                var isHeading = /^H[1-6]$/.test(tag);

                if (isLandmark) {
                    var indent = '  '.repeat(depth);
                    var landmarkAttrs = '';
                    if (node.id) landmarkAttrs += ' id=' + node.id;
                    if (node.getAttribute('aria-label')) landmarkAttrs += ' "' + node.getAttribute('aria-label') + '"';
                    if (tag === 'FORM' && node.action) landmarkAttrs += ' action=' + node.getAttribute('action');
                    output.push(indent + '[' + (role || tag.toLowerCase()) + landmarkAttrs + ']');
                    landmarkStack.push(tag);
                }

                if (isInteractive || isHeading) {
                    var indent2 = '  '.repeat(Math.max(depth, landmarkStack.length));
                    var name = getName(node);
                    var attrs = getAttrs(node);
                    var line = indent2 + (role || tag.toLowerCase());
                    if (name) line += ' "' + name + '"';
                    if (attrs) line += ' ' + attrs;
                    output.push(line);
                }

                var children = node.children;
                for (var i = 0; i < children.length; i++) {
                    walk(children[i], depth + 1);
                }

                if (isLandmark) {
                    landmarkStack.pop();
                }
            }

            var title = document.title;
            if (title) output.push('Page: ' + title.trim());
            var url = window.location.href;
            if (url) output.push('URL: ' + url);
            output.push('');

            walk(document.body, 0);
            return output.join('\\n');
        })();
        """;

    /**
     * Extracts an accessibility tree representation from the current page.
     *
     * <p>Uses JavaScript injection via {@link JavascriptExecutor}, which works on
     * all browsers (Chrome, Firefox, Safari, Edge) and Appium WebViews.</p>
     *
     * <p>For native Appium drivers that don't support JavaScript, returns {@code null}
     * so the caller can fall back to {@link DOMMinimizer}.</p>
     *
     * @param driver The WebDriver instance
     * @return Compact accessibility tree text, or null if extraction failed
     */
    public static String extractTree(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor jsExecutor)) {
            Reporter.log("AccessibilityTreeExtractor: Driver does not support JavascriptExecutor — skipping AX tree", LogLevel.DEBUG);
            return null;
        }

        try {
            Object result = jsExecutor.executeScript(AX_TREE_SCRIPT);
            if (result == null) {
                Reporter.log("AccessibilityTreeExtractor: Script returned null", LogLevel.WARN);
                return null;
            }

            String axTree = result.toString();

            // Safety cap
            if (axTree.length() > MAX_OUTPUT_LENGTH) {
                axTree = axTree.substring(0, MAX_OUTPUT_LENGTH) + "\n<!-- AX tree truncated at " + MAX_OUTPUT_LENGTH + " chars -->";
            }

            Reporter.log("AccessibilityTreeExtractor: Captured " + axTree.length() + " chars", LogLevel.INFO_BLUE);
            return axTree;

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null) {
                int nl = msg.indexOf('\n');
                if (nl > 0) msg = msg.substring(0, nl);
            }
            Reporter.log("AccessibilityTreeExtractor: JS extraction failed: " + msg
                    + " — falling back to DOMMinimizer", LogLevel.DEBUG);
            return null;
        }
    }
}
