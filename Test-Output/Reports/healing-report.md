# Ellithium AI Healing Report

Generated at: 2026-05-16T17:53:25.3001965

The following locators failed during execution and were healed by the AI Engine.

| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |
|---|------|--------|--------|----------------|----------------|------------|
| 1 | `algorithmic-baseline` | - | - | `By.id: user` | `By.id: username` | 0.65 |
| 2 | `algorithmic-baseline` | - | - | `By.id: pass` | `By.id: password` | 0.65 |
| 3 | `semantic-strategy` | clickLoginBtn | clickOnElement | `By.cssSelector: button.radius` | `By.cssSelector: button.radius` | 0.85 |
| 4 | `algorithmic-baseline` | - | - | `By.id: user` | `By.id: username` | 0.65 |
| 5 | `algorithmic-baseline` | - | - | `By.id: pass` | `By.id: password` | 0.65 |
| 6 | `semantic-strategy` | clickLoginBtn | clickOnElement | `By.cssSelector: button.radius` | `By.cssSelector: button.radius` | 0.85 |
| 7 | `algorithmic-baseline` | - | - | `By.id: pass` | `By.id: password` | 0.65 |
| 8 | `algorithmic-baseline` | - | - | `By.id: user` | `By.id: username` | 0.65 |
| 9 | `semantic-strategy` | clickLoginBtn | clickOnElement | `By.cssSelector: button.radius` | `By.cssSelector: button.radius` | 0.85 |

## Detailed Reasoning

### 1. By.id: user
- **Healed to:** `By.id: username`
- **Reasoning:** [TIER 1 - Algorithmic] Matched by: id='username' name='username' tag='input' text(partial)

---

### 2. By.id: pass
- **Healed to:** `By.id: password`
- **Reasoning:** [TIER 1 - Algorithmic] Matched by: id='password' name='password' tag='input' text(partial)

---

### 3. By.cssSelector: button.radius
- **Method:** `clickLoginBtn`
- **Healed to:** `By.cssSelector: button.radius`
- **Reasoning:** [TIER 1.5 - Semantic] first button after text 'Login'

---

### 4. By.id: user
- **Healed to:** `By.id: username`
- **Reasoning:** [TIER 1 - Algorithmic] Matched by: id='username' name='username' tag='input' text(partial)

---

### 5. By.id: pass
- **Healed to:** `By.id: password`
- **Reasoning:** [TIER 1 - Algorithmic] Matched by: id='password' name='password' tag='input' text(partial)

---

### 6. By.cssSelector: button.radius
- **Method:** `clickLoginBtn`
- **Healed to:** `By.cssSelector: button.radius`
- **Reasoning:** [TIER 1.5 - Semantic] first button after text 'Login'

---

### 7. By.id: pass
- **Healed to:** `By.id: password`
- **Reasoning:** [TIER 1 - Algorithmic] Matched by: id='password' name='password' tag='input' text(partial)

---

### 8. By.id: user
- **Healed to:** `By.id: username`
- **Reasoning:** [TIER 1 - Algorithmic] Matched by: id='username' name='username' tag='input' text(partial)

---

### 9. By.cssSelector: button.radius
- **Method:** `clickLoginBtn`
- **Healed to:** `By.cssSelector: button.radius`
- **Reasoning:** [TIER 1.5 - Semantic] first button after text 'Login'

---

