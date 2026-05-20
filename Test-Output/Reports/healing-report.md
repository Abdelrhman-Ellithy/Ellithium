# Ellithium AI Healing Report

Generated at: 2026-05-16T21:38:09.9461544

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `semantic-strategy` | setUserName | sendData | `By.id: username` | `By.id: username` | 0.85 |
| 2 | `semantic-strategy` | setPassword | sendData | `By.id: password` | `By.id: password` | 0.85 |
| 3 | `semantic-strategy` | getLoginMessage | getText | `By.cssSelector: i.fa` | `By.cssSelector: i.fa` | 0.85 |

## Detailed Reasoning

### 1. By.id: username
- **Method:** `setUserName`
- **Healed to:** `By.id: username`
- **Reasoning:** [TIER 1.5 - Semantic] input[id~='UserName']

---

### 2. By.id: password
- **Method:** `setPassword`
- **Healed to:** `By.id: password`
- **Reasoning:** [TIER 1.5 - Semantic] input[id~='Password']

---

### 3. By.cssSelector: i.fa
- **Method:** `getLoginMessage`
- **Healed to:** `By.cssSelector: i.fa`
- **Reasoning:** [TIER 1.5 - Semantic] element with exact text 'Login'

---

