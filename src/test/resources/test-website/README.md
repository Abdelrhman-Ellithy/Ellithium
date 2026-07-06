# Ellithium Testing Arena — Agent & Developer Guide

> **Location:** `src/test/resources/test-website/`  
> **Entry point:** `index.html`  
> **Purpose:** A multi-page, self-contained HTML/JS/CSS minefield designed to stress-test every action wrapper in the Ellithium Selenium framework against real browser mechanics — real timing delays, real DOM mutations, real exceptions. No mocks, no shortcuts.

---

## Site Map

| File | Module | Primary Test Classes |
|------|--------|---------------------|
| `index.html` | Home / Element ID Reference | — (navigation hub) |
| `waits.html` | Wait & Rendering Traps | `WaitActionsTest` |
| `state.html` | State & Interactivity Traps | `InteractionRecoveryTest` |
| `stale.html` | Stale Element & Shadow DOM | `WaitActionsTest`, `InteractionRecoveryTest` |
| `frames.html` | Frames & Windows | `FrameActionsTest`, `WindowActionsTest` |
| `forms.html` | Complex Forms & Input Variety | `SelectActionsTest`, `JavaScriptActionsTest` |
| `mouse.html` | Mouse & Keyboard Actions | `MouseActionsTest` |
| `alerts.html` | Alerts & Modals | `AlertActionsTest` |
| `navigation.html` | Navigation | `NavigationActionsTest`, `WaitActionsTest` |
| `cookies.html` | Cookies & Web Storage | `CookieActionsTest`, `JavaScriptActionsTest` |

---

## How to Serve the Site

The arena is **fully self-contained** — no build step, no server required. Open any page via a `file://` URI or serve locally:

```bash
# Option A: Direct file URI (simplest — works out of the box)
String arenaPath = new File("src/test/resources/test-website/index.html").getAbsolutePath();
driver.get("file:///" + arenaPath.replace("\\", "/"));

# Option B: Serve with Python (if Blob URLs or CORS cause issues)
cd src/test/resources/test-website
python -m http.server 8080
# Then navigate to: http://localhost:8080/index.html
```

Store the base URL in a constant and concatenate the page filename:
```java
private static final String BASE_URL = "file:///C:/path/to/test-website/";
driver.get(BASE_URL + "waits.html");
```

---

## Shared Assets

| File | Purpose |
|------|---------|
| `_shared.css` | Complete design system — CSS variables, cards, buttons, forms, tables, status indicators |
| `_shared.js` | `ArenaLog` console logger, `setStatus()`, `ts()` timestamp, `randInt()` helpers |

Both files are auto-linked on every page. Zero external CDN dependencies.

---

## Page-by-Page Reference

### `waits.html` — Wait & Rendering Traps

**Maps to:** `WaitActionsTest`

| Element ID | Type | Selenium Wait Method | Timing |
|---|---|---|---|
| `trigger-spinner-btn` | button | `waitForElementToBeClickable` | Immediate |
| `injected-form` | div | `waitForElementPresence` | **5 seconds** after click |
| `injected-first-name` | input | `waitForElementToBeVisible` | Inside injected form |
| `injected-submit-btn` | button | `waitForElementToBeClickable` | Inside injected form |
| `delayed-el-1/2/3` | div | `waitForElementToBeVisible` | Random **2–7 seconds** each |
| `polling-counter` | div | `waitForTextToBePresentInElement` | Counts 0→10, then text="DONE" |
| `lazy-image` | img | `waitForElementAttributeToBe(src, value)` | src set after **4 seconds** |
| `dyn-items-container` | div | `waitForNumberOfElementsToBeMoreThan` | +1 item per second |
| `auto-toggle-cb` | checkbox | `waitForElementSelectionStateToBe` | Auto-toggles after **3 seconds** |
| `enable-me-btn` | button | `waitForElementToBeEnabled` | Disabled → enabled after **5 seconds** |
| `attr-target-el` | div | `waitForElementAttributeToBe(data-state, value)` | Changes on click |

```java
driver.get(BASE_URL + "waits.html");

// Trigger spinner, then wait for injected form (5s delay)
driver.findElement(By.id("trigger-spinner-btn")).click();
waitActions.waitForElementPresence(By.id("injected-form"), 10, 500);
waitActions.waitForElementToBeVisible(By.id("injected-first-name"), 10, 500);
driver.findElement(By.id("injected-first-name")).sendKeys("Ellithium");

// Counter polling — wait for text "DONE"
driver.findElement(By.id("start-counter-btn")).click();
waitActions.waitForTextToBePresentInElement(By.id("polling-counter"), "DONE", 15, 500);

// Lazy image src attribute
driver.findElement(By.id("trigger-lazy-btn")).click();
waitActions.waitForElementAttributeContains(By.id("lazy-image"), "src", "data:", 8, 500);
```

---

### `state.html` — State & Interactivity Traps

**Maps to:** `InteractionRecoveryTest`

| Element ID | Exception Triggered | Recovery Method |
|---|---|---|
| `covered-btn` | `ElementClickInterceptedException` | Remove overlay or `jsClick()` |
| `z-index-overlay` | Blocking div on top of button | `driver.findElement(By.id("z-index-overlay"))` remove |
| `disabled-input` | `InvalidElementStateException` on `sendKeys` | Unlock via `unlock-cb` first |
| `readonly-input` | `InvalidElementStateException` on `clear()` | — |
| `offscreen-element` | `ElementNotInteractableException` | `jsClick()` or scroll |
| `mutating-el` | Race condition on attribute | `waitForElementAttributeToBe(data-state, ...)` |
| `candidate-0..4` | All except #3 are blocked | `InteractionRecovery.resolveInteractable` |
| `js-only-btn-1/2` | Pointer-events intercepted by overlay | `InteractionRecovery.jsClick(element)` |
| `arm-alert-trap-btn` | Alert fires 2s after click | `InteractionRecovery.handleUnexpectedAlert()` |
| `inert-container` | `inert` attribute blocks all children | — |

```java
driver.get(BASE_URL + "state.html");

// JS click to bypass z-index overlay
WebElement btn = driver.findElement(By.id("covered-btn"));
((JavascriptExecutor)driver).executeScript("arguments[0].click();", btn);

// resolveInteractable — finds candidate-3 (only interactable one)
WebElement interactable = recovery.resolveInteractable(By.cssSelector(".add"), 5, 200);
assertNotNull(interactable);
interactable.click();

// Unlock disabled field
driver.findElement(By.id("unlock-cb")).click();
driver.findElement(By.id("disabled-input")).sendKeys("Now editable");
```

---

### `stale.html` — Stale Element & Shadow DOM

**Maps to:** `WaitActionsTest` (waitForElementStaleness), `InteractionRecoveryTest` (recoverContext)

| Element ID | Trap | Exception |
|---|---|---|
| `stale-tbody` | Destroyed + recreated every 2s and on every refresh | `StaleElementReferenceException` |
| `staleness-target` | Detaches after **3 seconds**, new element injected | `StaleElementReferenceException` |
| `shadow-host` | Open Shadow DOM (`mode:'open'`) | Requires `.shadowRoot` + JS executor |

```java
driver.get(BASE_URL + "stale.html");

// waitForElementStaleness — capture ref, trigger detach, wait
WebElement el = driver.findElement(By.id("staleness-target"));
driver.findElement(By.id("trigger-staleness-btn")).click();
waitActions.waitForElementStaleness(el, 8, 300);

// Shadow DOM piercing
WebElement host = driver.findElement(By.id("shadow-host"));
SearchContext shadowRoot = (SearchContext)
    ((JavascriptExecutor)driver).executeScript("return arguments[0].shadowRoot", host);
WebElement shadowInput = shadowRoot.findElement(By.id("shadow-input"));
shadowInput.sendKeys("Shadow DOM text");

// Always re-fetch after table refresh (never reuse old refs)
driver.findElement(By.id("refresh-table-btn")).click();
WebElement freshRow = driver.findElement(By.id("table-row-0")); // new findElement
```

---

### `frames.html` — Frames & Windows

**Maps to:** `FrameActionsTest`, `WindowActionsTest`

| Element ID | Trap | Timing |
|---|---|---|
| `dynamic-iframe` | Not in DOM at page load | Injected after **3 seconds** |
| `iframe-username` | Inside `dynamic-iframe` | Switch required |
| `iframe-password` | Inside `dynamic-iframe` | Switch required |
| `iframe-login-btn` | Inside `dynamic-iframe` | Switch required |
| `outer-nested-frame` | Level-1 nested iframe | Inject via button |
| `deep-input` | Level-2 nested iframe | 2× `switchTo().frame()` required |

```java
driver.get(BASE_URL + "frames.html");

// Delayed iframe — wait before switching (3s injection delay)
waitActions.waitForFrameToBeAvailableAndSwitchToIt(By.id("dynamic-iframe"), 10, 500);
driver.findElement(By.id("iframe-username")).sendKeys("admin");
driver.findElement(By.id("iframe-password")).sendKeys("secret");
driver.findElement(By.id("iframe-login-btn")).click();
driver.switchTo().defaultContent(); // mandatory exit

// Window handling
String original = driver.getWindowHandle();
driver.findElement(By.id("open-blank-window-btn")).click();
windowActions.waitForNumberOfWindowsToBe(2, 5, 300);
String newHandle = windowActions.getAllWindowHandles().stream()
    .filter(h -> !h.equals(original)).findFirst().get();
driver.switchTo().window(newHandle);
// ... interact ...
driver.close();
driver.switchTo().window(original);

// Nested frames
driver.findElement(By.id("inject-nested-btn")).click();
driver.switchTo().frame("outer-frame");
driver.switchTo().frame("inner-frame");
driver.findElement(By.id("deep-input")).sendKeys("Deep text");
driver.switchTo().defaultContent();
```

---

### `forms.html` — Complex Forms

**Maps to:** `SelectActionsTest`, `JavaScriptActionsTest`

| Element ID | Type | Interaction Method |
|---|---|---|
| `native-select` | `<select>` single | `SelectActions.selectDropdownByText/Value/Index` |
| `multi-select` | `<select multiple>` | `SelectActions` multiple calls + `deselectAll` |
| `native-select-grouped` | `<select>` + `<optgroup>` | `SelectActions.selectDropdownByText` |
| `custom-dd` | Custom div dropdown | Click `custom-dd-trigger` → click option (NOT SelectActions) |
| `file-upload` | `input[type=file]` | `element.sendKeys(absolutePath)` or `uploadFileUsingJS` |
| `full-form` | form | Submit → `waitForElementAttributeToBe(form, "data-submitted", "true")` |
| `content-editable` | contenteditable div | `Keys.chord(Keys.CONTROL,"a")` + `sendKeys(text)` |
| `js-set-value-target` | text input | `JavaScriptActions.setElementValueUsingJS(By.id(...), value)` |

```java
driver.get(BASE_URL + "forms.html");

// SelectActions
selectActions.selectDropdownByText(By.id("native-select"), "Selenium WebDriver");
selectActions.selectDropdownByValue(By.id("native-select"), "playwright");
selectActions.selectDropdownByIndex(By.id("native-select"), 3);
List<String> selected = selectActions.getDropdownSelectedOptions(By.id("multi-select"));
selectActions.deselectAll(By.id("multi-select"));

// Custom dropdown (NOT SelectActions — requires manual steps)
driver.findElement(By.id("custom-dd-trigger")).click();
driver.findElement(By.id("dd-blink")).click();

// File upload
driver.findElement(By.id("file-upload")).sendKeys("C:\\path\\to\\testfile.txt");

// Form submit + attribute assertion
driver.findElement(By.id("form-name")).sendKeys("Jane Doe");
driver.findElement(By.id("form-email")).sendKeys("jane@example.com");
driver.findElement(By.id("form-submit-btn")).click();
waitActions.waitForElementAttributeToBe(By.id("full-form"), "data-submitted", "true", 5, 200);
```

---

### `mouse.html` — Mouse & Keyboard

**Maps to:** `MouseActionsTest`

| Element ID | MouseActions Method | Notes |
|---|---|---|
| `hover-menu-trigger` | `hoverOverElement` | Must hover before sub-links are visible |
| `hover-smoke/regression/integration/performance` | click after hover | Invisible without hover |
| `dbl-click-target` | `doubleClick` | Single click does nothing |
| `right-click-target` | `rightClick` | Opens custom DOM context menu |
| `drag-source/2/3` | `dragAndDrop` source | HTML5 draggable |
| `drop-target-a/b` | `dragAndDrop` target | Drop zones |
| `keyboard-input` | `sendKeys`, `Keys.chord` | Logs all keypresses |
| `scroll-target-btn` | `scrollByOffset` / `scrollIntoView` | Hidden below scroll container |
| `hold-target` | `clickAndHold` + `pause(2000)` + `release` | 2s hold required |

```java
driver.get(BASE_URL + "mouse.html");
Actions actions = new Actions(driver);

// Hover then click sub-menu link
actions.moveToElement(driver.findElement(By.id("hover-menu-trigger"))).perform();
waitActions.waitForElementToBeVisible(By.id("hover-submenu"), 3, 200);
driver.findElement(By.id("hover-smoke")).click();

// Double-click
mouseActions.doubleClick(By.id("dbl-click-target"), 5, 200);

// Right-click
mouseActions.rightClick(By.id("right-click-target"), 5, 200);

// Drag and drop
mouseActions.dragAndDrop(By.id("drag-source"), By.id("drop-target-a"), 5, 200);

// Scroll then click
((JavascriptExecutor)driver).executeScript(
    "arguments[0].scrollIntoView(true);",
    driver.findElement(By.id("scroll-target-btn")));
driver.findElement(By.id("scroll-target-btn")).click();
```

---

### `alerts.html` — Alerts & Modals

**Maps to:** `AlertActionsTest`

| Element ID | Alert Type | Timing | Known Text |
|---|---|---|---|
| `trigger-alert-btn` | `window.alert()` | Immediate | `ELLITHIUM_SIMPLE_ALERT` (in message) |
| `trigger-confirm-btn` | `window.confirm()` | Immediate | `ELLITHIUM_CONFIRM` |
| `trigger-prompt-btn` | `window.prompt()` | Immediate | `ELLITHIUM_PROMPT` |
| `trigger-delayed-alert-btn` | `window.confirm()` | **3-second delay** | `ELLITHIUM_DELAYED_CONFIRM` |
| `trigger-delayed-prompt-btn` | `window.prompt()` | **3-second delay** | `ELLITHIUM_DELAYED_PROMPT` |
| `blur-alert-input` | `window.alert()` on blur | On tab-out | `ELLITHIUM_BLUR_ALERT` |
| `alert-text-a-btn` | `window.alert()` | Immediate | **`ELLITHIUM_ALERT_A`** |
| `alert-text-b-btn` | `window.alert()` | Immediate | **`ELLITHIUM_CONFIRM_B`** |
| `alert-text-c-btn` | `window.alert()` | Immediate | **`ELLITHIUM_PROMPT_C`** |
| `open-modal-btn` | DOM modal (not browser dialog) | Immediate | — |
| `modal-input` | Input inside DOM modal | — | — |

```java
driver.get(BASE_URL + "alerts.html");

// Immediate alert
driver.findElement(By.id("trigger-alert-btn")).click();
assertEquals(alertActions.getText(5, 200), ...);
alertActions.accept(5, 200);

// Confirm — dismiss
driver.findElement(By.id("trigger-confirm-btn")).click();
alertActions.dismiss(5, 200);

// Prompt — sendData then accept
driver.findElement(By.id("trigger-prompt-btn")).click();
alertActions.sendData("MyTestInput", 5, 200);
alertActions.accept(5, 200);

// 3-second delayed — must use waitForAlertPresence
driver.findElement(By.id("trigger-delayed-alert-btn")).click();
waitActions.waitForAlertPresence(By.id("trigger-delayed-alert-btn"), 8, 500);
alertActions.accept(5, 200);

// Known text assertions
driver.findElement(By.id("alert-text-a-btn")).click();
assertEquals(alertActions.getText(3, 200), "ELLITHIUM_ALERT_A");
alertActions.accept(3, 200);

// DOM modal (NOT a browser dialog — interact with elements inside)
driver.findElement(By.id("open-modal-btn")).click();
waitActions.waitForElementToBeVisible(By.id("custom-modal"), 3, 200);
driver.findElement(By.id("modal-input")).sendKeys("Test Data");
driver.findElement(By.id("modal-confirm-btn")).click();
```

---

### `navigation.html` — Navigation

**Maps to:** `NavigationActionsTest`, `WaitActionsTest`

| Element ID | Action | Wait Method |
|---|---|---|
| `navigate-to-btn` | Navigates to selected page | `waitForUrlContains` |
| `nav-back-btn` | Browser back | `waitForUrlContains` |
| `nav-forward-btn` | Browser forward | `waitForUrlContains` |
| `refresh-page-btn` | Full page reload | `waitForUrlContains` |
| `change-title-btn` | Title changes after 3s | `waitForTitleContains("READY", 8, 300)` |
| `start-redirect-btn` | Redirect to cookies.html after 3s | `waitForUrlContains("cookies.html", 8, 500)` |
| `scroll-to-alpha/beta/gamma` | Hash anchor links | `waitForUrlContains("#hash-target-alpha")` |

```java
driver.get(BASE_URL + "navigation.html");
navActions.navigateToUrl(BASE_URL + "forms.html");
navActions.navigateBack();
navActions.refreshPage();

// Title wait (3s delay before change)
driver.findElement(By.id("change-title-btn")).click();
waitActions.waitForTitleContains("READY", 8, 300);

// Redirect chain
driver.findElement(By.id("start-redirect-btn")).click();
waitActions.waitForUrlContains("cookies.html", 8, 500);

// Hash navigation
driver.findElement(By.id("scroll-to-alpha")).click();
waitActions.waitForUrlContains("#hash-target-alpha", 3, 200);
```

---

### `cookies.html` — Cookies & Storage

**Maps to:** `CookieActionsTest`, `JavaScriptActionsTest`

Pre-seeded cookies written by the page's own JavaScript **on every load**:

| Cookie Name | Expected Value | Assert |
|---|---|---|
| `ellithium_auth` | `token_qa_12345` | `assertEquals(cookie.getValue(), "token_qa_12345")` |
| `ellithium_role` | `QA_ENGINEER` | `assertEquals` |
| `ellithium_pref` | `dark_mode=true` | `assertEquals` |
| `ellithium_session_id` | random `sess-XXXX` | `assertNotNull(cookie)` |

```java
driver.get(BASE_URL + "cookies.html");

// Verify pre-seeded cookies (no interaction needed — already set on load)
Cookie auth = cookieActions.getCookieNamed("ellithium_auth");
assertNotNull(auth);
assertEquals(auth.getValue(), "token_qa_12345");

// Get all
Set<Cookie> all = cookieActions.getCookies();
assertTrue(all.size() >= 4);

// Add a cookie via framework
cookieActions.addCookie(new Cookie("session", "abc123"));
assertEquals(cookieActions.getCookieNamed("session").getValue(), "abc123");

// Delete named → verify null
cookieActions.deleteCookieNamed("session");
assertNull(cookieActions.getCookieNamed("session"));

// Delete all
cookieActions.deleteAllCookies();
assertTrue(cookieActions.getCookies().isEmpty());

// localStorage via JS executor
((JavascriptExecutor)driver).executeScript("localStorage.setItem('test_key','test_val')");
String val = (String)((JavascriptExecutor)driver)
    .executeScript("return localStorage.getItem('test_key')");
assertEquals(val, "test_val");
```

---

## Exception Trap Quick Reference

| Exception | Trigger Element | Page |
|---|---|---|
| `NoSuchElementException` | `injected-form` before 5s | `waits.html` |
| `TimeoutException` | Wrong expected text on `polling-counter` | `waits.html` |
| `StaleElementReferenceException` | Reuse `stale-tbody` row ref after refresh | `stale.html` |
| `ElementNotInteractableException` | `offscreen-element` (left:-9999px) | `state.html` |
| `ElementClickInterceptedException` | `covered-btn` with z-index overlay | `state.html` |
| `InvalidElementStateException` | `clear()` on `disabled-input` | `state.html` |
| `NoSuchFrameException` | `switchTo().frame()` before 3s injection | `frames.html` |
| `NoSuchWindowException` | Switch to closed popup handle | `frames.html` |
| `UnhandledAlertException` | Any driver command after `arm-alert-trap-btn` fires | `state.html` |
| `NoAlertPresentException` | `switchTo().alert()` before 3s delay | `alerts.html` |
| `DetachedShadowRootException` | Reuse shadow ref after `replaceShadow()` | `stale.html` |

---

## InteractionRecovery Wiring Map

| Recovery Method | Arena Element | Page |
|---|---|---|
| `resolveInteractable(locator)` | `.add` candidates 0–4 (only #3 is enabled) | `state.html` |
| `jsClick(element)` | `js-only-btn-1/2` with intercepting overlays | `state.html` |
| `handleUnexpectedAlert()` | `arm-alert-trap-btn` fires alert 2s later | `state.html` |
| `recoverContext(locator, NoSuchWindowException)` | Close popup → recover back to main | `frames.html` |
| `recoverContext(locator, NoSuchFrameException)` | `iframe-username` before iframe exists | `frames.html` |

---

## Agent Instructions (for automated test scripting)

1. **Always start at `index.html`** to verify site is reachable before running module tests.
2. **Use explicit waits only** — no `Thread.sleep()`. The arena is designed to **break** fixed sleeps.
3. **Never reuse `WebElement` references** across table refreshes on `stale.html`.
4. **Frame exit is mandatory** — after any iframe interaction, call `driver.switchTo().defaultContent()` before accessing elements outside the frame.
5. **Alert handling before navigation** — call `InteractionRecovery.handleUnexpectedAlert()` if commands throw `UnhandledAlertException`.
6. **Shadow DOM** requires `executeScript("return arguments[0].shadowRoot", host)` — standard `findElement` does NOT pierce shadow boundaries.
7. **Cookie pre-seeds** are written by the page's own JS on every page load. No clicks required — navigate to `cookies.html` and immediately call `getCookieNamed("ellithium_auth")`.
8. **Custom dropdown on `forms.html`** (`#custom-dd`) is NOT a `<select>` — do NOT use `SelectActions`. Click trigger → click option div.
9. **Blob URL iframes** on `frames.html` have a `blob:` scheme `src`. Switch by `id="dynamic-iframe"` or `name="login-frame"`.
10. **Window count** — after clicking `open-blank-window-btn`, always call `waitForNumberOfWindowsToBe(2, ...)` before enumerating handles to avoid race conditions.

---

## File Structure

```
src/test/resources/test-website/
├── README.md             ← This file
├── _shared.css           ← Design tokens, cards, buttons, forms, tables, status badges
├── _shared.js            ← ArenaLog, setStatus(), ts(), randInt() shared utilities
├── index.html            ← Home dashboard + full element ID reference table
├── waits.html            ← 13+ WaitActions scenarios with real timing
├── state.html            ← Interactivity traps (intercepted, disabled, offscreen, inert)
├── stale.html            ← StaleElementReference + open Shadow DOM
├── frames.html           ← FrameActions + WindowActions (delayed iframe, nested frames, popups)
├── forms.html            ← SelectActions + HTML5 inputs + file upload + contenteditable
├── mouse.html            ← MouseActions (hover, dbl-click, drag-drop, right-click, scroll)
├── alerts.html           ← AlertActions (alert/confirm/prompt, 3s delays, DOM modals)
├── navigation.html       ← NavigationActions + waitForTitle/Url + hash + redirect
└── cookies.html          ← CookieActions + localStorage + sessionStorage (pre-seeded)
```

---

*Generated by Antigravity for the Ellithium project.  
All pages: zero external dependencies · zero CDN · works offline via `file://` URI.*
