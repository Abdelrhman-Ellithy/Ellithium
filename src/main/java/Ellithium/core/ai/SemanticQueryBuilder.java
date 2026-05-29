package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.models.ElementFingerprint;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Builds the canonical query string for Tier 3 / Tier 4. */
public class SemanticQueryBuilder {

    private static final Pattern CAMEL_SPLIT =
            Pattern.compile("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|[-_]");

    private static final Pattern BY_VALUE = Pattern.compile("By\\.[a-zA-Z]+:\\s*(.*)");

    private static final Map<String, String> ACTION_EXPANSIONS = new LinkedHashMap<>();

    static {
        ACTION_EXPANSIONS.put("senddata",          "type input enter text");
        ACTION_EXPANSIONS.put("settext",            "type input enter text");
        ACTION_EXPANSIONS.put("clickonelement",     "click press button");
        ACTION_EXPANSIONS.put("click",              "click press button");
        ACTION_EXPANSIONS.put("tap",                "tap click press");
        ACTION_EXPANSIONS.put("doubletap",          "double tap click");
        ACTION_EXPANSIONS.put("doubleclick",        "double click press");
        ACTION_EXPANSIONS.put("rightclick",         "right click context menu");
        ACTION_EXPANSIONS.put("longpress",          "long press hold");
        ACTION_EXPANSIONS.put("selectdropdown",     "select dropdown option choose");
        ACTION_EXPANSIONS.put("selectdropdownby",   "select dropdown option choose");
        ACTION_EXPANSIONS.put("gettext",            "read text label value");
        ACTION_EXPANSIONS.put("getattributevalue",  "read attribute property");
        ACTION_EXPANSIONS.put("iselementdisplayed", "check visible display");
        ACTION_EXPANSIONS.put("iselementpresent",   "check exists present");
        ACTION_EXPANSIONS.put("iselementclickable", "check clickable enabled");
        ACTION_EXPANSIONS.put("iselementselected",  "check selected state");
        ACTION_EXPANSIONS.put("uploadfile",         "upload file input attach");
        ACTION_EXPANSIONS.put("uploadmultiplefiles","upload file input attach multiple");
        ACTION_EXPANSIONS.put("hoverelement",       "hover mouse over");
        ACTION_EXPANSIONS.put("hoveroverelement",   "hover mouse over");
        ACTION_EXPANSIONS.put("hoverandclick",      "hover mouse click");
        ACTION_EXPANSIONS.put("clearelement",       "clear input reset");
        ACTION_EXPANSIONS.put("movetoelement",      "scroll move element");
        ACTION_EXPANSIONS.put("scrollintoelement",  "scroll into view");
        ACTION_EXPANSIONS.put("dragandrop",         "drag drop move");
        ACTION_EXPANSIONS.put("draganddropbyoffset","drag drop offset");
        ACTION_EXPANSIONS.put("swipe",              "swipe scroll gesture");
        ACTION_EXPANSIONS.put("scroll",             "scroll swipe");
        ACTION_EXPANSIONS.put("movesliderto",       "slider range move");
        ACTION_EXPANSIONS.put("movesliderbyoffset", "slider range offset");
        ACTION_EXPANSIONS.put("javascriptclick",    "click javascript button");
        ACTION_EXPANSIONS.put("waitforvisibility",  "wait visible appear");
        ACTION_EXPANSIONS.put("waitforelement",     "wait element appear");
    }

    private static final String[] METHOD_PREFIXES_V2 = {
            "click", "tap", "doubletap", "press", "longpress", "submit", "send", "senddata", "sendkeys",
            "type", "fill", "enter", "set", "settext", "input", "get", "gettext", "getvalue", "read",
            "extract", "fetch", "verify", "check", "assert", "select", "choose", "pick", "upload",
            "download", "clear", "reset", "hover", "focus", "scroll", "swipe", "wait", "is",
            "isdisplayed", "isenabled", "isselected", "navigate", "goto", "go", "open", "close",
            "dismiss", "accept", "cancel", "search", "filter", "sort", "expand", "collapse", "toggle",
            "enable", "disable", "add", "remove", "delete", "edit", "save", "update", "create",
            "confirm", "view", "perform",
    };
    static {
        Arrays.sort(METHOD_PREFIXES_V2, Comparator.comparingInt(String::length).reversed());
    }

    public static String buildFromContext(String actionType, String locatorValue,
                                          String methodName, ElementFingerprint fingerprint) {
        String lastText        = fingerprint != null ? fingerprint.getText()        : null;
        String lastId          = fingerprint != null ? fingerprint.getId()          : null;
        String lastAriaLabel   = fingerprint != null ? fingerprint.getAriaLabel()   : null;
        String lastPlaceholder = fingerprint != null ? fingerprint.getPlaceholder() : null;
        String lastDataTestId  = fingerprint != null ? fingerprint.getDataTestId()  : null;
        String lastTagName     = fingerprint != null ? fingerprint.getTagName()     : null;

        boolean isReadable = SemanticLocatorResolver.ElementCategory.READABLE
                == SemanticLocatorResolver.categorizeAction(actionType);
        if (isReadable) lastText = null;

        return build(actionType, locatorValue, methodName, null,
                lastText, lastId, lastAriaLabel, lastPlaceholder, lastDataTestId, lastTagName);
    }

    public static String build(String actionType, String locatorValue, String methodName,
                                String hint,
                                String lastText, String lastId, String lastAriaLabel,
                                String lastPlaceholder, String lastDataTestId, String lastTagName) {
        List<String> tokens = new ArrayList<>();

        if (isPresent(actionType)) {
            tokens.add(expandAction(actionType));
        }

        if (isPresent(locatorValue)) {
            tokens.add(deCamelCase(extractLocatorValue(locatorValue)));
        }

        if (isPresent(methodName)) {
            String stripped = deCamelCase(stripMethodPrefixV2(methodName));
            if (!stripped.isBlank()) {
                tokens.add(stripped);
            }
        }

        if (isPresent(hint)) {
            tokens.add(hint.toLowerCase());
        }

        if (isPresent(lastText)) {
            tokens.add(lastText.substring(0, Math.min(100, lastText.length())).toLowerCase());
        }

        if (isPresent(lastId)) {
            tokens.add(deCamelCase(lastId));
        }

        if (isPresent(lastAriaLabel)) {
            tokens.add(lastAriaLabel.toLowerCase());
        }

        if (isPresent(lastPlaceholder)) {
            tokens.add(lastPlaceholder.toLowerCase());
        }

        if (isPresent(lastDataTestId)) {
            tokens.add(deCamelCase(lastDataTestId));
        }

        if (isPresent(lastTagName)) {
            tokens.add(lastTagName.toLowerCase());
        }

        String joined = tokens.stream()
                .filter(SemanticQueryBuilder::isPresent)
                .collect(Collectors.joining(" "))
                .trim();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String tok : joined.split("\\s+")) {
            if (!tok.isEmpty()) seen.add(tok);
        }
        return String.join(" ", seen);
    }

    static String stripMethodPrefixV2(String name) {
        if (name == null) return "";
        String low = name.toLowerCase();
        for (String prefix : METHOD_PREFIXES_V2) {
            if (low.startsWith(prefix) && name.length() > prefix.length()
                    && Character.isUpperCase(name.charAt(prefix.length()))) {
                return name.substring(prefix.length());
            }
        }
        return name;
    }

    public static String deCamelCase(String identifier) {
        if (identifier == null || identifier.isBlank()) return "";
        String[] parts = CAMEL_SPLIT.split(identifier);
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(p -> !p.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.joining(" "));
    }

    public static String expandAction(String actionType) {
        if (actionType == null || actionType.isBlank()) return "";
        switch (SemanticLocatorResolver.categorizeAction(actionType)) {
            case READABLE:  return "read text label value";
            case INPUT:     return "type input enter text";
            case CLICKABLE: return "click press button";
            case SELECT:    return "select dropdown option choose";
            default:        break;
        }
        String key = actionType.toLowerCase().replace("-", "").replace("_", "");
        if (ACTION_EXPANSIONS.containsKey(key)) {
            return ACTION_EXPANSIONS.get(key);
        }
        for (Map.Entry<String, String> entry : ACTION_EXPANSIONS.entrySet()) {
            if (key.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return deCamelCase(actionType);
    }

    public static String extractLocatorValue(String locatorOrValue) {
        if (locatorOrValue == null) return "";
        java.util.regex.Matcher m = BY_VALUE.matcher(locatorOrValue.trim());
        return m.find() ? m.group(1).trim() : locatorOrValue.trim();
    }

    private static boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }
}
