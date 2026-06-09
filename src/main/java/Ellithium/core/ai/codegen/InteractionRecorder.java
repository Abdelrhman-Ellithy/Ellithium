package Ellithium.core.ai.codegen;

import Ellithium.core.ai.healing.BaselineStore;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionRecorder {

    private InteractionRecorder() {}

    private static final Gson GSON = new Gson();
    private static final long POLL_MS = 250L;
    private static final long CODE_PREVIEW_DEBOUNCE_MS = 1_000L;

    private static volatile boolean recording = false;
    private static volatile WebDriver driver = null;
    static volatile RecorderOptions options = RecorderOptions.defaults();
    private static volatile Thread drainThread = null;
    private static volatile String lastUrl = null;
    static volatile String startUrl = null;
    private static volatile long navHintEpoch = 0L;
    private static final java.util.Set<String> knownHandles =
            java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());
    private static final java.util.List<String> urlHistory =
            java.util.Collections.synchronizedList(new ArrayList<>());
    private static volatile int urlIndex = -1;
    private static final long NAV_HINT_TTL = 10_000L;

    static final List<RecordedStep> STEPS = Collections.synchronizedList(new ArrayList<>());
    static final Map<String, RecordedStep> BY_ID = new ConcurrentHashMap<>();
    static volatile List<LocatorCandidate> lastPicked = List.of();

    static volatile long lastCodeRenderMs = 0L;
    static volatile String cachedCodePreview = "";

    public static synchronized void start(WebDriver d, RecorderOptions opts) {
        start(d, opts, null);
    }

    public static synchronized void start(WebDriver d, RecorderOptions opts, String explicitStartUrl) {
        if (recording) return;
        driver = d;
        options = opts != null ? opts : RecorderOptions.defaults();
        STEPS.clear();
        BY_ID.clear();
        lastPicked = List.of();
        lastCodeRenderMs = 0L;
        cachedCodePreview = "";
        recording = true;
        lastUrl = currentUrl();
        startUrl = (explicitStartUrl != null && !explicitStartUrl.isBlank()) ? explicitStartUrl : lastUrl;
        navHintEpoch = 0L;
        knownHandles.clear();
        try { knownHandles.addAll(d.getWindowHandles()); } catch (Exception ignored) {}
        urlHistory.clear();
        if (!isBlankUrl(startUrl)) { urlHistory.add(startUrl); urlIndex = 0; } else { urlIndex = -1; }
        if (Ellithium.core.ai.DriverProfile.detect(d).isNativeMobile()) {
            Reporter.log("InteractionRecorder: native mobile has no DOM — manual capture/overlay unavailable; "
                    + "use UniqueLocatorGenerator on a resolved element instead", LogLevel.WARN);
        }
        clearLog();
        drainThread = Thread.ofVirtual().name("ellithium-codegen-recorder").start(InteractionRecorder::drainLoop);
        Reporter.log("InteractionRecorder: recording started", LogLevel.INFO_YELLOW);
    }

    public static synchronized List<RecordedStep> stop() {
        recording = false;
        Thread t = drainThread;
        if (t != null) {
            t.interrupt();
            try { t.join(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        drainThread = null;
        try { drainOnce(); } catch (Exception ignored) {}
        removeOverlay();
        List<RecordedStep> snapshot = new ArrayList<>(STEPS);
        Reporter.log("InteractionRecorder: recording stopped — " + snapshot.size() + " steps", LogLevel.INFO_GREEN);
        driver = null;
        return snapshot;
    }

    public static boolean isRecording() { return recording; }

    public static List<RecordedStep> getSteps() { return new ArrayList<>(STEPS); }

    public static RecorderOptions getOptions() { return options; }

    public static String getStartUrl() { return startUrl; }

    private static void drainLoop() {
        while (recording) {
            try {
                if (!driverAlive()) { recording = false; break; }
                boolean freshInject = ensureInjected();
                boolean changed = drainOnce();
                reinjectOnNavigation();
                checkNewTabs();
                if (stopRequested()) { recording = false; break; }
                if (changed || freshInject) render();
            } catch (Exception ignored) {}
            sleep(POLL_MS);
        }
    }

    public static WebDriver getRecorderDriver() {
        return driverAlive() ? driver : null;
    }

    private static boolean driverAlive() {
        WebDriver d = driver;
        if (d == null) return false;
        try {
            return !d.getWindowHandles().isEmpty();
        } catch (Exception e) {
            if (e.getClass().getSimpleName().contains("UnhandledAlert")) return true;
            try { return !d.getWindowHandles().isEmpty(); } catch (Exception e2) { return false; }
        }
    }

    private static boolean drainOnce() {
        boolean changed = false;
        for (Map<String, Object> ev : readLog()) {
            if (processEvent(ev)) changed = true;
        }
        return changed;
    }

    private static List<Map<String, Object>> readLog() {
        if (!(driver instanceof JavascriptExecutor js)) return List.of();
        try {
            Object res = js.executeScript(
                    "var a=localStorage.getItem('__ellRecLog')||'[]'; localStorage.setItem('__ellRecLog','[]'); return a;");
            if (res == null) return List.of();
            List<Map<String, Object>> out =
                    GSON.fromJson(res.toString(), new TypeToken<List<Map<String, Object>>>() {}.getType());
            return out != null ? out : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    static boolean processEvent(Map<String, Object> ev) {
        String type = str(ev.get("type"));
        if (type == null) return false;
        if ("override".equals(type)) {
            RecordedStep step = BY_ID.get(str(ev.get("id")));
            if (step != null) { step.choose((int) asLong(ev.get("index"))); return true; }
            return false;
        }
        if ("clearAll".equals(type)) {
            STEPS.clear(); BY_ID.clear(); lastPicked = List.of();
            cachedCodePreview = ""; lastCodeRenderMs = 0L;
            return true;
        }
        if ("assertModeToggle".equals(type)) {
            options = options.withAssertMode(options.isSoftAssert() ? "hard" : "soft");
            return true;
        }
        if ("autoGenerate".equals(type)) {
            String stepId = str(ev.get("id"));
            if (stepId == null) return false;
            RecordedStep step = BY_ID.get(stepId);
            if (step == null) return false;
            Object m = ev.get("method");
            step.setGeneratorMethod((m == null || "null".equals(m.toString()) || m.toString().isBlank()) ? null : m.toString());
            return true;
        }
        if ("delete".equals(type)) {
            String delId = str(ev.get("id"));
            if (delId == null) return false;
            BY_ID.remove(delId);
            synchronized (STEPS) {
                for (int i = STEPS.size() - 1; i >= 0; i--) {
                    if (delId.equals(STEPS.get(i).getId())) {
                        STEPS.remove(i);
                        return true;
                    }
                }
            }
            return false;
        }
        String id = str(ev.get("id"));
        if (id == null) return false;
        List<LocatorCandidate> candidates = buildCandidates(ev.get("candidates"));
        if ("inspect".equals(type)) {
            lastPicked = candidates;
            return true;
        }
        if ("doubleClick".equals(type)) {
            dropTrailingClicks(candidates.isEmpty() ? null : candidates.get(0).javaExpression());
        }
        List<Integer> frame = frameChainOf(ev.get("frame"));
        List<LocatorCandidate> targetCandidates = buildCandidates(ev.get("targetCandidates"));
        RecordedStep step = new RecordedStep(id, type, str(ev.get("value")),
                str(ev.get("tag")), str(ev.get("name")), candidates, frame, targetCandidates);
        if ("assertValue".equals(type)) {
            String aa = str(ev.get("assertAttr"));
            if (aa != null && !aa.isBlank()) step.setAssertAttr(aa);
        }
        if (frame.isEmpty()) seedOne(candidates);
        STEPS.add(step);
        BY_ID.put(id, step);
        if ("click".equals(type) || "select".equals(type) || "input".equals(type)
                || "pressEnter".equals(type) || "doubleClick".equals(type)
                || "dragAndDrop".equals(type)) {
            navHintEpoch = System.currentTimeMillis();
        }
        return true;
    }

    private static void dropTrailingClicks(String expr) {
        if (expr == null) return;
        synchronized (STEPS) {
            int removed = 0;
            for (int i = STEPS.size() - 1; i >= 0 && removed < 2; i--) {
                RecordedStep s = STEPS.get(i);
                if ("click".equals(s.getActionType()) && s.chosen() != null
                        && expr.equals(s.chosen().javaExpression())) {
                    STEPS.remove(i);
                    BY_ID.remove(s.getId());
                    removed++;
                } else {
                    break;
                }
            }
        }
    }

    private static List<LocatorCandidate> buildCandidates(Object raw) {
        List<LocatorCandidate> out = new ArrayList<>();
        if (!(raw instanceof List<?> rows)) return out;
        for (Object o : rows) {
            if (!(o instanceof Map<?, ?> m)) continue;
            out.add(UniqueLocatorGenerator.fromCapture(
                    str(m.get("type")), str(m.get("sel")), str(m.get("value")),
                    str(m.get("tier")), asBool(m.get("unique")), asBool(m.get("param"))));
        }
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        return out;
    }

    private static void seedOne(List<LocatorCandidate> candidates) {
        if (candidates.isEmpty()) return;
        try {
            LocatorCandidate c = candidates.get(0);
            List<WebElement> els = driver.findElements(c.by());
            if (els.size() == 1) BaselineStore.capture(driver, c.by(), els.get(0));
        } catch (Exception ignored) {}
    }

    private static void reinjectOnNavigation() {
        String url = currentUrl();
        if (url == null || url.equals(lastUrl)) return;
        boolean firstReal = isBlankUrl(startUrl) && !isBlankUrl(url);
        boolean actionInduced = (System.currentTimeMillis() - navHintEpoch) <= NAV_HINT_TTL;
        boolean paused = isPaused();
        long ts = System.currentTimeMillis();
        lastUrl = url;
        if (firstReal) {
            startUrl = url;
            urlHistory.clear();
            urlHistory.add(url);
            urlIndex = 0;
        } else {
            synchronized (urlHistory) {
                if (urlIndex > 0 && url.equals(urlHistory.get(urlIndex - 1))) {
                    urlIndex--;
                    if (!paused) STEPS.add(new RecordedStep("back-" + ts, "navigateBack", null, null, null, List.of()));
                } else if (urlIndex >= 0 && urlIndex < urlHistory.size() - 1
                        && url.equals(urlHistory.get(urlIndex + 1))) {
                    urlIndex++;
                    if (!paused) STEPS.add(new RecordedStep("fwd-" + ts, "navigateForward", null, null, null, List.of()));
                } else {
                    while (urlHistory.size() > urlIndex + 1) urlHistory.remove(urlHistory.size() - 1);
                    urlHistory.add(url);
                    urlIndex = urlHistory.size() - 1;
                    boolean autoNav = isProgNav() || (STEPS.isEmpty() && navHintEpoch == 0L);
                    if (!actionInduced && !paused && !autoNav) {
                        STEPS.add(new RecordedStep("nav-" + ts, "navigate", url, null, null, List.of()));
                    }
                }
            }
        }
        navHintEpoch = 0L;
        ensureInjected();
        render();
    }

    private static boolean isPaused() {
        if (!(driver instanceof JavascriptExecutor js)) return false;
        try {
            Object v = js.executeScript(
                    "try{return localStorage.getItem('__ellPaused')==='1';}catch(e){return false;}");
            return Boolean.TRUE.equals(v);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isProgNav() {
        if (!(driver instanceof JavascriptExecutor js)) return false;
        try {
            Object v = js.executeScript(
                    "var t=window.__ellProgNavTs||0; window.__ellProgNavTs=0;"
                    + "return t>0&&(Date.now()-t)<3000;");
            return Boolean.TRUE.equals(v);
        } catch (Exception e) { return false; }
    }

    private static boolean isBlankUrl(String u) {
        return u == null || u.isBlank() || u.startsWith("about:") || u.startsWith("data:") || u.startsWith("chrome:");
    }

    private static boolean isInternalUrl(String u) {
        if (u == null || u.isBlank()) return true;
        return u.startsWith("chrome-devtools://") || u.startsWith("devtools://")
                || u.startsWith("chrome://") || u.startsWith("edge://")
                || u.startsWith("about:") || u.startsWith("data:");
    }

    private static void checkNewTabs() {
        if (driver == null) return;
        try {
            java.util.Set<String> handles = driver.getWindowHandles();
            String genuinelyNew = null;
            for (String h : handles) {
                if (!knownHandles.contains(h)) { genuinelyNew = h; break; }
            }
            if (genuinelyNew != null) {
                knownHandles.clear();
                knownHandles.addAll(handles);
                driver.switchTo().window(genuinelyNew);
                lastUrl = currentUrl();
                if (isInternalUrl(lastUrl)) return;
                navHintEpoch = 0L;
                STEPS.add(new RecordedStep("tab-" + System.currentTimeMillis(), "navigate",
                        lastUrl, null, null, List.of()));
                ensureInjected();
                render();
            } else if (handles.size() < knownHandles.size()) {
                knownHandles.clear();
                knownHandles.addAll(handles);
                try {
                    driver.getCurrentUrl();
                } catch (Exception currentWindowClosed) {
                    String survivor = handles.isEmpty() ? null : handles.iterator().next();
                    if (survivor != null) {
                        driver.switchTo().window(survivor);
                        lastUrl = currentUrl();
                        navHintEpoch = 0L;
                        ensureInjected();
                        render();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static boolean stopRequested() {
        if (!(driver instanceof JavascriptExecutor js)) return false;
        try {
            return Boolean.TRUE.equals(js.executeScript("return window.__ellStop === true;"));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean ensureInjected() {
        if (!(driver instanceof JavascriptExecutor js)) return false;
        try {
            js.executeScript(CAPTURE_SCRIPT, options.pickModeDefault());
            return Boolean.TRUE.equals(js.executeScript(OVERLAY_SCRIPT));
        } catch (Exception ignored) {}
        return false;
    }

    private static void clearLog() {
        if (!(driver instanceof JavascriptExecutor js)) return;
        try { js.executeScript("localStorage.setItem('__ellRecLog','[]'); localStorage.setItem('__ellPaused','0');"); } catch (Exception ignored) {}
    }

    private static void render() {
        if (!(driver instanceof JavascriptExecutor js)) return;
        try { js.executeScript(RENDER_SCRIPT, renderJson()); } catch (Exception ignored) {}
    }

    private static void removeOverlay() {
        if (!(driver instanceof JavascriptExecutor js)) return;
        try {
            js.executeScript("var b=document.getElementById('ellithium-recorder-toolbar');if(b)b.remove(); window.__ellOverlayDone=false;");
        } catch (Exception ignored) {}
    }

    static String renderJson() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        synchronized (STEPS) {
            for (RecordedStep s : STEPS) {
                JsonObject o = new JsonObject();
                o.addProperty("id", s.getId());
                o.addProperty("action", s.getActionType());
                o.addProperty("data", s.getData());
                String gm = s.getGeneratorMethod();
                o.addProperty("generatorMethod", gm != null ? gm : "");
                String aa = s.getAssertAttr();
                o.addProperty("assertAttr", aa != null ? aa : "");
                o.addProperty("chosenIndex", s.getChosenIndex());
                JsonArray fr = new JsonArray();
                for (Integer idx : s.getFrameChain()) fr.add(idx);
                o.add("frame", fr);
                o.add("candidates", candidatesJson(s.getCandidates()));
                if (!s.getTargetCandidates().isEmpty())
                    o.add("targetCandidates", candidatesJson(s.getTargetCandidates()));
                arr.add(o);
            }
        }
        root.add("steps", arr);
        List<LocatorCandidate> picked = lastPicked;
        if (!picked.isEmpty()) {
            JsonObject p = new JsonObject();
            p.add("candidates", candidatesJson(picked));
            root.add("picked", p);
        }
        root.addProperty("assertMode", options.assertMode());
        long now = System.currentTimeMillis();
        if (now - lastCodeRenderMs >= CODE_PREVIEW_DEBOUNCE_MS) {
            try {
                List<RecordedStep> snap = new ArrayList<>(STEPS);
                boolean soft = options.isSoftAssert();
                cachedCodePreview = options.isTest()
                        ? PomCodeEmitter.previewTestSource(snap, CodegenCli.deriveClassName(startUrl),
                            options.packageName(), startUrl, options.browser(), soft)
                        : PomCodeEmitter.previewSource(snap, "RecordedPage", options.packageName(), soft);
                lastCodeRenderMs = now;
            } catch (Exception ignored) {}
        }
        root.addProperty("code", cachedCodePreview);
        return GSON.toJson(root);
    }

    private static JsonArray candidatesJson(List<LocatorCandidate> candidates) {
        JsonArray cands = new JsonArray();
        for (LocatorCandidate c : candidates) {
            JsonObject cj = new JsonObject();
            cj.addProperty("expr", c.javaExpression());
            cj.addProperty("tier", c.tier());
            cj.addProperty("unique", c.unique());
            cj.addProperty("param", c.parameterizable());
            cands.add(cj);
        }
        return cands;
    }

    private static List<Integer> frameChainOf(Object o) {
        if (!(o instanceof List<?> l)) return List.of();
        List<Integer> out = new ArrayList<>(l.size());
        for (Object x : l) if (x instanceof Number n) out.add(n.intValue());
        return out;
    }

    private static String currentUrl() {
        try { return driver != null ? driver.getCurrentUrl() : null; } catch (Exception e) { return null; }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String str(Object o) { return o != null ? o.toString() : null; }
    private static boolean asBool(Object o) { return Boolean.TRUE.equals(o) || "true".equals(String.valueOf(o)); }

    private static long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try { return (long) Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return -1L; }
    }

    static void resetForTest() {
        STEPS.clear();
        BY_ID.clear();
        lastPicked = List.of();
        options = RecorderOptions.defaults();
        startUrl = null;
        lastUrl = null;
        navHintEpoch = 0L;
        urlHistory.clear();
        urlIndex = -1;
        lastCodeRenderMs = 0L;
        cachedCodePreview = "";
        recording = false;
        driver = null;
    }

    static final String CAPTURE_SCRIPT =
            "(function(pick){"
            + " var W=window;"
            + " if(!W.__ellRecInit){ W.__ellRecInit=true; W.__ellMode=pick?'pick':'record'; W.__ellStop=false;"
            + "   W.__ellPaused=(function(){try{return localStorage.getItem('__ellPaused')==='1';}catch(e){return false;}})();"
            + "   W.__ellLastVal=new WeakMap(); W.__ellProgNavTs=0; W.__ellLastClickTs=W.__ellLastClickTs||0;"
            + "   (function(){"
            + "     function markProg(){ if((Date.now()-W.__ellLastClickTs)>600) W.__ellProgNavTs=Date.now(); }"
            + "     try{ var _ph=history.pushState.bind(history),_rh=history.replaceState.bind(history);"
            + "       history.pushState=function(){markProg();return _ph.apply(history,arguments);};"
            + "       history.replaceState=function(){markProg();return _rh.apply(history,arguments);};"
            + "     }catch(e){}"
            + "     try{ var _ld=Object.getOwnPropertyDescriptor(Location.prototype,'href');"
            + "       if(_ld&&_ld.set) Object.defineProperty(Location.prototype,'href',{get:_ld.get,"
            + "         set:function(u){markProg();_ld.set.call(this,u);},configurable:true});"
            + "     }catch(e){}"
            + "   })();"
            + " }"
            + " function LOG(){ try{return JSON.parse(localStorage.getItem('__ellRecLog')||'[]');}catch(e){return [];} }"
            + " function SAVE(a){ try{localStorage.setItem('__ellRecLog',JSON.stringify(a));}catch(e){} }"
            + " function emit(r){ var a=LOG(); a.push(r); SAVE(a); }"
            + " function nid(){ return Date.now().toString(36)+Math.random().toString(36).slice(2,7); }"
            + " function lit(s){ s=''+s; if(s.indexOf(\"'\")<0) return \"'\"+s+\"'\"; if(s.indexOf('\"')<0) return '\"'+s+'\"';"
            + "   return \"concat('\"+s.split(\"'\").join(\"',\\\"'\\\",'\")+\"')\"; }"
            + " function uCss(d,sel){ try{return d.querySelectorAll(sel).length===1;}catch(e){return false;} }"
            + " function uXp(d,xp){ try{return d.evaluate('count('+xp+')',d,null,XPathResult.NUMBER_TYPE,null).numberValue===1;}catch(e){return false;} }"
            + " function dyn(v){ v=''+v; return /(\\d{4,})|(_\\d+$)|(^\\d+$)|([0-9a-fA-F]{8}-)/.test(v); }"
            + " function asel(a,v){ return '['+a+\"='\"+(''+v).replace(/'/g,\"\\\\'\")+\"']\"; }"
            + " function cpath(el){ var p=[],c=el; while(c&&c.nodeType===1&&c!==document.body){ var t=c.tagName.toLowerCase();"
            + "   var par=c.parentElement; if(!par){p.unshift(t);break;} var same=Array.prototype.filter.call(par.children,function(x){return x.tagName===c.tagName;});"
            + "   if(same.length>1) t+=':nth-of-type('+(Array.prototype.indexOf.call(same,c)+1)+')'; p.unshift(t); c=par; if(p.length>6)break; } return p.join(' > '); }"
            + " function uCls(d,c){ try{return d.getElementsByClassName(c).length===1;}catch(e){return false;} }"
            + " function meaningful(el){ var c=el,n=0; while(c&&n<5){ var t=(c.tagName||'').toLowerCase();"
            + "   var r=c.getAttribute&&c.getAttribute('role');"
            + "   if(c.id||(c.getAttribute&&(c.getAttribute('data-testid')||c.getAttribute('name')||c.getAttribute('aria-label')))||(r&&r!=='presentation'&&r!=='none'&&r!=='generic')||['a','button','input','select','textarea','summary','label'].indexOf(t)>=0) return c;"
            + "   c=c.parentElement; n++; } return el; }"
            + " function isInteractable(el){ if(!el)return false;"
            + "   try{ var cs=window.getComputedStyle(el);"
            + "     if(cs.pointerEvents==='none'||cs.visibility==='hidden'||cs.display==='none'||cs.opacity==='0') return false; }catch(x){}"
            + "   return !!(el.offsetWidth>0||el.offsetHeight>0||el.getClientRects().length>0); }"
            + " function stableClassOf(el){ var c=el.getAttribute&&el.getAttribute('class'); if(!c)return null; var t=c.trim().split(/\\s+/); for(var i=0;i<t.length;i++){ if(t[i]&&!dyn(t[i])) return t[i]; } return null; }"
            + " function xpIndexOf(d,xp,el){ try{ var r=d.evaluate(xp,d,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null); for(var i=0;i<r.snapshotLength;i++){ if(r.snapshotItem(i)===el) return i+1; } }catch(e){} return 0; }"
            + " function attrW(nm){"
            + "   var TD=['data-testid','data-test','data-cy','data-qa']; if(TD.indexOf(nm)>=0) return 1.0;"
            + "   if(nm==='id') return 0.9; if(nm==='name') return 0.85;"
            + "   if(nm==='aria-label'||nm==='aria-labelledby') return 0.80;"
            + "   if(nm==='role') return 0.78;"
            + "   if(nm.indexOf('data-')===0) return 0.70;"
            + "   if(nm==='href'||nm==='src'||nm==='alt') return 0.68;"
            + "   if(nm==='type'||nm==='value'||nm==='placeholder'||nm==='title') return 0.62;"
            + "   return 0.50; }"
            + " function cands(el){"
            + "   var d=el.ownerDocument,o=[],tgn=(el.tagName||'').toLowerCase(),tx=(el.textContent||'').trim();"
            + "   function add(t,s,v,tier,u,p){ o.push({type:t,sel:s,value:v,tier:tier,unique:u,param:p}); }"
            + "   var SKIP=['style','class','data-ellithium-pick','onclick','onmousedown','onmouseover','onfocus','onblur','tabindex','colspan','rowspan'];"
            + "   var TOP_DATA=['data-testid','data-test','data-cy','data-qa'];"
            + "   var attrs=[];"
            + "   if(el.attributes){ for(var ai=0;ai<el.attributes.length;ai++){"
            + "     var an=el.attributes[ai].name,av=el.attributes[ai].value;"
            + "     if(!av||SKIP.indexOf(an)>=0||an.indexOf('on')===0) continue;"
            + "     if(av.length>150||av.indexOf('data:')===0||av.indexOf('blob:')===0) continue;"
            + "     if(an==='role'&&(av==='presentation'||av==='none'||av==='generic'||av==='group')) continue;"
            + "     attrs.push({name:an,value:av,w:attrW(an),dyn:dyn(av)});"
            + "   } attrs.sort(function(a,b){return b.w-a.w;}); }"
            + "   for(var ai=0;ai<attrs.length;ai++){"
            + "     var an=attrs[ai].name,av=attrs[ai].value,isDyn=attrs[ai].dyn;"
            + "     if(an==='id'&&av){ var uid=uCss(d,'#'+av); add('id','#'+av,av,'id',uid,isDyn); if(uid) continue; }"
            + "     var s1=asel(an,av);"
            + "     var u1=uCss(d,s1); if(u1){ add('css',s1,av,TOP_DATA.indexOf(an)>=0?an:'attr-css',true,isDyn); continue; }"
            + "     if(tgn){ var s1t=tgn+s1; if(uCss(d,s1t)){ add('css',s1t,av,'tag-attr-css',true,isDyn); continue; } }"
            + "     for(var aj=ai+1;aj<Math.min(ai+4,attrs.length);aj++){"
            + "       var bn=attrs[aj].name,bv=attrs[aj].value,bd=attrs[aj].dyn;"
            + "       var s2=s1+asel(bn,bv); if(uCss(d,s2)){ add('css',s2,av,'combo-css',true,isDyn||bd); break; }"
            + "       if(tgn){ var s2t=tgn+s2; if(uCss(d,s2t)){ add('css',s2t,av,'combo-css',true,isDyn||bd); break; } }"
            + "     }"
            + "   }"
            + "   var stClasses=[]; var elCl=el.getAttribute&&el.getAttribute('class');"
            + "   if(elCl){ elCl.trim().split(/\\s+/).forEach(function(c){ if(c&&!dyn(c)&&c.length>1) stClasses.push(c); }); }"
            + "   if(stClasses.length>0){ var mcc='',mcFound=false; for(var mci=0;mci<stClasses.length&&!mcFound;mci++){"
            + "     mcc+='.'+stClasses[mci];"
            + "     if(uCss(d,mcc)){ add('css',mcc,mcc,'class-css',true,false); mcFound=true; }"
            + "     else if(tgn&&uCss(d,tgn+mcc)){ add('css',tgn+mcc,tgn+mcc,'tag-class-css',true,false); mcFound=true; } } }"
            + "   if(tx&&tx.length>=2&&tx.length<=80){"
            + "     var role=el.getAttribute&&el.getAttribute('role');"
            + "     if(role){ var xr=\"//*[@role='\"+role+\"' and normalize-space(.)=\"+lit(tx)+\"]\"; if(uXp(d,xr)) add('xpath',xr,tx,'role-text',true,false); }"
            + "     var TT=['a','button','label','summary','span','div','li','td','th','p','h1','h2','h3','h4','option','dt','dd'];"
            + "     if(TT.indexOf(tgn)>=0){"
            + "       var xt='//'+tgn+'[normalize-space(.)='+lit(tx)+']';"
            + "       if(uXp(d,xt)){ add('xpath',xt,tx,'text',true,false); }"
            + "       else{"
            + "         var sc=stableClassOf(el); if(sc){ var xc=xt+'[contains(concat(\" \",normalize-space(@class),\" \"),concat(\" \",'+lit(sc)+',\" \"))]'; if(uXp(d,xc)) add('xpath',xc,tx,'text-class',true,false); }"
            + "         var taFound=false; for(var ak=0;ak<Math.min(5,attrs.length);ak++){"
            + "           var an4=attrs[ak].name,av4=attrs[ak].value;"
            + "           if(an4==='class'||an4==='style'||an4==='id') continue;"
            + "           var xta=xt+'[@'+an4+'='+lit(av4)+']';"
            + "           if(uXp(d,xta)){ add('xpath',xta,tx,'text-attr',true,false); taFound=true; break; } }"
            + "         var ix=xpIndexOf(d,xt,el); if(ix>0){ var xi='('+xt+')['+ix+']'; add('xpath',xi,tx,'xpath-indexed',uXp(d,xi),false); }"
            + "         var xct='//'+tgn+'[contains(.,'+lit(tx)+')]';"
            + "         if(!taFound){"
            + "           if(uXp(d,xct)){ add('xpath',xct,tx,'text-contains',true,false); }"
            + "           else{ var ix2=xpIndexOf(d,xct,el); if(ix2>0){ var xi2='('+xct+')['+ix2+']'; add('xpath',xi2,tx,'text-contains-indexed',uXp(d,xi2),false); } }"
            + "         }"
            + "       }"
            + "     }"
            + "     if(tgn==='a') add('linkText',null,tx,'link-text',uXp(d,'//a[normalize-space(.)='+lit(tx)+']'),false);"
            + "   }"
            + "   if(tgn==='a'){ var hr=el.getAttribute&&el.getAttribute('href');"
            + "     if(hr&&hr.length>0&&hr.length<120&&hr!=='#'&&hr.indexOf('javascript')<0&&!dyn(hr)){"
            + "       var hs='a'+asel('href',hr); add('css',hs,hr,'href-css',uCss(d,hs),false); } }"
            + "   (function(){ var anc=el.parentElement,n=0; while(anc&&n<6){"
            + "     var aid=anc.id,atd=anc.getAttribute&&(anc.getAttribute('data-testid')||anc.getAttribute('data-test'));"
            + "     var anchor=null; if(aid&&!dyn(aid)) anchor='#'+aid; else if(atd&&!dyn(atd)) anchor=asel('data-testid',atd);"
            + "     if(!anchor){ var acl=anc.getAttribute&&anc.getAttribute('class'); if(acl){"
            + "       var ascs=acl.trim().split(/\\s+/).filter(function(c){ return c&&!dyn(c)&&c.length>1; });"
            + "       var ancCls=''; for(var aci=0;aci<Math.min(ascs.length,3);aci++){ ancCls+='.'+ascs[aci]; if(uCss(d,ancCls)){ anchor=ancCls; break; } } } }"
            + "     if(anchor&&tgn){ var as1=anchor+' '+tgn; if(uCss(d,as1)){ add('css',as1,as1,'ancestor-css',true,false); return; }"
            + "       for(var k=0;k<Math.min(3,attrs.length);k++){ var an3=attrs[k].name,av3=attrs[k].value;"
            + "         if(an3==='class'||an3==='style') continue; var as2=anchor+' '+tgn+asel(an3,av3);"
            + "         if(uCss(d,as2)){ add('css',as2,as2,'ancestor-css',true,false); return; } }"
            + "     } anc=anc.parentElement; n++; }"
            + "   })();"
            + "   var cl=el.getAttribute&&el.getAttribute('class'); if(cl){ var toks=cl.trim().split(/\\s+/),fc=null; for(var ci=0;ci<toks.length;ci++){ if(toks[ci]&&!dyn(toks[ci])){fc=toks[ci];break;} } if(fc) add('className',null,fc,'class-name',uCls(d,fc),false); }"
            + "   if(tgn) add('tagName',null,tgn,'tag-name',uCss(d,tgn),false);"
            + "   var cp=cpath(el); if(cp) add('css',cp,cp,'css-path',uCss(d,cp),false); return o; }"
            + " W.__ellInBar=W.__ellInBar||function(el){return el&&(el.id==='ellithium-recorder-toolbar'||el.id==='ell-code-overlay'||el.id==='ell-attr-dlg'||(el.closest&&(el.closest('#ellithium-recorder-toolbar')||el.closest('#ell-code-overlay')||el.closest('#ell-attr-dlg'))));};function inBar(el){return W.__ellInBar(el);}"
            + " function tgt(e){ var p=e.composedPath&&e.composedPath(); return (p&&p.length)?p[0]:e.target; }"
            + " function emitEl(type,el,value,frame){ if(inBar(el))return; var nm=(el.getAttribute&&(el.getAttribute('aria-label')||el.getAttribute('name')||el.getAttribute('placeholder')))||(el.textContent||'').trim().slice(0,40);"
            + "   emit({id:nid(),type:type,tag:(el.tagName||'').toLowerCase(),name:nm,value:(value==null?null:value),frame:frame,candidates:cands(el)}); }"
            + " function disarm(){ W.__ellMode='record'; var a=document.querySelectorAll('.ell-armed'); for(var i=0;i<a.length;i++)a[i].classList.remove('ell-armed'); }"
            + " function inp(el,frame){ var v=el.value; if(W.__ellLastVal.get(el)===v)return; W.__ellLastVal.set(el,v); emitEl('input',el,v,frame); }"
            + " function attach(doc,frame){ if(!doc||doc.__ellAttached)return; doc.__ellAttached=true;"
            + "   doc.addEventListener('click',function(e){ var raw=tgt(e); if(inBar(raw)||W.__ellResizing)return; var m=W.__ellMode||'record';"
            + "     if(m==='inspect'){e.preventDefault();e.stopPropagation();emitEl('inspect',raw,null,frame);return;}"
            + "     if(m==='assertVisible'){e.preventDefault();e.stopPropagation();emitEl('assertVisible',raw,null,frame);disarm();return;}"
            + "     if(m==='assertText'){e.preventDefault();e.stopPropagation();emitEl('assertText',raw,((raw.innerText||raw.textContent||'').trim()).slice(0,80),frame);disarm();return;}"
            + "     if(m==='assertValue'){e.preventDefault();e.stopPropagation();"
            + "       var el=raw,tag=(el.tagName||'').toLowerCase();"
            + "       var nm=(el.getAttribute&&(el.getAttribute('aria-label')||el.getAttribute('name')))||(el.textContent||'').trim().slice(0,40);"
            + "       var pAttrs={};"
            + "       if(el.attributes){for(var ai=0;ai<el.attributes.length;ai++){"
            + "         var an=el.attributes[ai].name,av=el.attributes[ai].value;"
            + "         if(an.indexOf('on')===0||an==='style'||an.length>60||av.length>250)continue;"
            + "         pAttrs[an]=av;}}"
            + "       if(typeof el.value==='string'&&el.value!=='')pAttrs['value']=el.value;"
            + "       if(typeof el.checked==='boolean')pAttrs['checked']=String(el.checked);"
            + "       var txt=(el.textContent||'').trim().slice(0,120);"
            + "       if(txt)pAttrs['text()']=txt;"
            + "       var prefer=['value','id','name','class','aria-label','href','placeholder'];"
            + "       var defAttr='text()';"
            + "       if(['input','textarea','select'].indexOf(tag)>=0&&pAttrs['value']!==undefined){defAttr='value';}"
            + "       else{for(var pi=0;pi<prefer.length;pi++){if(pAttrs[prefer[pi]]!==undefined){defAttr=prefer[pi];break;}}}"
            + "       var pend={id:nid(),frame:frame,candidates:cands(el),tag:tag,name:nm,attrs:pAttrs,defaultAttr:defAttr};"
            + "       if(typeof W.__ellShowAttrDialog==='function')W.__ellShowAttrDialog(pend);"
            + "       disarm();return;}"
            + "     if(m==='assertEnabled'){e.preventDefault();e.stopPropagation();emitEl('assertEnabled',raw,null,frame);disarm();return;}"
            + "     if(m==='assertSelected'){e.preventDefault();e.stopPropagation();emitEl('assertSelected',raw,null,frame);disarm();return;}"
            + "     if(m==='hover'){e.preventDefault();e.stopPropagation();emitEl('hover',meaningful(raw),null,frame);disarm();return;}"
            + "     if(W.__ellPaused)return;"
            + "     var me=meaningful(raw); var meTag=(me.tagName||'').toLowerCase();"
            + "     if(meTag==='select'||meTag==='option') return;"  // change handler covers selects
            + "     if(!isInteractable(me)) return;"                 // skip invisible/zero-size elements
            + "     var now=Date.now();"                             // label→input dedup: suppress synthetic clicks within 80ms of same element
            + "     if(W.__ellLastClickEl===me&&(now-W.__ellLastClickTs)<80) return;"
            + "     W.__ellLastClickEl=me; W.__ellLastClickTs=now;"
            + "     emitEl('click',me,null,frame); },true);"
            + "   doc.addEventListener('dblclick',function(e){ var el=tgt(e); if(inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return; emitEl('doubleClick', meaningful(el), null, frame); },true);"
            + "   doc.addEventListener('dragstart',function(e){ var el=meaningful(tgt(e)); if(inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return;"
            + "     W.__ellDragSrc={el:el,cands:cands(el)}; },true);"
            + "   doc.addEventListener('drop',function(e){ var src=W.__ellDragSrc; W.__ellDragSrc=null; if(!src)return; var tg=meaningful(tgt(e)); if(inBar(tg))return;"
            + "     if(W.__ellPaused||(W.__ellMode||'record')!=='record')return;"
            + "     emit({id:nid(),type:'dragAndDrop',tag:(src.el.tagName||'').toLowerCase(),"
            + "       name:(src.el.getAttribute&&(src.el.getAttribute('aria-label')||src.el.getAttribute('name')))||(src.el.textContent||'').trim().slice(0,40),"
            + "       value:null,frame:frame,candidates:src.cands,targetCandidates:cands(tg)}); },true);"
            + "   doc.addEventListener('change',function(e){ var el=tgt(e); if(!el||inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return;"
            + "     var tag=(el.tagName||'').toLowerCase(); if(tag==='select'){"
            + "       if(el.multiple){"
            + "         var desel=[]; for(var oi=0;oi<el.options.length;oi++){ if(!el.options[oi].selected&&el.options[oi].__ellWasSel) desel.push(el.options[oi].text); }"
            + "         for(var oi=0;oi<el.options.length;oi++) el.options[oi].__ellWasSel=el.options[oi].selected;"
            + "         if(desel.length>0){ emitEl('deselectByText',el,desel[0],frame); return; }"
            + "       }"
            + "       var t=el.options[el.selectedIndex]?el.options[el.selectedIndex].text:el.value; emitEl('select',el,t,frame); }"
            + "     else if(tag==='input'||tag==='textarea'){ var ty=(el.getAttribute('type')||'').toLowerCase();"
            + "       if(ty==='file'){ emitEl('uploadFile', el, (el.value||'').split('\\\\').pop(), frame); return; }"
            + "       if(ty==='checkbox'||ty==='radio'||ty==='submit'||ty==='button'||ty==='image') return;"
            + "       inp(el,frame); } },true);"
            + "   doc.addEventListener('focus',function(e){ var el=tgt(e); if(!el||el.tagName!=='SELECT'||!el.multiple)return;"
            + "     for(var oi=0;oi<el.options.length;oi++) el.options[oi].__ellWasSel=el.options[oi].selected; },true);"
            + "   doc.addEventListener('keydown',function(e){ var el=tgt(e); if(!el||inBar(el))return;"
            + "     if((e.key==='F5'||(e.ctrlKey&&e.key==='r'))&&!inBar(el)){ if(W.__ellPaused||(W.__ellMode||'record')!=='record')return;"
            + "       e.preventDefault(); emit({id:nid(),type:'navigateRefresh',tag:'',name:'',value:null,frame:frame,candidates:[]}); return; }"
            + "     if(e.key!=='Enter')return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return;"
            + "     var tag=(el.tagName||'').toLowerCase(); if(tag==='input'||tag==='textarea'){ inp(el,frame); emitEl('pressEnter',el,null,frame); } },true);"
            + "   doc.addEventListener('contextmenu',function(e){ var el=tgt(e); if(inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return; emitEl('rightClick', meaningful(el), null, frame); },true);"
            + "   doc.addEventListener('mousemove',function(e){ var m=W.__ellMode||'record'; if(m==='record'){ if(W.__ellHi){try{W.__ellHi.style.outline=W.__ellHiPrev||''}catch(x){} W.__ellHi=null;} return; }"
            + "     var el=tgt(e); if(inBar(el))return; if(W.__ellHi&&W.__ellHi!==el){try{W.__ellHi.style.outline=W.__ellHiPrev||''}catch(x){}}"
            + "     if(el&&el!==W.__ellHi){ W.__ellHi=el; W.__ellHiPrev=el.style.outline; try{el.style.outline='2px solid #0a84ff'}catch(x){} } },true); }"
            + " function walk(doc,path,depth){ if(!doc||depth>3)return; attach(doc,path); var fr=doc.querySelectorAll('iframe,frame');"
            + "   for(var i=0;i<fr.length&&i<12;i++){ try{ var cd=fr[i].contentDocument; if(cd) walk(cd,path.concat([i]),depth+1); }catch(e){} } }"
            + " walk(document,[],0);"
            + "})(arguments[0]);";

    static final String OVERLAY_SCRIPT =
            "return (function(){"
            + " if(window.__ellOverlayDone && document.getElementById('ellithium-recorder-toolbar')) return false;"
            + " var __ex=document.querySelectorAll('#ellithium-recorder-toolbar'); for(var __i=1;__i<__ex.length;__i++)__ex[__i].remove(); if(__ex.length>=1){ window.__ellOverlayDone=true; return false; }"
            + " var bar=document.createElement('div'); bar.id='ellithium-recorder-toolbar';"
            + " bar.style.cssText='position:fixed;top:12px;right:12px;z-index:2147483647;width:390px;max-height:86vh;"
            + "display:flex;flex-direction:column;overflow:hidden;background:#0d1117;color:#e6edf3;border-radius:12px;"
            + "border:1px solid #30363d;font-family:system-ui,sans-serif;font-size:12px;box-shadow:0 16px 48px rgba(0,0,0,.75);';"
            + " function btn(id,label,title){ return '<button id=\"'+id+'\" title=\"'+title+'\" class=\"ell-btn\" style=\"background:#21262d;color:#c9d1d9;border:1px solid #30363d;border-radius:6px;padding:4px 8px;cursor:pointer;font-size:11px;line-height:1.3\">'+label+'</button>'; }"
            + " bar.innerHTML='<div id=\"ell-head\" style=\"display:flex;align-items:center;gap:8px;padding:10px 12px;"
            + "background:#161b22;border-bottom:1px solid #30363d;border-radius:12px 12px 0 0;cursor:move;flex-shrink:0\">'"
            + "  +'<img src=\"data:image/x-icon;base64,AAABAAEAEBAAAAEAIABoBAAAFgAAACgAAAAQAAAAIAAAAAEAIAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAASEhT/HB0d/ygmJf8jIyT/ODUx/0ZAN/9IPzP/Sz4t/0g5J/8/MSH/RTsv/zctJP8pIRn/JiQk/x8fH/8XFhf/FBUX/yYmJf8+PTb/hZCU/4CGgP9+fnP/gHps/3VtXv9+dGT/mJaJ/4mKgP+Nk47/eX97/z1AQv8oKCr/Ghka/xgZGv8pLC7/LTI1/2hyef9cYmP/RkA2/0tCNv+LfWv/fGZP/2hkW/9QS0L/aGlk/1tfXf8/Q0b/Kywt/x4dHP8dHx//KC0w/yg1RP8ZIzL/JCkv/yYrMP8yNz3/f2pS/4lxVf8qKzD/Fxge/xsaHP8qMzz/KTM9/xcWGP8cGhn/HR8f/z5KUf9RaHv/OUtb/zdBSv9kZmP/gHFc/8yvev/Ssnj/dmJM/0tJR/8hJzP/OERO/0pba/84RlH/GxgX/x4gIf9RW17/dI2a/z5NWv92cWf/mXxU/5d7Tv94ZET/eGJD/5h7Tv+Qb0X/aWFY/z5NXv9WZnD/TFZc/yQiH/8mKir/QERF/zhEUP9VWl7/g2tM/4p0UP8uMjj/Qzw0/0Y+OP8uMTn/gWxM/4htSP9iam//LThF/yspKP8rKSb/LTAw/0NHS/9KWmf/XldM/5R8V/88QUf/a11J/6GAUv+jhFr/eGhR/zAzOP+Ockv/d3Bj/yswOf8fHBz/LCon/zY3NP+Femr/p6KV/8Gjb/+kkm3/UFdb/5+FXv+0l1r/vqZl/6GBWP87NjP/indV/9W4hP+Re2T/dV1D/z04MP8tMDH/XWFi/42cnv95aFH/koFi/3aJlP+ll3v/rJVm/6WJU/+XfFT/NzpA/4hzUP+Ugmb/YmFg/0tBN/8zMSz/Jy4x/zM+Sf9wi5n/YGpr/3BZPv+Wrbb/nLG5/6OWfv+djG3/VFVS/1RUTv+Mck//TElG/ys3Qv8mJyv/Jycm/yUsL/88S1X/aIWW/6TBx/9YSzz/m4Zq/5Sgof94i5T/ZG9y/3F0b/+giWT/aFZB/1NbX/89SVP/Iygw/x8eHf8dJCn/S15p/01mdv+Jq7v/tMnI/2tcSv9vVjn/rJVn/7KZaf9/Z0f/cF9J/3Fybf87RU3/X215/z9GTf8ZGRr/HCQr/ycxOv9BUlv/SGBu/4Skt/+duMD/e4F8/7Cfe/+wnnn/aWdc/3J9fP9edYP/Mj5L/ysyO/8kKS3/Fxga/xgfJf8mLzf/R1hj/0pZZP9DUVz/UmFs/2l+if+Pl5T/jZCK/1VkbP9OWWD/YniF/0laaP8yOUD/ICUp/xMUFv8UGh//HSQr/yozOv8tNjz/MDg8/zE3Ov8vNTr/RD42/0Y+M/8zNTf/P0JC/zk8Pf81OTz/LTM4/xgbH/8OEBH/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\" style=\"width:18px;height:18px;image-rendering:pixelated;flex-shrink:0\">'"
            + "  +'<span style=\"font-weight:700;font-size:13px;color:#58a6ff;letter-spacing:.3px\">Ellithium</span>'"
            + "  +'<span style=\"color:#8b949e;font-size:11px\">Recorder</span>'"
            + "  +'<span style=\"flex:1\"></span>'"
            + "  +'<span id=\"ell-dot\" style=\"width:8px;height:8px;background:#f85149;border-radius:50%;box-shadow:0 0 7px rgba(248,81,73,.7);flex-shrink:0\"></span>'"
            + "  +'</div>'"
            + "  +'<div style=\"display:flex;align-items:center;gap:4px;flex-wrap:wrap;padding:7px 12px;"
            + "border-bottom:1px solid #30363d;background:#161b22;flex-shrink:0\">'"
            + "  +btn('ell-rec','&#x23F8;&#xFE0F; Pause','Pause/Resume recording')"
            + "  +btn('ell-pick','&#x1F50D; Inspect','Inspect / pick locator (toggle)')"
            + "  +btn('ell-hover','Hover','Record a hover on the next click')"
            + "  +btn('ell-av','Eye','Assert visible')"
            + "  +btn('ell-at','Aa','Assert text')"
            + "  +btn('ell-aval','&#x270F;&#xFE0F; Val','Assert value')"
            + "  +btn('ell-aen','Enabled','Assert enabled')"
            + "  +btn('ell-asel','&#x2714; Sel','Assert selected')"
            + "  +'</div>'"
            + "  +'<div style=\"display:flex;align-items:center;gap:4px;padding:6px 12px;"
            + "border-bottom:1px solid #30363d;flex-shrink:0\">'"
            + "  +btn('ell-assert','Assert: soft','Toggle hard/soft asserts')"
            + "  +'<span style=\"flex:1\"></span>'"
            + "  +btn('ell-clear','&#x1F5D1;&#xFE0F; Clear','Clear all steps')"
            + "  +btn('ell-stop','&#x23F9;&#xFE0F; Stop','Stop and generate')"
            + "  +'</div>'"
            + "  +'<div style=\"padding:8px 12px;border-bottom:1px solid #30363d;flex-shrink:0\">'"
            + "  +'<input id=\"ell-eval\" placeholder=\"Evaluate CSS or XPath...\" style=\"width:100%;box-sizing:border-box;"
            + "background:#161b22;color:#e6edf3;border:1px solid #30363d;border-radius:6px;padding:5px 8px;font-size:11px\">'"
            + "  +'<div id=\"ell-eval-count\" style=\"color:#8b949e;margin-top:4px;font-size:11px\">&nbsp;</div>'"
            + "  +'</div>'"
            + "  +'<div style=\"overflow:auto;flex:1;padding:8px 12px\">'"
            + "  +'<div id=\"ell-picked\"></div><div id=\"ell-steps\"></div>'"
            + "  +'<div style=\"display:flex;align-items:center;gap:6px;margin-top:8px;padding-top:8px;border-top:1px solid #21262d\">'"
            + "  +'<span style=\"font-size:10px;font-weight:600;color:#8b949e;text-transform:uppercase;letter-spacing:.6px;flex:1\">Code Preview</span>'"
            + "  +btn('ell-copy','&#x1F4CB; Copy','Copy code')"
            + "  +btn('ell-expand','&#x29C9; Expand','Open code in separate window')"
            + "  +'</div>'"
            + "  +'<pre id=\"ell-code\" style=\"background:#161b22;border:1px solid #30363d;border-radius:6px;padding:8px;"
            + "white-space:pre-wrap;word-break:break-word;max-height:26vh;overflow:auto;margin:6px 0 0;"
            + "font-size:11px;line-height:1.5;color:#c9d1d9\"></pre>'"
            + "  +'</div>';"
            + " document.body.appendChild(bar);"
            + " bar.addEventListener('mousedown',function(e){"
            + "   if(e.target.tagName!=='INPUT'&&e.target.tagName!=='TEXTAREA'&&e.target.tagName!=='SELECT') e.preventDefault();"
            + " },true);"
            + " (function(){"
            + "   var rs=[['ell-rs-left',     'position:absolute;left:0;top:10px;bottom:10px;width:5px;cursor:ew-resize;z-index:3;border-radius:0 3px 3px 0','w'],"
            + "           ['ell-rs-right',    'position:absolute;right:0;top:10px;bottom:10px;width:5px;cursor:ew-resize;z-index:3;border-radius:3px 0 0 3px','e'],"
            + "           ['ell-rs-bottom',   'position:absolute;bottom:0;left:10px;right:10px;height:5px;cursor:ns-resize;z-index:3','s'],"
            + "           ['ell-rs-top',      'position:absolute;top:0;left:10px;right:10px;height:5px;cursor:ns-resize;z-index:3','n'],"
            + "           ['ell-rs-corner',   'position:absolute;bottom:0;left:0;width:10px;height:10px;cursor:nesw-resize;z-index:4','ws'],"
            + "           ['ell-rs-corner-br','position:absolute;bottom:0;right:0;width:10px;height:10px;cursor:nwse-resize;z-index:4','se'],"
            + "           ['ell-rs-corner-tr','position:absolute;top:0;right:0;width:10px;height:10px;cursor:nesw-resize;z-index:4','ne'],"
            + "           ['ell-rs-corner-tl','position:absolute;top:0;left:0;width:10px;height:10px;cursor:nwse-resize;z-index:4','nw']];"
            + "   rs.forEach(function(r){ var h=document.createElement('div'); h.className='ell-rs '+r[0]; h.style.cssText=r[1]; h.setAttribute('data-dir',r[2]); bar.appendChild(h); }); })();"
            + " var style=document.createElement('style');"
            + " style.textContent='#ellithium-recorder-toolbar .ell-btn:hover{background:#30363d!important;border-color:#58a6ff!important}'"
            + "  +'#ellithium-recorder-toolbar .ell-armed{background:#1f6feb!important;border-color:#388bfd!important;color:#e6edf3!important}'"
            + "  +'#ellithium-recorder-toolbar code{font-size:11px;word-break:break-all;font-family:ui-monospace,\"Cascadia Code\",monospace}'"
            + "  +'#ellithium-recorder-toolbar #ell-eval:focus{outline:none;border-color:#58a6ff!important}'"
            + "  +'#ellithium-recorder-toolbar .ell-rs{background:transparent;transition:background .15s}'"
            + "  +'#ellithium-recorder-toolbar .ell-rs:hover{background:rgba(88,166,255,.28)}';"
            + " document.head.appendChild(style);"
            + " window.__ellInBar=function(el){return el&&(el.id==='ellithium-recorder-toolbar'||el.id==='ell-code-overlay'||el.id==='ell-attr-dlg'"
            + "   ||(el.closest&&(el.closest('#ellithium-recorder-toolbar')||el.closest('#ell-code-overlay')||el.closest('#ell-attr-dlg'))));};  "
            + " function arm(id,mode){ document.getElementById(id).addEventListener('click', function(){ var on=window.__ellMode===mode;"
            + "   var a=document.querySelectorAll('.ell-armed'); for(var i=0;i<a.length;i++)a[i].classList.remove('ell-armed');"
            + "   window.__ellMode=on?'record':mode; if(!on) this.classList.add('ell-armed'); }); }"
            + " arm('ell-pick','inspect'); arm('ell-hover','hover'); arm('ell-av','assertVisible'); arm('ell-at','assertText'); arm('ell-aval','assertValue'); arm('ell-aen','assertEnabled'); arm('ell-asel','assertSelected');"
            + " function logPush(o){ try{ var a=JSON.parse(localStorage.getItem('__ellRecLog')||'[]'); a.push(o); localStorage.setItem('__ellRecLog',JSON.stringify(a)); }catch(e){} }"
            + " window.__ellShowAttrDialog=function(pending){"
            + "   var old=document.getElementById('ell-attr-dlg'); if(old) old.remove();"
            + "   var dlg=document.createElement('div'); dlg.id='ell-attr-dlg';"
            + "   dlg.style.cssText='position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);z-index:2147483647;"
            + "background:#161b22;border:1px solid #388bfd;border-radius:10px;padding:16px;width:320px;"
            + "box-shadow:0 8px 32px rgba(0,0,0,.85);font-family:system-ui,sans-serif;font-size:12px;color:#e6edf3';"
            + "   dlg.innerHTML='<div style=\"font-weight:700;font-size:13px;color:#58a6ff;margin-bottom:8px\">Assert Attribute Value</div>'"
            + "     +'<div id=\"ell-ad-info\" style=\"color:#8b949e;font-size:11px;margin-bottom:10px\"></div>'"
            + "     +'<label style=\"display:block;color:#8b949e;margin-bottom:3px\">Attribute</label>'"
            + "     +'<select id=\"ell-ad-sel\" style=\"width:100%;background:#0d1117;color:#e6edf3;border:1px solid #30363d;border-radius:6px;padding:5px;margin-bottom:6px;box-sizing:border-box\"></select>'"
            + "     +'<input id=\"ell-ad-cust\" placeholder=\"Custom attribute name\" style=\"display:none;width:100%;box-sizing:border-box;background:#0d1117;color:#e6edf3;border:1px solid #30363d;border-radius:6px;padding:5px;margin-bottom:6px\">'"
            + "     +'<label style=\"display:block;color:#8b949e;margin-bottom:3px\">Expected value</label>'"
            + "     +'<input id=\"ell-ad-val\" style=\"width:100%;box-sizing:border-box;background:#0d1117;color:#e6edf3;border:1px solid #30363d;border-radius:6px;padding:5px;margin-bottom:12px\">'"
            + "     +'<div style=\"display:flex;gap:6px;justify-content:flex-end\">'"
            + "     +'<button id=\"ell-ad-cancel\" class=\"ell-btn\">Cancel</button>'"
            + "     +'<button id=\"ell-ad-ok\" class=\"ell-btn\" style=\"background:#1f6feb;border-color:#388bfd\">&#x2713; Add</button>'"
            + "     +'</div>';"
            + "   document.body.appendChild(dlg);"
            + "   document.getElementById('ell-ad-info').textContent='<'+pending.tag+'> '+(pending.name||'element').slice(0,30);"
            + "   var sel=document.getElementById('ell-ad-sel');"
            + "   Object.keys(pending.attrs||{}).forEach(function(a){"
            + "     var opt=document.createElement('option'); opt.value=a;"
            + "     opt.textContent=a+' = \"'+String(pending.attrs[a]).slice(0,25)+'\"';"
            + "     if(a===pending.defaultAttr) opt.selected=true; sel.appendChild(opt); });"
            + "   var custOpt=document.createElement('option'); custOpt.value='__custom__';"
            + "   custOpt.textContent='Custom attribute...'; sel.appendChild(custOpt);"
            + "   var cust=document.getElementById('ell-ad-cust');"
            + "   var val=document.getElementById('ell-ad-val');"
            + "   val.value=(pending.attrs[pending.defaultAttr]||'');"
            + "   sel.addEventListener('change',function(){"
            + "     if(sel.value==='__custom__'){cust.style.display='block';val.value='';}"
            + "     else{cust.style.display='none';val.value=(pending.attrs[sel.value]||'');} });"
            + "   document.getElementById('ell-ad-cancel').addEventListener('click',function(){dlg.remove();});"
            + "   document.getElementById('ell-ad-ok').addEventListener('click',function(){"
            + "     var attr=sel.value==='__custom__'?cust.value.trim():sel.value;"
            + "     if(!attr) return;"
            + "     logPush({type:'assertValue',id:pending.id,tag:pending.tag,name:pending.name,"
            + "              value:val.value,assertAttr:attr,frame:pending.frame,candidates:pending.candidates});"
            + "     dlg.remove(); });"
            + "   val.focus(); val.select(); };"
            + " (function(){"
            + "   var p=window.__ellPaused||(function(){try{return localStorage.getItem('__ellPaused')==='1';}catch(e){return false;}}());"
            + "   var rb=document.getElementById('ell-rec'); if(p){ rb.innerHTML='&#x25B6;&#xFE0F; Resume';"
            + "   var dot=document.getElementById('ell-dot');"
            + "   dot.style.background='#8b949e'; dot.style.boxShadow='none'; } })();"
            + " document.getElementById('ell-rec').addEventListener('click', function(){ window.__ellPaused=!window.__ellPaused;"
            + "   try{localStorage.setItem('__ellPaused', window.__ellPaused?'1':'0');}catch(e){}"
            + "   this.innerHTML=window.__ellPaused?'&#x25B6;&#xFE0F; Resume':'&#x23F8;&#xFE0F; Pause';"
            + "   var dot=document.getElementById('ell-dot');"
            + "   dot.style.background=window.__ellPaused?'#8b949e':'#f85149';"
            + "   dot.style.boxShadow=window.__ellPaused?'none':'0 0 7px rgba(248,81,73,.7)'; });"
            + " document.getElementById('ell-clear').addEventListener('click', function(){ logPush({type:'clearAll'}); });"
            + " document.getElementById('ell-assert').addEventListener('click', function(){ logPush({type:'assertModeToggle'}); });"
            + " document.getElementById('ell-copy').addEventListener('click', function(){ var t=document.getElementById('ell-code').textContent;"
            + "   try{ navigator.clipboard.writeText(t); this.innerHTML='&#x2713; Copied'; var b=this; setTimeout(function(){b.innerHTML='&#x1F4CB; Copy';},1200); }catch(e){} });"
            + " document.getElementById('ell-stop').addEventListener('click', function(){ window.__ellStop=true; });"
            + " function clearHi(){ if(window.__ellEvalHi){ for(var i=0;i<window.__ellEvalHi.length;i++){ try{window.__ellEvalHi[i].style.outline=''}catch(x){} } } window.__ellEvalHi=[]; }"
            + " document.getElementById('ell-eval').addEventListener('input', function(){ clearHi(); var q=this.value.trim();"
            + "   var cnt=document.getElementById('ell-eval-count'); if(!q){ cnt.textContent='\\u00a0'; return; } var els=[];"
            + "   try{ if(q.charAt(0)==='/'||q.charAt(0)==='('){ var r=document.evaluate(q,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);"
            + "     for(var i=0;i<r.snapshotLength;i++) els.push(r.snapshotItem(i)); } else { els=Array.prototype.slice.call(document.querySelectorAll(q)); } }"
            + "   catch(x){ cnt.textContent='invalid selector'; return; } window.__ellEvalHi=els;"
            + "   for(var j=0;j<els.length;j++){ try{els[j].style.outline='2px solid #3fb950'}catch(x){} }"
            + "   cnt.textContent=els.length+' match'+(els.length===1?'':'es')+(els.length>1?' - not unique':''); });"
            + " ['keydown','keyup','keypress','input','paste'].forEach(function(ev){ document.getElementById('ell-eval').addEventListener(ev, function(e){ e.stopPropagation(); }, true); });"
            + " function expandCode(){"
            + "   var existing=document.getElementById('ell-code-overlay');"
            + "   if(existing){ existing.remove();"
            + "     var escH=existing.__escClose; if(escH) document.removeEventListener('keydown',escH);"
            + "     var eb=document.getElementById('ell-expand'); if(eb) eb.classList.remove('ell-armed'); return; }"
            + "   var logoSrc=(document.querySelector('#ellithium-recorder-toolbar img')||{}).src||'';"
            + "   var code=document.getElementById('ell-code').textContent;"
            + "   var ov=document.createElement('div'); ov.id='ell-code-overlay';"
            + "   ov.style.cssText='position:fixed;top:0;left:0;right:0;bottom:0;z-index:2147483646;"
            + "background:#0d1117;color:#c9d1d9;display:flex;flex-direction:column;"
            + "font-family:ui-monospace,\"Cascadia Code\",monospace';"
            + "   var hd=document.createElement('div');"
            + "   hd.style.cssText='display:flex;align-items:center;gap:10px;padding:10px 16px;"
            + "background:#161b22;border-bottom:1px solid #30363d;flex-shrink:0';"
            + "   hd.innerHTML='<img src=\"'+logoSrc+'\" width=18 height=18 style=image-rendering:pixelated>'"
            + "     +'<b style=color:#58a6ff>Ellithium</b><span style=\"color:#8b949e;font-size:12px\">Code Preview</span>';"
            + "   var pre=document.createElement('pre');"
            + "   pre.style.cssText='flex:1;overflow:auto;margin:0;padding:16px;white-space:pre-wrap;"
            + "word-break:break-all;font-size:13px;line-height:1.6;tab-size:4'; pre.textContent=code;"
            + "   var cpBtn=document.createElement('button'); cpBtn.className='ell-btn';"
            + "   cpBtn.style.marginLeft='auto'; cpBtn.innerHTML='&#x1F4CB; Copy';"
            + "   cpBtn.addEventListener('click',function(){"
            + "     try{navigator.clipboard.writeText(pre.textContent);"
            + "       cpBtn.innerHTML='&#x2713; Copied'; setTimeout(function(){cpBtn.innerHTML='&#x1F4CB; Copy';},1200);}catch(x){} });"
            + "   var clBtn=document.createElement('button'); clBtn.className='ell-btn';"
            + "   clBtn.style.cssText='color:#f85149;border-color:rgba(248,81,73,.3)';"
            + "   clBtn.innerHTML='&#x2715; Close'; clBtn.addEventListener('click',expandCode);"
            + "   hd.appendChild(cpBtn); hd.appendChild(clBtn);"
            + "   ov.appendChild(hd); ov.appendChild(pre); document.body.appendChild(ov);"
            + "   var eb=document.getElementById('ell-expand'); if(eb) eb.classList.add('ell-armed');"
            + "   function escH(e){ if(e.key==='Escape') expandCode(); }"
            + "   document.addEventListener('keydown',escH); ov.__escClose=escH; }"
            + " document.getElementById('ell-expand').addEventListener('click', expandCode);"
            + " var head=document.getElementById('ell-head'); var drag=false, ox=0, oy=0;"
            + " var rsz=false, rsDir='', rsRight0=0, rsTop0=0, rsLeft0=0, rsBottom0=0;"
            + " head.addEventListener('mousedown', function(e){ if(e.target.tagName==='BUTTON'||e.target.tagName==='INPUT') return;"
            + "   drag=true; var r=bar.getBoundingClientRect(); ox=e.clientX-r.left; oy=e.clientY-r.top; bar.style.right='auto'; e.preventDefault(); });"
            + " bar.querySelectorAll('.ell-rs').forEach(function(h){"
            + "   h.addEventListener('mousedown',function(e){"
            + "     rsz=true; window.__ellResizing=true; rsDir=h.getAttribute('data-dir'); var r=bar.getBoundingClientRect();"
            + "     rsRight0=r.right; rsTop0=r.top; rsLeft0=r.left; rsBottom0=r.bottom;"
            + "     bar.style.left=r.left+'px'; bar.style.right='auto';"
            + "     e.preventDefault(); e.stopPropagation(); });"
            + "   h.addEventListener('click',function(e){ e.stopPropagation(); e.preventDefault(); }); });"
            + " document.addEventListener('mousemove', function(e){"
            + "   if(drag){ bar.style.left=Math.max(0,e.clientX-ox)+'px'; bar.style.top=Math.max(0,e.clientY-oy)+'px'; return; }"
            + "   if(!rsz) return;"
            + "   if(rsDir.indexOf('w')>=0){ var nw=Math.max(280,rsRight0-e.clientX); bar.style.width=nw+'px'; bar.style.left=(rsRight0-nw)+'px'; }"
            + "   if(rsDir.indexOf('e')>=0) bar.style.width=Math.max(280,e.clientX-rsLeft0)+'px';"
            + "   if(rsDir.indexOf('s')>=0) bar.style.maxHeight=Math.max(200,e.clientY-rsTop0)+'px';"
            + "   if(rsDir.indexOf('n')>=0){ var nh=Math.max(200,rsBottom0-e.clientY); bar.style.maxHeight=nh+'px'; bar.style.top=Math.max(0,rsBottom0-nh)+'px'; } });"
            + " document.addEventListener('mouseup', function(){ drag=false; rsz=false; window.__ellResizing=false; });"
            + " window.__ellOverlayDone=true; return true;"
            + "})();";

    static final String RENDER_SCRIPT =
            "(function(json){"
            + " var data=JSON.parse(json); var steps=data.steps||[]; var picked=data.picked;"
            + " var ce=document.getElementById('ell-code'); if(ce) ce.textContent=data.code||'';"
            + " var ab=document.getElementById('ell-assert'); if(ab&&data.assertMode) ab.textContent='Assert: '+data.assertMode;"
            + " function row(c){ return '<div style=\"padding:2px 0;display:flex;align-items:baseline;gap:5px\">'"
            + "   +'<span style=\"color:'+(c.unique?'#3fb950':'#d29922')+';flex-shrink:0\">'+(c.unique?'&#x2713;':'&#x26A0;')+'</span>'"
            + "   +'<code>'+c.expr.replace(/</g,'&lt;')+'</code>'"
            + "   +'<span style=\"color:#8b949e;font-size:10px;flex-shrink:0\">'+c.tier+(c.param?' param':'')+'</span></div>'; }"
            + " function actionColor(a){"
            + "   if(a==='click'||a==='doubleClick') return '#58a6ff';"
            + "   if(a==='input'||a==='select'||a==='sendData'||a==='deselectByText') return '#3fb950';"
            + "   if(a.indexOf('assert')===0) return '#d29922';"
            + "   if(a==='navigate'||a==='navigateBack'||a==='navigateForward'||a==='navigateRefresh') return '#bc8cff';"
            + "   if(a==='hover'||a==='rightClick') return '#79c0ff';"
            + "   if(a==='pressEnter'||a==='uploadFile'||a==='dragAndDrop') return '#ffa657';"
            + "   return '#8b949e'; }"
            + " var pf=document.getElementById('ell-picked');"
            + " if(pf){ if(picked && picked.candidates && picked.candidates.length){"
            + "   var ph='<div style=\"border:1px solid rgba(56,139,253,.4);border-radius:8px;padding:8px;margin-bottom:8px;"
            + "background:rgba(31,111,235,.06)\"><div style=\"font-size:11px;font-weight:600;color:#58a6ff;margin-bottom:5px\">&#x1F50D; Picked Locator</div>';"
            + "   for(var i=0;i<picked.candidates.length;i++) ph+=row(picked.candidates[i]); pf.innerHTML=ph+'</div>'; } else pf.innerHTML=''; }"
            + " var host=document.getElementById('ell-steps'); if(!host) return; var html='';"
            + " var TDG_METHODS=[{g:'Name',m:['getRandomFullName','getRandomFirstName','getRandomLastName','getRandomUsername']},"
            + "   {g:'Contact',m:['getRandomEmail','getRandomPhoneNumber']},"
            + "   {g:'Location',m:['getRandomAddress','getRandomStreetAddress','getRandomCity','getRandomState','getRandomCountry','getRandomZipCode']},"
            + "   {g:'Professional',m:['getRandomCompany','getRandomJobTitle']},"
            + "   {g:'Security',m:['getRandomPassword']},"
            + "   {g:'Web',m:['getRandomWebsite','getRandomIPAddress']},"
            + "   {g:'Content',m:['getRandomSentence','getRandomParagraph']},"
            + "   {g:'Date',m:['getDayDateStamp','getRandomBirthDate']},"
            + "   {g:'Other',m:['getRandomCreditCardNumber','getRandomColor','getRandomUniversity']}];"
            + " function tdgOptions(cur){ var s='<option value=\"\"'+(cur===''?' selected':'')+'>\\u2014 Use JSON value (manual)</option>'; for(var g=0;g<TDG_METHODS.length;g++){ s+='<optgroup label=\"'+TDG_METHODS[g].g+'\">'; for(var m=0;m<TDG_METHODS[g].m.length;m++){ var mv=TDG_METHODS[g].m[m]; s+='<option value=\"'+mv+'\"'+(cur===mv?' selected':'')+'>'+mv+'()</option>'; } s+='</optgroup>'; } return s; }"
            + " var isInput=function(a){ return a==='input'||a==='sendData'; };"
            + " for(var s=0;s<steps.length;s++){ var st=steps[s];"
            + "   var fl=(st.frame&&st.frame.length)?(' <span style=\"color:#8b949e;font-size:10px\">[frame '+st.frame.join('>')+']</span>'):'';"
            + "   var ac=actionColor(st.action);"
            + "   var genBadge;"
            + "   if(st.action==='assertValue'&&st.assertAttr) genBadge=' <span style=\"color:#d29922\">'+st.assertAttr+'=&quot;'+(st.data||'').slice(0,25)+'&quot;</span>';"
            + "   else if(st.generatorMethod) genBadge=' <span style=\"background:rgba(63,185,80,.1);color:#3fb950;border:1px solid rgba(63,185,80,.3);border-radius:4px;padding:1px 5px;font-size:10px\">&#x1F3B2; '+st.generatorMethod+'()</span>';"
            + "   else genBadge=st.data?(' <span style=\"color:#8b949e\">\\u2192 '+(''+st.data).slice(0,40)+'</span>'):'';"
            + "   var genBtn=isInput(st.action)?('<select class=\"ell-gen\" data-id=\"'+st.id+'\" title=\"Auto-generate test data\" style=\"margin-left:4px;background:#21262d;color:#c9d1d9;border:1px solid #30363d;border-radius:4px;font-size:10px;cursor:pointer\">'+tdgOptions(st.generatorMethod||'')+'</select>'):'';"
            + "   html+='<div style=\"border:1px solid #21262d;border-radius:8px;padding:8px;margin-bottom:6px\">';"
            + "   html+='<div style=\"display:flex;align-items:center;gap:5px;flex-wrap:wrap;margin-bottom:4px\">';"
            + "   html+='<span style=\"background:#161b22;color:#8b949e;border-radius:4px;padding:1px 5px;font-size:10px;min-width:18px;text-align:center;flex-shrink:0\">'+(s+1)+'</span>';"
            + "   html+='<span style=\"color:'+ac+';font-weight:600;font-size:12px\">'+st.action+'</span>'+fl+genBadge+genBtn;"
            + "   html+='<button class=\"ell-del\" data-del=\"'+st.id+'\" title=\"Remove step\" style=\"margin-left:auto;background:rgba(248,81,73,.1);color:#f85149;border:1px solid rgba(248,81,73,.25);border-radius:4px;cursor:pointer;font-size:12px;padding:1px 6px;flex-shrink:0\">\\u2715</button>';"
            + "   html+='</div>';"
            + "   for(var j=0;j<st.candidates.length;j++){ var c=st.candidates[j]; var sel=(j===st.chosenIndex);"
            + "     html+='<label style=\"display:flex;gap:5px;align-items:baseline;cursor:pointer;padding:2px 0\">';"
            + "     html+='<input type=\"radio\" name=\"ell-'+st.id+'\" '+(sel?'checked':'')+' data-id=\"'+st.id+'\" data-idx=\"'+j+'\">';"
            + "     html+='<span style=\"color:'+(c.unique?'#3fb950':'#d29922')+';flex-shrink:0\">'+(c.unique?'\\u2713':'\\u26a0')+'</span>';"
            + "     html+='<code>'+c.expr.replace(/</g,'&lt;')+'</code>';"
            + "     html+='<span style=\"color:#8b949e;font-size:10px;flex-shrink:0\">'+c.tier+(c.param?' \\u00b7 param':'')+'</span></label>'; }"
            + "   html+='</div>'; }"
            + " host.innerHTML=html;"
            + " function push(o){ var a=JSON.parse(localStorage.getItem('__ellRecLog')||'[]'); a.push(o); localStorage.setItem('__ellRecLog', JSON.stringify(a)); }"
            + " var gens=host.querySelectorAll('.ell-gen');"
            + " for(var gi=0;gi<gens.length;gi++){ (function(sel){ var sid=sel.getAttribute('data-id');"
            + "   sel.addEventListener('change',function(){ push({type:'autoGenerate',id:sid,method:this.value||null}); }); })(gens[gi]); }"
            + " var radios=host.querySelectorAll('input[type=radio]');"
            + " for(var k=0;k<radios.length;k++){ radios[k].addEventListener('change', function(){"
            + "   push({type:'override', id:this.getAttribute('data-id'), index:parseInt(this.getAttribute('data-idx'))}); }); }"
            + " var dels=host.querySelectorAll('.ell-del');"
            + " for(var di=0;di<dels.length;di++){ dels[di].addEventListener('click', function(){ push({type:'delete', id:this.getAttribute('data-del')}); }); }"
            + "})(arguments[0]);";
}
