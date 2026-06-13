# Ellithium AI Healing Report

Generated at: 2026-06-09T04:07:51.1507208

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `algorithmic-baseline` | - | - | `By.cssSelector: input#emaaaiiillll` | `By.id: username` | 1.00 |
| 2 | `algorithmic-baseline` | - | - | `By.tagName: seecrret` | `By.id: password` | 1.00 |
| 3 | `algorithmic-baseline` | - | - | `By.cssSelector: login.bttn` | `By.xpath: //button[normalize-space(.)='Login' and @type='submit']` | 1.00 |
| 4 | `algorithmic-baseline` | - | - | `By.tagName: seecrret` | `By.id: password` | 1.00 |
| 5 | `algorithmic-baseline` | - | - | `By.cssSelector: input#emaaaiiillll` | `By.id: username` | 1.00 |
| 6 | `algorithmic-baseline` | - | - | `By.cssSelector: login.bttn` | `By.xpath: //button[normalize-space(.)='Login' and @type='submit']` | 1.00 |

## Detailed Reasoning

### 1. By.cssSelector: input#emaaaiiillll
- **Healed to:** `By.id: username`
- **Reasoning:** [TIER 1 - AttrSearch]

---

### 2. By.tagName: seecrret
- **Healed to:** `By.id: password`
- **Reasoning:** [TIER 1 - AttrSearch]

---

### 3. By.cssSelector: login.bttn
- **Healed to:** `By.xpath: //button[normalize-space(.)='Login' and @type='submit']`
- **Reasoning:** [TIER 1 - Algorithmic] Matched by: tag='button' text(partial)

---

### 4. By.tagName: seecrret
- **Healed to:** `By.id: password`
- **Reasoning:** [TIER 1 - AttrSearch]

---

### 5. By.cssSelector: input#emaaaiiillll
- **Healed to:** `By.id: username`
- **Reasoning:** [TIER 1 - AttrSearch]

---

### 6. By.cssSelector: login.bttn
- **Healed to:** `By.xpath: //button[normalize-space(.)='Login' and @type='submit']`
- **Reasoning:** [TIER 1 - Algorithmic] Matched by: tag='button' text(partial)

---


---

## Run: 2026-06-11T21:18:46.3988013

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/UI_NonBDD/tempTest.java` | testProducts | clickOnElement | `By.xpath: products` | `By.xpath("//a[normalize-space(.)=' Products']")` | 0.74 |

## Detailed Reasoning

### 1. By.xpath: products
- **Class:** `UI_NonBDD.tempTest`
- **Method:** `testProducts`
- **Line:** 17
- **Healed to:** `By.xpath("//a[normalize-space(.)=' Products']")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-11T21:23:33.6549144

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn-` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: message` | `By.id("content")` | 0.79 |
| 5 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 6 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 7 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn-` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 8 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: message` | `By.id("content")` | 0.79 |
| 9 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 10 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 11 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn-` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 12 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: message` | `By.id("content")` | 0.81 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn-
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.id: message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 5. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 6. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 7. By.tagName: btnnnn-
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 8. By.id: message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 9. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 10. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 11. By.tagName: btnnnn-
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 12. By.id: message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-11T21:24:39.8021341

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn-` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: secure-area-message` | `By.id("content")` | 0.74 |
| 5 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 6 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 7 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn-` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 8 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: secure-area-message` | `By.id("content")` | 0.74 |
| 9 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 10 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 11 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn-` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 12 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: secure-area-message` | `By.cssSelector("div.row")` | 0.77 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn-
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.id: secure-area-message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 5. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 6. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 7. By.tagName: btnnnn-
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 8. By.id: secure-area-message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 9. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 10. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 11. By.tagName: btnnnn-
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 12. By.id: secure-area-message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.cssSelector("div.row")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-11T21:28:48.6203515

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.xpath: valid or invalid']` | `By.id("content")` | 0.70 |
| 5 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 6 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 7 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 8 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.xpath: valid or invalid']` | `By.id("content")` | 0.70 |
| 9 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 10 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 11 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 12 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.xpath: valid or invalid']` | `By.cssSelector("div.row")` | 0.75 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.xpath: valid or invalid']
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 5. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 6. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 7. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 8. By.xpath: valid or invalid']
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 9. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 10. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 11. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 12. By.xpath: valid or invalid']
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.cssSelector("div.row")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-11T21:30:42.6208599

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: message` | `By.id("content")` | 0.79 |
| 5 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 6 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 7 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 8 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: message` | `By.id("content")` | 0.79 |
| 9 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 10 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 11 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 12 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: message` | `By.id("content")` | 0.81 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.id: message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 5. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 6. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 7. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 8. By.id: message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 9. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 10. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 11. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 12. By.id: message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-11T22:08:25.7776905

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/AmazonSearchPage.java` | searchForItem | sendData | `By.id: search` | `By.id("twotabsearchtextbox")` | 0.75 |
| 2 | `src/test/java/Pages/AmazonSearchPage.java` | clickSearch | clickOnElement | `By.tagName: searchbutton` | `By.id("nav-search-submit-button")` | 0.66 |

## Detailed Reasoning

### 1. By.id: search
- **Class:** `Pages.AmazonSearchPage`
- **Method:** `searchForItem`
- **Line:** 30
- **Healed to:** `By.id("twotabsearchtextbox")`
- **Reasoning:** [TIER 2]

---

### 2. By.tagName: searchbutton
- **Class:** `Pages.AmazonSearchPage`
- **Method:** `clickSearch`
- **Line:** 34
- **Healed to:** `By.id("nav-search-submit-button")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-12T15:26:11.0466153

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/UI_NonBDD/tempTest.java` | testProducts | clickOnElement | `By.xpath: products` | `By.xpath("//a[normalize-space(.)=' Products']")` | 0.74 |

## Detailed Reasoning

### 1. By.xpath: products
- **Class:** `UI_NonBDD.tempTest`
- **Method:** `testProducts`
- **Line:** 17
- **Healed to:** `By.xpath("//a[normalize-space(.)=' Products']")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-12T15:28:20.5370502

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 5 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 6 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 7 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 8 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 9 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 5. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 6. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 7. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 8. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 9. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-12T15:30:41.1513373

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: !` | `By.id("content")` | 0.74 |
| 5 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 6 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 7 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 8 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: !` | `By.id("content")` | 0.74 |
| 9 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 10 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 11 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 12 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: !` | `By.id("flash-messages")` | 0.81 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.id: !
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 5. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 6. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 7. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 8. By.id: !
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 9. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 10. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 11. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 12. By.id: !
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("flash-messages")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-12T15:34:12.0486546

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: valid message` | `By.id("content")` | 0.77 |
| 5 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 6 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 7 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 8 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: valid message` | `By.id("content")` | 0.77 |
| 9 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 10 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 11 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 12 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: valid message` | `By.id("content")` | 0.77 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.id: valid message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 5. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 6. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 7. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 8. By.id: valid message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---

### 9. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 10. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 11. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 12. By.id: valid message
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("content")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-12T19:45:32.1099648

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 5 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 6 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 7 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 8 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 9 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 10 | `src/test/java/Pages/SecureAreaPage.java` | getSecureAreaMessage | getText | `By.id: flash` | `By.id("flash-messages")` | 0.85 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 5. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 6. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 7. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 8. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 9. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 10. By.id: flash
- **Class:** `Pages.SecureAreaPage`
- **Method:** `getSecureAreaMessage`
- **Line:** 19
- **Healed to:** `By.id("flash-messages")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-12T19:58:11.4743804

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 5 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 6 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 7 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 8 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 9 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 5. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 6. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 7. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 8. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 9. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-13T00:28:33.7075714

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 5 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 6 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 7 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 8 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 9 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 5. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 6. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 7. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 8. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 9. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-13T15:22:04.3244776

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 5 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 6 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 7 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 8 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 9 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 5. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 6. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 7. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 8. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 9. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---


---

## Run: 2026-06-13T15:27:04.5327255

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 2 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 3 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 4 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 5 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 6 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |
| 7 | `src/test/java/Pages/LoginPage.java` | setPassword | sendData | `By.id: sssecreeett` | `By.id("password")` | 0.94 |
| 8 | `src/test/java/Pages/LoginPage.java` | setUserName | sendData | `By.cssSelector: emaaaiiiail` | `By.id("username")` | 0.89 |
| 9 | `src/test/java/Pages/LoginPage.java` | clickLoginBtn | clickOnElement | `By.tagName: btnnnn` | `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")` | 0.71 |

## Detailed Reasoning

### 1. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 2. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 3. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 4. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 5. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 6. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

### 7. By.id: sssecreeett
- **Class:** `Pages.LoginPage`
- **Method:** `setPassword`
- **Line:** 23
- **Healed to:** `By.id("password")`
- **Reasoning:** [TIER 2]

---

### 8. By.cssSelector: emaaaiiiail
- **Class:** `Pages.LoginPage`
- **Method:** `setUserName`
- **Line:** 19
- **Healed to:** `By.id("username")`
- **Reasoning:** [TIER 2]

---

### 9. By.tagName: btnnnn
- **Class:** `Pages.LoginPage`
- **Method:** `clickLoginBtn`
- **Line:** 27
- **Healed to:** `By.xpath("//button[normalize-space(.)='Login' and @type='submit']")`
- **Reasoning:** [TIER 2]

---

