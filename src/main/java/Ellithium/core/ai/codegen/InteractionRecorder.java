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
    private static volatile int knownHandleCount = 0;
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
        try { knownHandleCount = d.getWindowHandles().size(); } catch (Exception e) { knownHandleCount = 1; }
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
        seedBaselines(snapshot);
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
        if ("delete".equals(type)) {
            String id = str(ev.get("id"));
            RecordedStep removed = BY_ID.remove(id);
            if (removed != null) { STEPS.remove(removed); return true; }
            return false;
        }
        String id = str(ev.get("id"));
        if (id == null) return false;
        List<LocatorCandidate> candidates = buildCandidates(ev.get("candidates"));
        if ("inspect".equals(type)) {
            lastPicked = candidates;
            return true;
        }
        List<Integer> frame = frameChainOf(ev.get("frame"));
        RecordedStep step = new RecordedStep(id, type, str(ev.get("value")),
                str(ev.get("tag")), str(ev.get("name")), candidates, frame);
        if (frame.isEmpty()) seedOne(candidates);
        STEPS.add(step);
        BY_ID.put(id, step);
        if ("click".equals(type) || "select".equals(type) || "input".equals(type)) {
            navHintEpoch = System.currentTimeMillis();
        }
        return true;
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

    private static void seedBaselines(List<RecordedStep> steps) {
        for (RecordedStep step : steps) {
            if (!step.getFrameChain().isEmpty()) continue;
            LocatorCandidate c = step.chosen();
            if (c == null) continue;
            try {
                List<WebElement> els = driver.findElements(c.by());
                if (els.size() == 1) BaselineStore.capture(driver, c.by(), els.get(0));
            } catch (Exception ignored) {}
        }
    }

    private static void reinjectOnNavigation() {
        String url = currentUrl();
        if (url == null || url.equals(lastUrl)) return;
        boolean firstReal = isBlankUrl(startUrl) && !isBlankUrl(url);
        boolean actionInduced = (System.currentTimeMillis() - navHintEpoch) <= NAV_HINT_TTL;
        lastUrl = url;
        if (firstReal) {
            startUrl = url;
        } else if (!actionInduced) {
            STEPS.add(new RecordedStep("nav-" + System.currentTimeMillis(), "navigate", url, null, null, List.of()));
        }
        navHintEpoch = 0L;
        ensureInjected();
        render();
    }

    private static boolean isBlankUrl(String u) {
        return u == null || u.isBlank() || u.startsWith("about:") || u.startsWith("data:") || u.startsWith("chrome:");
    }

    private static void checkNewTabs() {
        if (driver == null) return;
        try {
            java.util.Set<String> handles = driver.getWindowHandles();
            if (handles.size() > knownHandleCount) {
                knownHandleCount = handles.size();
                String newest = null;
                for (String h : handles) newest = h;
                if (newest != null) {
                    driver.switchTo().window(newest);
                    lastUrl = currentUrl();
                    navHintEpoch = 0L;
                    STEPS.add(new RecordedStep("tab-" + System.currentTimeMillis(), "navigate",
                            lastUrl, null, null, List.of()));
                    ensureInjected();
                    render();
                }
            } else if (handles.size() < knownHandleCount) {
                knownHandleCount = handles.size();
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
        try { js.executeScript("localStorage.setItem('__ellRecLog','[]');"); } catch (Exception ignored) {}
    }

    private static void render() {
        if (!(driver instanceof JavascriptExecutor js)) return;
        try { js.executeScript(RENDER_SCRIPT, renderJson()); } catch (Exception ignored) {}
    }

    private static void removeOverlay() {
        if (!(driver instanceof JavascriptExecutor js)) return;
        try {
            js.executeScript("var b=document.getElementById('ellithium-recorder-toolbar');if(b)b.remove();");
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
        try {
            List<RecordedStep> snap = new ArrayList<>(STEPS);
            String code = options.isTest()
                    ? PomCodeEmitter.previewTestSource(snap, CodegenCli.deriveClassName(startUrl),
                        options.packageName(), startUrl, options.browser())
                    : PomCodeEmitter.previewSource(snap, "RecordedPage", options.packageName());
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
            + "   W.__ellPaused=false; W.__ellLastVal=new WeakMap(); }"
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
            + "   if(c.id || (c.getAttribute&&(c.getAttribute('data-testid')||c.getAttribute('name')||c.getAttribute('aria-label')||c.getAttribute('role'))) || ['a','button','input','select','textarea','summary','label'].indexOf(t)>=0) return c;"
            + "   c=c.parentElement; n++; } return el; }"
            + " function cands(el){ var d=el.ownerDocument, o=[]; function add(type,sel,value,tier,uniq,param){ o.push({type:type,sel:sel,value:value,tier:tier,unique:uniq,param:param}); }"
            + "   ['data-testid','data-test','data-cy','data-qa'].forEach(function(a){ var v=el.getAttribute&&el.getAttribute(a); if(v){var s=asel(a,v); add('css',s,v,a,uCss(d,s),dyn(v));} });"
            + "   if(el.id){ var s=asel('id',el.id); add('id',s,el.id,'id',uCss(d,s),dyn(el.id)); }"
            + "   var nm=el.getAttribute&&el.getAttribute('name'); if(nm){ var s2=asel('name',nm); add('name',s2,nm,'name',uCss(d,s2),dyn(nm)); }"
            + "   var al=el.getAttribute&&el.getAttribute('aria-label'); if(al){ var s3=asel('aria-label',al); add('css',s3,al,'aria-label',uCss(d,s3),false); }"
            + "   var role=el.getAttribute&&el.getAttribute('role'); var tx=(el.textContent||'').trim();"
            + "   if(tx&&tx.length<=80){ if(role){ var xr=\"//*[@role='\"+role+\"' and normalize-space(.)=\"+lit(tx)+\"]\"; add('xpath',xr,tx,'role-text',uXp(d,xr),false); }"
            + "     var tg=el.tagName.toLowerCase(); if(['a','button','label','summary'].indexOf(tg)>=0){ var xt='//'+tg+'[normalize-space(.)='+lit(tx)+']'; add('xpath',xt,tx,'text',uXp(d,xt),false); } }"
            + "   if(el.attributes){ for(var i=0;i<el.attributes.length;i++){ var at=el.attributes[i];"
            + "     if(at.name.indexOf('data-')===0 && ['data-testid','data-test','data-cy','data-qa'].indexOf(at.name)<0 && at.value){ var s4=asel(at.name,at.value); add('css',s4,at.value,at.name,uCss(d,s4),dyn(at.value)); } } }"
            + "   var tgn=el.tagName?el.tagName.toLowerCase():'';"
            + "   if(tgn==='a'){ var ltx=(el.textContent||'').trim(); if(ltx&&ltx.length<=80){ add('linkText',null,ltx,'link-text',uXp(d,'//a[normalize-space(.)='+lit(ltx)+']'),false); add('partialLinkText',null,ltx,'partial-link-text',false,false); } }"
            + "   var cl=el.getAttribute&&el.getAttribute('class'); if(cl){ var toks=cl.trim().split(/\\s+/); var fc=null; for(var ci=0;ci<toks.length;ci++){ if(toks[ci]&&!dyn(toks[ci])){fc=toks[ci];break;} } if(fc) add('className',null,fc,'class-name',uCls(d,fc),false); }"
            + "   if(tgn) add('tagName',null,tgn,'tag-name',uCss(d,tgn),false);"
            + "   var cp=cpath(el); if(cp) add('css',cp,cp,'css-path',uCss(d,cp),false); return o; }"
            + " function inBar(el){ return el&&(el.id==='ellithium-recorder-toolbar'||(el.closest&&el.closest('#ellithium-recorder-toolbar'))); }"
            + " function tgt(e){ var p=e.composedPath&&e.composedPath(); return (p&&p.length)?p[0]:e.target; }"
            + " function emitEl(type,el,value,frame){ if(inBar(el))return; var nm=(el.getAttribute&&(el.getAttribute('aria-label')||el.getAttribute('name')||el.getAttribute('placeholder')))||(el.textContent||'').trim().slice(0,40);"
            + "   emit({id:nid(),type:type,tag:(el.tagName||'').toLowerCase(),name:nm,value:(value==null?null:value),frame:frame,candidates:cands(el)}); }"
            + " function disarm(){ W.__ellMode='record'; var a=document.querySelectorAll('.ell-armed'); for(var i=0;i<a.length;i++)a[i].classList.remove('ell-armed'); }"
            + " function inp(el,frame){ var v=el.value; if(W.__ellLastVal.get(el)===v)return; W.__ellLastVal.set(el,v); emitEl('input',el,v,frame); }"
            + " function attach(doc,frame){ if(!doc||doc.__ellAttached)return; doc.__ellAttached=true;"
            + "   doc.addEventListener('click',function(e){ var raw=tgt(e); if(inBar(raw))return; var m=W.__ellMode||'record';"
            + "     if(m==='inspect'){e.preventDefault();e.stopPropagation();emitEl('inspect',raw,null,frame);return;}"
            + "     if(m==='assertVisible'){e.preventDefault();e.stopPropagation();emitEl('assertVisible',raw,null,frame);disarm();return;}"
            + "     if(m==='assertText'){e.preventDefault();e.stopPropagation();emitEl('assertText',raw,(raw.textContent||'').replace(/\\s+/g,' ').trim().slice(0,80),frame);disarm();return;}"
            + "     if(m==='assertValue'){e.preventDefault();e.stopPropagation();emitEl('assertValue',raw,(raw.value!=null?raw.value:''),frame);disarm();return;}"
            + "     if(W.__ellPaused)return; emitEl('click',meaningful(raw),null,frame); },true);"
            + "   doc.addEventListener('change',function(e){ var el=tgt(e); if(!el||inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return;"
            + "     var tag=(el.tagName||'').toLowerCase(); if(tag==='select'){ var t=el.options[el.selectedIndex]?el.options[el.selectedIndex].text:el.value; emitEl('select',el,t,frame); }"
            + "     else if(tag==='input'||tag==='textarea'){ inp(el,frame); } },true);"
            + "   doc.addEventListener('keydown',function(e){ if(e.key!=='Enter')return; var el=tgt(e); if(!el||inBar(el))return; if(W.__ellPaused||(W.__ellMode||'record')!=='record')return;"
            + "     var tag=(el.tagName||'').toLowerCase(); if(tag==='input'||tag==='textarea') inp(el,frame); },true);"
            + "   doc.addEventListener('mousemove',function(e){ var m=W.__ellMode||'record'; if(m==='record'){ if(W.__ellHi){try{W.__ellHi.style.outline=W.__ellHiPrev||''}catch(x){} W.__ellHi=null;} return; }"
            + "     var el=tgt(e); if(inBar(el))return; if(W.__ellHi&&W.__ellHi!==el){try{W.__ellHi.style.outline=W.__ellHiPrev||''}catch(x){}}"
            + "     if(el&&el!==W.__ellHi){ W.__ellHi=el; W.__ellHiPrev=el.style.outline; try{el.style.outline='2px solid #0a84ff'}catch(x){} } },true); }"
            + " function walk(doc,path,depth){ if(!doc||depth>3)return; attach(doc,path); var fr=doc.querySelectorAll('iframe,frame');"
            + "   for(var i=0;i<fr.length&&i<12;i++){ try{ var cd=fr[i].contentDocument; if(cd) walk(cd,path.concat([i]),depth+1); }catch(e){} } }"
            + " walk(document,[],0);"
            + "})(arguments[0]);";

    private static final String OVERLAY_SCRIPT =
            "(function(){"
            + " if (document.getElementById('ellithium-recorder-toolbar')) return;"
            + " var bar=document.createElement('div'); bar.id='ellithium-recorder-toolbar';"
            + " bar.style.cssText='position:fixed;top:10px;right:10px;z-index:2147483647;width:360px;max-height:80vh;"
            + "overflow:auto;background:rgba(20,20,20,0.95);color:#fff;padding:10px;border-radius:10px;"
            + "font-family:system-ui,sans-serif;font-size:12px;box-shadow:0 6px 20px rgba(0,0,0,0.5);';"
            + " function btn(id,label,title){ return '<button id=\"'+id+'\" title=\"'+title+'\" style=\"background:#444;color:#fff;border:0;border-radius:4px;padding:3px 7px;cursor:pointer\">'+label+'</button>'; }"
            + " bar.innerHTML='<div id=\"ell-head\" style=\"display:flex;align-items:center;gap:5px;flex-wrap:wrap;margin-bottom:8px;cursor:move\">'"
            + "  +'<span id=\"ell-dot\" style=\"width:10px;height:10px;background:#ff3b30;border-radius:50%;display:inline-block\"></span>'"
            + "  +'<b style=\"margin-right:auto\">Ellithium</b>'"
            + "  +btn('ell-rec','Pause','Pause/Resume recording')+btn('ell-pick','Inspect','Inspect / pick locator (toggle)')"
            + "  +btn('ell-av','Eye','Assert visible')+btn('ell-at','Aa','Assert text')+btn('ell-aval','Val','Assert value')"
            + "  +btn('ell-clear','Clear','Clear all steps')+btn('ell-stop','Stop','Stop and generate')+'</div>'"
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
            + " arm('ell-pick','inspect'); arm('ell-av','assertVisible'); arm('ell-at','assertText'); arm('ell-aval','assertValue');"
            + " function logPush(o){ try{ var a=JSON.parse(localStorage.getItem('__ellRecLog')||'[]'); a.push(o); localStorage.setItem('__ellRecLog',JSON.stringify(a)); }catch(e){} }"
            + " document.getElementById('ell-rec').addEventListener('click', function(){ window.__ellPaused=!window.__ellPaused;"
            + "   this.textContent=window.__ellPaused?'Resume':'Pause'; document.getElementById('ell-dot').style.background=window.__ellPaused?'#888':'#ff3b30'; });"
            + " document.getElementById('ell-clear').addEventListener('click', function(){ logPush({type:'clearAll'}); });"
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
            + " var head=document.getElementById('ell-head'); var drag=false, ox=0, oy=0;"
            + " head.addEventListener('mousedown', function(e){ if(e.target.tagName==='BUTTON'||e.target.tagName==='INPUT') return;"
            + "   drag=true; var r=bar.getBoundingClientRect(); ox=e.clientX-r.left; oy=e.clientY-r.top; bar.style.right='auto'; e.preventDefault(); });"
            + " document.addEventListener('mousemove', function(e){ if(!drag) return; bar.style.left=Math.max(0,e.clientX-ox)+'px'; bar.style.top=Math.max(0,e.clientY-oy)+'px'; });"
            + " document.addEventListener('mouseup', function(){ drag=false; });"
            + "})();";

    private static final String RENDER_SCRIPT =
            "(function(json){"
            + " var data=JSON.parse(json); var steps=data.steps||[]; var picked=data.picked;"
            + " var ce=document.getElementById('ell-code'); if(ce) ce.textContent=data.code||'';"
            + " function row(c){ return '<div style=\"padding:1px 0\"><span style=\"color:'+(c.unique?'#30d158':'#ff9f0a')+'\">'"
            + "   +(c.unique?'\\u2713':'\\u26a0')+'</span> <code>'+c.expr.replace(/</g,'&lt;')+'</code> <span style=\"color:#888\">'+c.tier+(c.param?' param':'')+'</span></div>'; }"
            + " var pf=document.getElementById('ell-picked');"
            + " if(pf){ if(picked && picked.candidates && picked.candidates.length){ var ph='<div style=\"border:1px solid #0a84ff;border-radius:6px;padding:6px;margin-bottom:6px\"><b>Picked locator</b>';"
            + "   for(var i=0;i<picked.candidates.length;i++) ph+=row(picked.candidates[i]); pf.innerHTML=ph+'</div>'; } else pf.innerHTML=''; }"
            + " var host=document.getElementById('ell-steps'); if(!host) return; var html='';"
            + " for(var s=0;s<steps.length;s++){ var st=steps[s]; var fl=(st.frame&&st.frame.length)?(' [frame '+st.frame.join('>')+']'):'';"
            + "   html+='<div style=\"border-top:1px solid #333;padding:6px 0\"><div style=\"display:flex;color:#0a84ff;font-weight:bold\"><span>'+(s+1)+'. '+st.action+fl+(st.data?(' \\u2192 '+(''+st.data).slice(0,40)):'')+'</span><button class=\"ell-del\" data-del=\"'+st.id+'\" title=\"Cancel step\" style=\"margin-left:auto;background:#633;color:#fff;border:0;border-radius:3px;cursor:pointer\">\\u2715</button></div>';"
            + "   for(var j=0;j<st.candidates.length;j++){ var c=st.candidates[j]; var sel=(j===st.chosenIndex);"
            + "     html+='<label style=\"display:flex;gap:6px;align-items:center;cursor:pointer\"><input type=\"radio\" name=\"ell-'+st.id+'\" '+(sel?'checked':'')+' data-id=\"'+st.id+'\" data-idx=\"'+j+'\">'"
            + "       +'<span style=\"color:'+(c.unique?'#30d158':'#ff9f0a')+'\">'+(c.unique?'\\u2713':'\\u26a0')+'</span><code>'+c.expr.replace(/</g,'&lt;')+'</code>'"
            + "       +'<span style=\"color:#888\">'+c.tier+(c.param?' \\u00b7 param':'')+'</span></label>'; }"
            + "   html+='</div>'; }"
            + " host.innerHTML=html;"
            + " function push(o){ var a=JSON.parse(localStorage.getItem('__ellRecLog')||'[]'); a.push(o); localStorage.setItem('__ellRecLog', JSON.stringify(a)); }"
            + " var radios=host.querySelectorAll('input[type=radio]');"
            + " for(var k=0;k<radios.length;k++){ radios[k].addEventListener('change', function(){"
            + "   push({type:'override', id:this.getAttribute('data-id'), index:parseInt(this.getAttribute('data-idx'))}); }); }"
            + " var dels=host.querySelectorAll('.ell-del');"
            + " for(var di=0;di<dels.length;di++){ dels[di].addEventListener('click', function(){ push({type:'delete', id:this.getAttribute('data-del')}); }); }"
            + "})(arguments[0]);";
}
