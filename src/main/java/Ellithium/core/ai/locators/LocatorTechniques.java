package Ellithium.core.ai.locators;

import java.util.regex.Pattern;

/**
 * Neutral, shared locator-construction primitives used by both the recorder/codegen
 * ({@code UniqueLocatorGenerator}) and the self-healing locator builder
 * ({@code HealedLocatorBuilder}). Depends on neither module — both depend on this.
 */
public final class LocatorTechniques {

    private LocatorTechniques() {}

    private static final Pattern DYNAMIC = Pattern.compile(
            "\\d{4,}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}|_\\d+$|:id/|[0-9a-f]{16,}");

    /** True when a value looks runtime-generated (long digit runs, UUID/hash, trailing _n, framework id). */
    public static boolean looksDynamic(String v) {
        if (v == null || v.isBlank()) return false;
        if (v.trim().matches("\\d+")) return true;
        return DYNAMIC.matcher(v).find();
    }

    /** True when the token is a valid bare CSS class identifier (no :, /, ., spaces, etc.). */
    public static boolean isCssSafeIdentifier(String token) {
        return token != null && !token.isBlank() && token.matches("^-?[_a-zA-Z][_a-zA-Z0-9-]*$");
    }

    /**
     * JS function (declaration only) that computes an ancestor-scoped CSS path with
     * {@code :nth-of-type} disambiguation: nearest stable ancestor ({@code #id} or unique
     * {@code [data-testid]}) + a {@code >}-joined chain of tag / {@code tag:nth-of-type(k)} segments.
     * Defines {@code __ellPath(el)} → css string (or empty). Defined ONCE; embedded by both the
     * standalone {@link #STRUCTURAL_PATH_SCRIPT} and the healing builder's validation script.
     */
    public static final String STRUCTURAL_PATH_FN =
            "function __ellPath(el){"
            + "function seg(n){var t=n.tagName.toLowerCase();"
            + " var p=n.parentElement; if(!p) return t;"
            + " var same=Array.prototype.filter.call(p.children,function(c){return c.tagName===n.tagName;});"
            + " if(same.length===1) return t;"
            + " return t+':nth-of-type('+(Array.prototype.indexOf.call(same,n)+1)+')';}"
            + "function ancestor(n){var c=n;while(c&&c!==document.body){"
            + " if(c.id&&document.querySelectorAll('#'+CSS.escape(c.id)).length===1) return c;"
            + " var dt=c.getAttribute&&c.getAttribute('data-testid');"
            + " if(dt&&document.querySelectorAll('[data-testid=\"'+dt+'\"]').length===1) return c;"
            + " c=c.parentElement;} return null;}"
            + "var root=ancestor(el), parts=[], cur=el, base='';"
            + "if(root&&root!==el){ if(root.id){base='#'+CSS.escape(root.id);}"
            + " else {base='[data-testid=\"'+root.getAttribute('data-testid')+'\"]';} }"
            + "while(cur&&cur!==document.body&&cur!==root){parts.unshift(seg(cur));cur=cur.parentElement;}"
            + "return (base?base+' ':'')+parts.join(' > ');}";

    /**
     * Standalone script: computes the ancestor-scoped nth-of-type CSS path for {@code arguments[0]}
     * and returns {@code {css, xpath}} ({@code xpath} null). Behaviorally identical to the original
     * inline script — now expressed via {@link #STRUCTURAL_PATH_FN}.
     */
    public static final String STRUCTURAL_PATH_SCRIPT =
            STRUCTURAL_PATH_FN + " return {'css': __ellPath(arguments[0]), 'xpath': null};";
}
