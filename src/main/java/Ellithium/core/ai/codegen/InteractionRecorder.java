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

    private static volatile boolean recording = false;
    private static volatile WebDriver driver = null;
    private static volatile RecorderOptions options = RecorderOptions.defaults();
    private static volatile Thread drainThread = null;
    private static volatile String lastUrl = null;
    private static volatile String startUrl = null;
    private static volatile long navHintEpoch = 0L;
    private static final java.util.Set<String> knownHandles =
            java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());
    private static final java.util.List<String> urlHistory =
            java.util.Collections.synchronizedList(new ArrayList<>());
    private static volatile int urlIndex = -1;
    private static final long NAV_HINT_TTL = 10_000L;

    private static final List<RecordedStep> STEPS = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, RecordedStep> BY_ID = new ConcurrentHashMap<>();
    private static volatile List<LocatorCandidate> lastPicked = List.of();

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
        recording = true;
        lastUrl = currentUrl();
        startUrl = (explicitStartUrl != null && !explicitStartUrl.isBlank()) ? explicitStartUrl : currentUrl();
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
        ensureInjected();
        render();
        drainThread = new Thread(InteractionRecorder::drainLoop, "ellithium-codegen-recorder");
        drainThread.setDaemon(true);
        drainThread.start();
        Reporter.log("InteractionRecorder: recording started", LogLevel.INFO_YELLOW);
    }

    public static synchronized List<RecordedStep> stop() {
        recording = false;
        Thread t = drainThread;
        if (t != null) {
            try { t.join(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
                ensureInjected();
                boolean changed = drainOnce();
                reinjectOnNavigation();
                checkNewTabs();
                if (stopRequested()) { recording = false; break; }
                if (changed) render();
            } catch (Exception ignored) {}
            sleep(POLL_MS);
        }
    }

    private static boolean driverAlive() {
        try {
            WebDriver d = driver;
            return d != null && !d.getWindowHandles().isEmpty();
        } catch (Exception e) {
            return false;
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

    private static boolean processEvent(Map<String, Object> ev) {
        String type = str(ev.get("type"));
        if (type == null) return false;
        if ("override".equals(type)) {
            RecordedStep step = BY_ID.get(str(ev.get("id")));
            if (step != null) { step.choose((int) asLong(ev.get("index"))); return true; }
            return false;
        }
        if ("clearAll".equals(type)) {
            STEPS.clear(); BY_ID.clear(); lastPicked = List.of();
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
        RecordedStep step = new RecordedStep(id, type, str(ev.get("value")),
                str(ev.get("tag")), str(ev.get("name")), candidates, frame);
        if (frame.isEmpty()) seedOne(candidates);
        STEPS.add(step);
        BY_ID.put(id, step);
        if ("click".equals(type) || "select".equals(type) || "input".equals(type)
                || "pressEnter".equals(type) || "doubleClick".equals(type)) {
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
                    STEPS.add(new RecordedStep("back-" + ts, "navigateBack", null, null, null, List.of()));
                } else if (urlIndex >= 0 && urlIndex < urlHistory.size() - 1
                        && url.equals(urlHistory.get(urlIndex + 1))) {
                    urlIndex++;
                    STEPS.add(new RecordedStep("fwd-" + ts, "navigateForward", null, null, null, List.of()));
                } else {
                    while (urlHistory.size() > urlIndex + 1) urlHistory.remove(urlHistory.size() - 1);
                    urlHistory.add(url);
                    urlIndex = urlHistory.size() - 1;
                    if (!actionInduced) {
                        STEPS.add(new RecordedStep("nav-" + ts, "navigate", url, null, null, List.of()));
                    }
                }
            }
        }
        navHintEpoch = 0L;
        ensureInjected();
        render();
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

    private static void ensureInjected() {
        if (!(driver instanceof JavascriptExecutor js)) return;
        try {
            js.executeScript(CAPTURE_SCRIPT, options.pickModeDefault());
            js.executeScript(OVERLAY_SCRIPT);
        } catch (Exception ignored) {}
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

    private static String renderJson() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        synchronized (STEPS) {
            for (RecordedStep s : STEPS) {
                JsonObject o = new JsonObject();
                o.addProperty("id", s.getId());
                o.addProperty("action", s.getActionType());
                o.addProperty("data", s.getData());
                o.addProperty("generatorMethod", s.getGeneratorMethod());
                o.addProperty("chosenIndex", s.getChosenIndex());
                JsonArray fr = new JsonArray();
                for (Integer idx : s.getFrameChain()) fr.add(idx);
                o.add("frame", fr);
                o.add("candidates", candidatesJson(s.getCandidates()));
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
        try {
            List<RecordedStep> snap = new ArrayList<>(STEPS);
            boolean soft = options.isSoftAssert();
            String code = options.isTest()
                    ? PomCodeEmitter.previewTestSource(snap, CodegenCli.deriveClassName(startUrl),
                        options.packageName(), startUrl, options.browser(), soft)
                    : PomCodeEmitter.previewSource(snap, "RecordedPage", options.packageName(), soft);
            root.addProperty("code", code);
        } catch (Exception ignored) {}
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

    private static final String CAPTURE_SCRIPT =
            "(function(pick){"
            + " var W=window;"
            + " if(!W.__ellRecInit){ W.__ellRecInit=true; W.__ellMode=pick?'pick':'record'; W.__ellStop=false;"
            + "   W.__ellPaused=(function(){try{return localStorage.getItem('__ellPaused')==='1';}catch(e){return false;}})();"
            + "   W.__ellLastVal=new WeakMap(); }"
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
            + "   var mcl=c.getAttribute&&c.getAttribute('class'); if(mcl){ var msc=mcl.trim().split(/\\s+/).filter(function(x){return x&&!dyn(x)&&x.length>2;}); if(msc.length>=2) return c; }"
            + "   c=c.parentElement; n++; } return el; }"
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
            + " function inBar(el){ return el&&(el.id==='ellithium-recorder-toolbar'||(el.closest&&el.closest('#ellithium-recorder-toolbar'))); }"
            + " function tgt(e){ var p=e.composedPath&&e.composedPath(); return (p&&p.length)?p[0]:e.target; }"
            + " function emitEl(type,el,value,frame){ if(inBar(el))return; var nm=(el.getAttribute&&(el.getAttribute('aria-label')||el.getAttribute('name')||el.getAttribute('placeholder')))||(el.textContent||'').trim().slice(0,40);"
            + "   emit({id:nid(),type:type,tag:(el.tagName||'').toLowerCase(),name:nm,value:(value==null?null:value),frame:frame,candidates:cands(el)}); }"
            + " function disarm(){ W.__ellMode='record'; var a=document.querySelectorAll('.ell-armed'); for(var i=0;i<a.length;i++)a[i].classList.remove('ell-armed'); }"
            + " function inp(el,frame){ var v=el.value; if(W.__ellLastVal.get(el)===v)return; W.__ellLastVal.set(el,v);"
            + "   var pw=((el.getAttribute('type')||'').toLowerCase()==='password'); emitEl('input',el, pw?'__ELL_SECRET__':v, frame); }"
            + " function attach(doc,frame){ if(!doc||doc.__ellAttached)return; doc.__ellAttached=true;"
            + "   doc.addEventListener('click',function(e){ var raw=tgt(e); if(inBar(raw))return; var m=W.__ellMode||'record';"
            + "     if(m==='inspect'){e.preventDefault();e.stopPropagation();emitEl('inspect',raw,null,frame);return;}"
            + "     if(m==='assertVisible'){e.preventDefault();e.stopPropagation();emitEl('assertVisible',raw,null,frame);disarm();return;}"
            + "     if(m==='assertText'){e.preventDefault();e.stopPropagation();emitEl('assertText',raw,((raw.innerText||raw.textContent||'').trim()).slice(0,80),frame);disarm();return;}"
            + "     if(m==='assertValue'){e.preventDefault();e.stopPropagation();emitEl('assertValue',raw,(raw.value!=null?raw.value:''),frame);disarm();return;}"
            + "     if(m==='hover'){e.preventDefault();e.stopPropagation();emitEl('hover',meaningful(raw),null,frame);disarm();return;}"
            + "     if(W.__ellPaused)return; emitEl('click',meaningful(raw),null,frame); },true);"
            + "   doc.addEventListener('dblclick',function(e){ var el=tgt(e); if(inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return; emitEl('doubleClick', meaningful(el), null, frame); },true);"
            + "   doc.addEventListener('change',function(e){ var el=tgt(e); if(!el||inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return;"
            + "     var tag=(el.tagName||'').toLowerCase(); if(tag==='select'){ var t=el.options[el.selectedIndex]?el.options[el.selectedIndex].text:el.value; emitEl('select',el,t,frame); }"
            + "     else if(tag==='input'||tag==='textarea'){ var ty=(el.getAttribute('type')||'').toLowerCase();"
            + "       if(ty==='file'){ emitEl('uploadFile', el, (el.value||'').split('\\\\').pop(), frame); return; }"
            + "       if(ty==='checkbox'||ty==='radio'||ty==='submit'||ty==='button'||ty==='image') return;"
            + "       inp(el,frame); } },true);"
            + "   doc.addEventListener('keydown',function(e){ if(e.key!=='Enter')return; var el=tgt(e); if(!el||inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return;"
            + "     var tag=(el.tagName||'').toLowerCase(); if(tag==='input'||tag==='textarea'){ inp(el,frame); emitEl('pressEnter',el,null,frame); } },true);"
            + "   doc.addEventListener('contextmenu',function(e){ var el=tgt(e); if(inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return; emitEl('rightClick', meaningful(el), null, frame); },true);"
            + "   doc.addEventListener('mousemove',function(e){ var m=W.__ellMode||'record'; if(m==='record'){ if(W.__ellHi){try{W.__ellHi.style.outline=W.__ellHiPrev||''}catch(x){} W.__ellHi=null;} return; }"
            + "     var el=tgt(e); if(inBar(el))return; if(W.__ellHi&&W.__ellHi!==el){try{W.__ellHi.style.outline=W.__ellHiPrev||''}catch(x){}}"
            + "     if(el&&el!==W.__ellHi){ W.__ellHi=el; W.__ellHiPrev=el.style.outline; try{el.style.outline='2px solid #0a84ff'}catch(x){} } },true); }"
            + " function walk(doc,path,depth){ if(!doc||depth>3)return; attach(doc,path); var fr=doc.querySelectorAll('iframe,frame');"
            + "   for(var i=0;i<fr.length&&i<12;i++){ try{ var cd=fr[i].contentDocument; if(cd) walk(cd,path.concat([i]),depth+1); }catch(e){} } }"
            + " walk(document,[],0);"
            + "})(arguments[0]);";

    private static final String OVERLAY_SCRIPT =
            "(function(){"
            + " if(window.__ellOverlayDone && document.getElementById('ellithium-recorder-toolbar')) return;"
            + " var __ex=document.querySelectorAll('#ellithium-recorder-toolbar'); for(var __i=1;__i<__ex.length;__i++)__ex[__i].remove(); if(__ex.length>=1){ window.__ellOverlayDone=true; return; }"
            + " var bar=document.createElement('div'); bar.id='ellithium-recorder-toolbar';"
            + " bar.style.cssText='position:fixed;top:10px;right:10px;z-index:2147483647;width:360px;max-height:80vh;"
            + "overflow:auto;background:rgba(20,20,20,0.95);color:#fff;padding:10px;border-radius:10px;"
            + "font-family:system-ui,sans-serif;font-size:12px;box-shadow:0 6px 20px rgba(0,0,0,0.5);';"
            + " function btn(id,label,title){ return '<button id=\"'+id+'\" title=\"'+title+'\" style=\"background:#444;color:#fff;border:0;border-radius:4px;padding:3px 7px;cursor:pointer\">'+label+'</button>'; }"
            + " bar.innerHTML='<div id=\"ell-head\" style=\"display:flex;align-items:center;gap:5px;flex-wrap:wrap;margin-bottom:8px;cursor:move\">'"
            + "  +'<span id=\"ell-dot\" style=\"width:10px;height:10px;background:#ff3b30;border-radius:50%;display:inline-block\"></span>'"
            + "  +'<b style=\"margin-right:auto\">Ellithium</b>'"
            + "  +btn('ell-rec','Pause','Pause/Resume recording')+btn('ell-pick','Inspect','Inspect / pick locator (toggle)')"
            + "  +btn('ell-hover','Hover','Record a hover on the next click')"
            + "  +btn('ell-av','Eye','Assert visible')+btn('ell-at','Aa','Assert text')+btn('ell-aval','Val','Assert value')"
            + "  +btn('ell-assert','Assert: soft','Toggle hard/soft asserts')+btn('ell-clear','Clear','Clear all steps')+btn('ell-stop','Stop','Stop and generate')+'</div>'"
            + "  +'<input id=\"ell-eval\" placeholder=\"Evaluate CSS or XPath...\" style=\"width:100%;box-sizing:border-box;background:#111;color:#fff;border:1px solid #333;border-radius:4px;padding:4px;margin-bottom:4px\">'"
            + "  +'<div id=\"ell-eval-count\" style=\"color:#888;margin-bottom:6px\">&nbsp;</div>'"
            + "  +'<div id=\"ell-picked\"></div><div id=\"ell-steps\"></div>'"
            + "  +'<div style=\"display:flex;align-items:center;gap:6px;margin-top:8px\"><b style=\"margin-right:auto\">Generated code</b>'+btn('ell-copy','Copy','Copy code')+'</div>'"
            + "  +'<pre id=\"ell-code\" style=\"background:#0b0b0b;border:1px solid #333;border-radius:4px;padding:6px;white-space:pre-wrap;word-break:break-word;max-height:30vh;overflow:auto;margin:4px 0 0\"></pre>';"
            + " document.body.appendChild(bar);"
            + " var style=document.createElement('style');"
            + " style.textContent='#ellithium-recorder-toolbar .ell-armed{background:#0a84ff!important} #ellithium-recorder-toolbar code{font-size:11px;word-break:break-all}';"
            + " document.head.appendChild(style);"
            + " function arm(id,mode){ document.getElementById(id).addEventListener('click', function(){ var on=window.__ellMode===mode;"
            + "   var a=document.querySelectorAll('.ell-armed'); for(var i=0;i<a.length;i++)a[i].classList.remove('ell-armed');"
            + "   window.__ellMode=on?'record':mode; if(!on) this.classList.add('ell-armed'); }); }"
            + " arm('ell-pick','inspect'); arm('ell-hover','hover'); arm('ell-av','assertVisible'); arm('ell-at','assertText'); arm('ell-aval','assertValue');"
            + " function logPush(o){ try{ var a=JSON.parse(localStorage.getItem('__ellRecLog')||'[]'); a.push(o); localStorage.setItem('__ellRecLog',JSON.stringify(a)); }catch(e){} }"
            + " (function(){ var rb=document.getElementById('ell-rec'); if(window.__ellPaused){ rb.textContent='Resume';"
            + "   document.getElementById('ell-dot').style.background='#888'; } })();"
            + " document.getElementById('ell-rec').addEventListener('click', function(){ window.__ellPaused=!window.__ellPaused;"
            + "   try{localStorage.setItem('__ellPaused', window.__ellPaused?'1':'0');}catch(e){}"
            + "   this.textContent=window.__ellPaused?'Resume':'Pause'; document.getElementById('ell-dot').style.background=window.__ellPaused?'#888':'#ff3b30'; });"
            + " document.getElementById('ell-clear').addEventListener('click', function(){ logPush({type:'clearAll'}); });"
            + " document.getElementById('ell-assert').addEventListener('click', function(){ logPush({type:'assertModeToggle'}); });"
            + " document.getElementById('ell-copy').addEventListener('click', function(){ var t=document.getElementById('ell-code').textContent;"
            + "   try{ navigator.clipboard.writeText(t); this.textContent='Copied'; var b=this; setTimeout(function(){b.textContent='Copy';},1200); }catch(e){} });"
            + " document.getElementById('ell-stop').addEventListener('click', function(){ window.__ellStop=true; });"
            + " function clearHi(){ if(window.__ellEvalHi){ for(var i=0;i<window.__ellEvalHi.length;i++){ try{window.__ellEvalHi[i].style.outline=''}catch(x){} } } window.__ellEvalHi=[]; }"
            + " document.getElementById('ell-eval').addEventListener('input', function(){ clearHi(); var q=this.value.trim();"
            + "   var cnt=document.getElementById('ell-eval-count'); if(!q){ cnt.textContent='\\u00a0'; return; } var els=[];"
            + "   try{ if(q.charAt(0)==='/'||q.charAt(0)==='('){ var r=document.evaluate(q,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);"
            + "     for(var i=0;i<r.snapshotLength;i++) els.push(r.snapshotItem(i)); } else { els=Array.prototype.slice.call(document.querySelectorAll(q)); } }"
            + "   catch(x){ cnt.textContent='invalid selector'; return; } window.__ellEvalHi=els;"
            + "   for(var j=0;j<els.length;j++){ try{els[j].style.outline='2px solid #30d158'}catch(x){} }"
            + "   cnt.textContent=els.length+' match'+(els.length===1?'':'es')+(els.length>1?' - not unique':''); });"
            + " ['keydown','keyup','keypress','input','paste'].forEach(function(ev){ document.getElementById('ell-eval').addEventListener(ev, function(e){ e.stopPropagation(); }, true); });"
            + " var head=document.getElementById('ell-head'); var drag=false, ox=0, oy=0;"
            + " head.addEventListener('mousedown', function(e){ if(e.target.tagName==='BUTTON'||e.target.tagName==='INPUT') return;"
            + "   drag=true; var r=bar.getBoundingClientRect(); ox=e.clientX-r.left; oy=e.clientY-r.top; bar.style.right='auto'; e.preventDefault(); });"
            + " document.addEventListener('mousemove', function(e){ if(!drag) return; bar.style.left=Math.max(0,e.clientX-ox)+'px'; bar.style.top=Math.max(0,e.clientY-oy)+'px'; });"
            + " document.addEventListener('mouseup', function(){ drag=false; });"
            + " window.__ellOverlayDone=true;"
            + "})();";

    private static final String RENDER_SCRIPT =
            "(function(json){"
            + " var data=JSON.parse(json); var steps=data.steps||[]; var picked=data.picked;"
            + " var ce=document.getElementById('ell-code'); if(ce) ce.textContent=data.code||'';"
            + " var ab=document.getElementById('ell-assert'); if(ab&&data.assertMode) ab.textContent='Assert: '+data.assertMode;"
            + " function row(c){ return '<div style=\"padding:1px 0\"><span style=\"color:'+(c.unique?'#30d158':'#ff9f0a')+'\">'"
            + "   +(c.unique?'\\u2713':'\\u26a0')+'</span> <code>'+c.expr.replace(/</g,'&lt;')+'</code> <span style=\"color:#888\">'+c.tier+(c.param?' param':'')+'</span></div>'; }"
            + " var pf=document.getElementById('ell-picked');"
            + " if(pf){ if(picked && picked.candidates && picked.candidates.length){ var ph='<div style=\"border:1px solid #0a84ff;border-radius:6px;padding:6px;margin-bottom:6px\"><b>Picked locator</b>';"
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
            + " function tdgOptions(){ var s='<option value=\"\">\\u2014 Use JSON value (manual)</option>'; for(var g=0;g<TDG_METHODS.length;g++){ s+='<optgroup label=\"'+TDG_METHODS[g].g+'\">'; for(var m=0;m<TDG_METHODS[g].m.length;m++) s+='<option value=\"'+TDG_METHODS[g].m[m]+'\">'+TDG_METHODS[g].m[m]+'()</option>'; s+='</optgroup>'; } return s; }"
            + " var isInput=function(a){ return a==='input'||a==='sendData'; };"
            + " for(var s=0;s<steps.length;s++){ var st=steps[s]; var fl=(st.frame&&st.frame.length)?(' [frame '+st.frame.join('>')+']'):'';"
            + "   var genBadge=st.generatorMethod?(' <span style=\"background:#1c3a1c;color:#30d158;border-radius:3px;padding:1px 4px;font-size:10px\">\\uD83C\\uDFB2 '+st.generatorMethod+'()</span>'):'(st.data?(\\' \\u2192 \\'+(\\'\\'+st.data).slice(0,40)):\\'\\');"
            + "   if(st.generatorMethod) genBadge=' <span style=\"background:#1c3a1c;color:#30d158;border-radius:3px;padding:1px 4px;font-size:10px\">\\uD83C\\uDFB2 '+st.generatorMethod+'()</span>';"
            + "   else genBadge=st.data?(' \\u2192 '+(''+st.data).slice(0,40)):'';"
            + "   var genBtn=isInput(st.action)?('<select class=\"ell-gen\" data-id=\"'+st.id+'\" title=\"Auto-generate test data\" style=\"margin-left:4px;background:#333;color:#fff;border:1px solid #555;border-radius:3px;font-size:10px;cursor:pointer\">'+tdgOptions()+'</select>'):'';"
            + "   html+='<div style=\"border-top:1px solid #333;padding:6px 0\"><div style=\"display:flex;align-items:center;color:#0a84ff;font-weight:bold\"><span>'+(s+1)+'. '+st.action+fl+genBadge+'</span>'+genBtn+'<button class=\"ell-del\" data-del=\"'+st.id+'\" title=\"Cancel step\" style=\"margin-left:4px;background:#633;color:#fff;border:0;border-radius:3px;cursor:pointer\">\\u2715</button></div>';"
            + "   for(var j=0;j<st.candidates.length;j++){ var c=st.candidates[j]; var sel=(j===st.chosenIndex);"
            + "     html+='<label style=\"display:flex;gap:6px;align-items:center;cursor:pointer\"><input type=\"radio\" name=\"ell-'+st.id+'\" '+(sel?'checked':'')+' data-id=\"'+st.id+'\" data-idx=\"'+j+'\">'"
            + "       +'<span style=\"color:'+(c.unique?'#30d158':'#ff9f0a')+'\">'+(c.unique?'\\u2713':'\\u26a0')+'</span><code>'+c.expr.replace(/</g,'&lt;')+'</code>'"
            + "       +'<span style=\"color:#888\">'+c.tier+(c.param?' \\u00b7 param':'')+'</span></label>'; }"
            + "   html+='</div>'; }"
            + " host.innerHTML=html;"
            + " function push(o){ var a=JSON.parse(localStorage.getItem('__ellRecLog')||'[]'); a.push(o); localStorage.setItem('__ellRecLog', JSON.stringify(a)); }"
            + " var gens=host.querySelectorAll('.ell-gen');"
            + " for(var gi=0;gi<gens.length;gi++){ (function(sel){ var sid=sel.getAttribute('data-id');"
            + "   var cur=null; for(var si=0;si<steps.length;si++){ if(steps[si].id===sid){ cur=steps[si].generatorMethod||''; break; } }"
            + "   sel.value=cur||''; sel.addEventListener('change',function(){ push({type:'autoGenerate',id:sid,method:this.value||null}); }); })(gens[gi]); }"
            + " var radios=host.querySelectorAll('input[type=radio]');"
            + " for(var k=0;k<radios.length;k++){ radios[k].addEventListener('change', function(){"
            + "   push({type:'override', id:this.getAttribute('data-id'), index:parseInt(this.getAttribute('data-idx'))}); }); }"
            + " var dels=host.querySelectorAll('.ell-del');"
            + " for(var di=0;di<dels.length;di++){ dels[di].addEventListener('click', function(){ push({type:'delete', id:this.getAttribute('data-del')}); }); }"
            + "})(arguments[0]);";
}
