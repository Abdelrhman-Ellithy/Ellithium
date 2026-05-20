# Ellithium AI Healing Report

Generated at: 2026-05-20T21:17:55.2600933

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `semantic-strategy` | setUserName | sendData | `By.id: username` | `By.id: username` | 0.85 |
| 2 | `semantic-strategy` | setPassword | sendData | `By.id: password` | `By.id: password` | 0.85 |
| 3 | `semantic-strategy` | clickLoginBtn | clickOnElement | `By.id: login` | `By.id: login` | 0.85 |
| 4 | `semantic-strategy` | getLoginMessage | getText | `By.id: login` | `By.id: login` | 0.85 |
| 5 | `algorithmic-baseline` | - | - | `By.id: email` | `By.id: username` | 0.92 |
| 6 | `algorithmic-baseline` | - | - | `By.id: pas` | `By.id: password` | 0.92 |
| 7 | `algorithmic-baseline` | - | - | `By.id: main-content` | `By.id: login` | 0.93 |
| 8 | `algorithmic-baseline` | - | - | `By.className: message` | `By.id: login` | 0.93 |
| 9 | `algorithmic-baseline` | - | - | `By.id: pas` | `By.id: password` | 0.92 |
| 10 | `algorithmic-baseline` | - | - | `By.id: email` | `By.id: username` | 0.92 |
| 11 | `algorithmic-baseline` | - | - | `By.id: main-content` | `By.id: login` | 0.93 |
| 12 | `algorithmic-baseline` | - | - | `By.className: message` | `By.id: login` | 0.93 |

## Detailed Reasoning

### 1. By.id: username
- **Method:** `setUserName`
- **Healed to:** `By.id: username`
- **Reasoning:** [TIER 2 - Semantic] id='username' (exact lower)

---

### 2. By.id: password
- **Method:** `setPassword`
- **Healed to:** `By.id: password`
- **Reasoning:** [TIER 2 - Semantic] id='password' (exact lower)

---

### 3. By.id: login
- **Method:** `clickLoginBtn`
- **Healed to:** `By.id: login`
- **Reasoning:** [TIER 2 - Semantic] id='login' (exact lower)

---

### 4. By.id: login
- **Method:** `getLoginMessage`
- **Healed to:** `By.id: login`
- **Reasoning:** [TIER 2 - Semantic] id='login' (exact lower)

---

### 5. By.id: email
- **Healed to:** `By.id: username`
- **Reasoning:** [TIER 1 - AttrSearch]

---

### 6. By.id: pas
- **Healed to:** `By.id: password`
- **Reasoning:** [TIER 1 - Mutation]

---

### 7. By.id: main-content
- **Healed to:** `By.id: login`
- **Reasoning:** [TIER 1 - AttrSearch]

---

### 8. By.className: message
- **Healed to:** `By.id: login`
- **Reasoning:** [TIER 1 - AttrSearch]

---

### 9. By.id: pas
- **Healed to:** `By.id: password`
- **Reasoning:** [TIER 1 - Mutation]

---

### 10. By.id: email
- **Healed to:** `By.id: username`
- **Reasoning:** [TIER 1 - AttrSearch]

---

### 11. By.id: main-content
- **Healed to:** `By.id: login`
- **Reasoning:** [TIER 1 - AttrSearch]

---

### 12. By.className: message
- **Healed to:** `By.id: login`
- **Reasoning:** [TIER 1 - AttrSearch]

---

