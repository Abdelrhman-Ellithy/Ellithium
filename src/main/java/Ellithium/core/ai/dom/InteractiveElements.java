package Ellithium.core.ai.dom;

import java.util.Locale;
import java.util.Set;

/**
 * Single source of truth for "what is an interactive element" and "what kind of action is this".
 *
 * <p>Shared by the interaction-recovery layer ({@code InteractionRecovery}) and the healing
 * orchestrator ({@code HealingOrchestrator}) so the interactive-element vocabulary and the
 * action-classification predicates are defined exactly once. Leaf class — depends only on the JDK.
 */
public final class InteractiveElements {

    private InteractiveElements() {
    }

    /** Tags that are natively interactive. */
    public static final Set<String> TAGS =
            Set.of("button", "a", "input", "select", "textarea", "option");

    /** ARIA roles that mark an element interactive. */
    public static final Set<String> ROLES =
            Set.of("button", "link", "menuitem", "menuitemcheckbox", "menuitemradio",
                    "tab", "option", "checkbox", "radio");

    /** CSS matching any interactive descendant — used to drill into a matched container. */
    public static final String DESCENDANT_CSS =
            "button, a, input, select, textarea, [role='button'], [role='link'], "
            + "[role='menuitem'], [role='tab'], [role='checkbox'], [role='radio']";

    /** XPath for the nearest interactive ancestor — used to drill up from a matched inner node. */
    public static final String ANCESTOR_XPATH =
            "./ancestor::*[self::a or self::button or self::input or @role='button' or @role='link'][1]";

    /** Priority-ordered inner interactive targets when a matched element is a container. */
    public static final String[] INNER_SELECTORS = {
            "button[type='submit']", "input[type='submit']", "button", "input[type='button']", "a"
    };

    /** Actions that semantically perform a click/tap/press/hover. */
    public static boolean isClickLikeAction(String actionType) {
        if (actionType == null || actionType.equals("unknown")) return false;
        String lower = actionType.toLowerCase(Locale.ROOT);
        return lower.contains("click") || lower.contains("tap")
                || lower.contains("press") || lower.contains("hover");
    }

    /** Plain native clicks eligible for a JS-click fallback (excludes double/right/hover/JS clicks). */
    public static boolean isPlainClick(String actionType) {
        return "clickOnElement".equals(actionType) || "clickOnMultipleElements".equals(actionType);
    }

    /** Actions that require an editable/input target (sendData, clearElement, setText, type, uploadFile). */
    public static boolean isTextInputAction(String actionType) {
        if (actionType == null || actionType.equals("unknown")) return false;
        String lower = actionType.toLowerCase(Locale.ROOT);
        if (lower.startsWith("get") || lower.startsWith("is") || lower.startsWith("wait") || lower.contains("read")) {
            return false;
        }
        return lower.contains("senddata") || lower.contains("sendkeys") || lower.contains("clear")
                || lower.contains("settext") || lower.contains("type")
                || lower.contains("upload") || lower.contains("fill");
    }

    /** Tags that accept text input natively. */
    public static final Set<String> TEXT_INPUT_TAGS =
            Set.of("input", "textarea");

    /** Roles that represent text inputs. */
    public static final Set<String> TEXT_INPUT_ROLES =
            Set.of("textbox", "searchbox", "combobox");

    /** Priority-ordered inner editable targets when a matched element is a container. */
    public static final String[] INNER_TEXT_SELECTORS = {
            "input:not([type='hidden']):not([type='submit']):not([type='button'])",
            "textarea",
            "[contenteditable='true']",
            "[role='textbox']",
            "[role='searchbox']"
    };
}