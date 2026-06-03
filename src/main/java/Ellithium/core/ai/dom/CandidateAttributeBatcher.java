package Ellithium.core.ai.dom;

import Ellithium.core.ai.DriverProfile;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Single-round-trip batched attribute reader for the healing ensemble. Replaces N per-element
 * {@code getAttribute}/{@code isDisplayed}/{@code getText} WebDriver calls with ONE
 * {@code executeScript} over the whole candidate array (~12N HTTP round-trips → 1).
 *
 * <p>Returns {@code null} (the caller's signal to fall back to native per-element reads) when:
 * <ul>
 *   <li>the candidate list is empty,</li>
 *   <li>the driver is Appium in a NATIVE context — there is no DOM, so the DOM JS would throw;
 *       a native short-circuit avoids the wasted reflective call + exception, and</li>
 *   <li>the driver has no {@link JavascriptExecutor}, or the script call fails.</li>
 * </ul>
 */
public final class CandidateAttributeBatcher {

    private CandidateAttributeBatcher() {}

    private static final String BATCH_SCRIPT =
            "return arguments[0].map(function(el){"
            + " if(!el) return null;"
            + " function a(n){ return el.getAttribute(n); }"
            + " var r = el.getBoundingClientRect();"
            + " var cs = window.getComputedStyle ? getComputedStyle(el) : null;"
            + " var visible = !!(el.offsetParent !== null && r.width > 0 && r.height > 0"
            + "   && (!cs || (cs.visibility !== 'hidden' && cs.display !== 'none')));"
            + " var txt = (el.textContent || '').trim();"
            + " if (txt.length > 100) txt = txt.substring(0,100);"
            + " var allv=''; var at=el.attributes;"
            + " var dm={};"
            + " for(var k=0;k<at.length;k++){"
            + "  var av=at[k].value; if(av) allv+=' '+av;"
            + "  var an=at[k].name;"
            + "  if(an.indexOf('data-')===0 && an!=='data-ellithium-pick' && av) dm[an]=av;"
            + " }"
            + " var p=el.parentElement, pv=el.previousElementSibling, nx=el.nextElementSibling;"
            + " return {'id':a('id'),'name':a('name'),'class':a('class'),"
            + "  'parent-tag':p?p.tagName.toLowerCase():null,"
            + "  'child-index':p?Array.prototype.indexOf.call(p.children,el):-1,"
            + "  'prev-sib':pv?pv.tagName.toLowerCase():null,"
            + "  'next-sib':nx?nx.tagName.toLowerCase():null,"
            + "  'aria-label':a('aria-label'),'data-testid':a('data-testid'),'role':a('role'),"
            + "  'placeholder':a('placeholder'),'resource-id':a('resource-id'),"
            + "  'accessibility-id':a('accessibility-id'),"
            + "  'content-desc':a('content-desc'),'data-test':a('data-test'),"
            + "  'title':a('title'),'label':a('label'),"
            + "  'type':a('type'),"
            + "  'allattrs':allv.toLowerCase(),"
            + "  'dataAttrs':dm,"
            + "  'text':txt,"
            + "  'visible':visible,"
            + "  'tag':el.tagName?el.tagName.toLowerCase():null};"
            + "});";

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> fetch(WebDriver driver, List<WebElement> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (DriverProfile.detect(driver) == DriverProfile.MOBILE_NATIVE) return null;
        if (!(driver instanceof JavascriptExecutor)) return null;
        try {
            Object res = ((JavascriptExecutor) driver).executeScript(BATCH_SCRIPT, candidates);
            if (res instanceof List<?> rows) {
                List<Map<String, Object>> out = new ArrayList<>(rows.size());
                for (Object row : rows) out.add(row instanceof Map<?, ?> ? (Map<String, Object>) row : null);
                return out;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
