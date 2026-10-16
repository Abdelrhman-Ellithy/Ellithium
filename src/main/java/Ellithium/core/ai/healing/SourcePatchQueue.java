package Ellithium.core.ai.healing;

import Ellithium.core.ai.JavaSourceModifier;
import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

class SourcePatchQueue {

    private final ConcurrentLinkedQueue<AISelfHealer.SourcePatch> queue;

    SourcePatchQueue(ConcurrentLinkedQueue<AISelfHealer.SourcePatch> queue) {
        this.queue = queue;
    }

    void queue(AISelfHealer.SourcePatch patch) {
        queue.add(patch);
    }

    Map<String, AISelfHealer.SourcePatch> resolvePatchConflicts(Iterable<AISelfHealer.SourcePatch> patches) {
        LinkedHashMap<String, AISelfHealer.SourcePatch> uniquePatches = new LinkedHashMap<>();
        for (AISelfHealer.SourcePatch patch : patches) {
            String key = (patch.filePath != null ? patch.filePath : "") + "|"
                    + (patch.fieldName != null ? patch.fieldName : "") + "|"
                    + (patch.byMethod != null ? patch.byMethod : "") + "|"
                    + (patch.byValue != null ? patch.byValue : "");
            AISelfHealer.SourcePatch existing = uniquePatches.get(key);
            if (existing == null || patch.confidence > existing.confidence) {
                uniquePatches.put(key, patch);
            }
        }
        return uniquePatches;
    }

    private final Map<String, AISelfHealer.SourcePatch> cumulativeProposals = new LinkedHashMap<>();

    synchronized void clearProposals() {
        cumulativeProposals.clear();
    }

    synchronized void apply() {
        if (queue.isEmpty()) return;
        Map<String, AISelfHealer.SourcePatch> uniquePatches = resolvePatchConflicts(queue);
        queue.clear();

        double storeThreshold = AIConfigLoader.getHealingStoreThreshold();
        java.util.List<AISelfHealer.SourcePatch> qualified = new java.util.ArrayList<>();
        for (AISelfHealer.SourcePatch patch : uniquePatches.values()) {
            if (patch.confidence >= storeThreshold) qualified.add(patch);
        }
        if (qualified.isEmpty()) return;

        if (AIConfigLoader.isCI() || AIConfigLoader.isProposalMode()) {
            String modeStr = AIConfigLoader.isProposalMode() ? "PROPOSAL_ONLY" : "CI";
            Reporter.log("[SOURCE-PATCH] " + modeStr + " mode active — exporting " + qualified.size()
                    + " patch proposal(s) without modifying source files", LogLevel.INFO_YELLOW);
            for (AISelfHealer.SourcePatch p : qualified) {
                String key = (p.filePath != null ? p.filePath : "") + "|"
                        + (p.fieldName != null ? p.fieldName : "") + "|"
                        + (p.byMethod != null ? p.byMethod : "") + "|"
                        + (p.byValue != null ? p.byValue : "");
                cumulativeProposals.put(key, p);
            }
            exportProposals(cumulativeProposals.values());
            return;
        }

        Reporter.log("AI Self-Healing: Applying " + qualified.size() + " source patches...", LogLevel.INFO_YELLOW);
        int applied = 0;
        for (AISelfHealer.SourcePatch patch : qualified) {
            boolean written = false;
            if (patch.fieldName != null) {
                written = JavaSourceModifier.updateLocatorValue(patch.filePath, patch.fieldName, patch.newLocatorExpression);
            } else if (patch.byMethod != null) {
                written = JavaSourceModifier.updateLocatorByOldValue(patch.filePath, patch.byMethod, patch.byValue, patch.newLocatorExpression);
            }
            if (written) applied++;
        }
        Reporter.log("AI Self-Healing: " + applied + "/" + qualified.size() + " source patches applied", LogLevel.INFO_GREEN);
    }

    private void exportProposals(java.util.Collection<AISelfHealer.SourcePatch> patches) {
        if (patches == null || patches.isEmpty()) return;
        try {
            java.nio.file.Path proposalDir = java.nio.file.Paths.get("Test-Output", "healing-proposals");
            java.nio.file.Files.createDirectories(proposalDir);

            // 1. JSON manifest
            StringBuilder json = new StringBuilder("[\n");
            int count = 0;
            for (AISelfHealer.SourcePatch patch : patches) {
                if (count > 0) json.append(",\n");
                json.append("  {\n");
                json.append("    \"filePath\": \"").append(escapeJson(patch.filePath)).append("\",\n");
                json.append("    \"fieldName\": ").append(patch.fieldName != null ? "\"" + escapeJson(patch.fieldName) + "\"" : "null").append(",\n");
                json.append("    \"byMethod\": ").append(patch.byMethod != null ? "\"" + escapeJson(patch.byMethod) + "\"" : "null").append(",\n");
                json.append("    \"byValue\": ").append(patch.byValue != null ? "\"" + escapeJson(patch.byValue) + "\"" : "null").append(",\n");
                json.append("    \"newLocatorExpression\": \"").append(escapeJson(patch.newLocatorExpression)).append("\",\n");
                json.append("    \"confidence\": ").append(String.format(java.util.Locale.US, "%.3f", patch.confidence)).append(",\n");
                json.append("    \"tier\": ").append(patch.tier).append("\n");
                json.append("  }");
                count++;
            }
            json.append("\n]\n");
            java.nio.file.Files.writeString(proposalDir.resolve("locator-patches.json"), json.toString());

            // 2. Diff patch format
            StringBuilder diff = new StringBuilder();
            diff.append("# Ellithium AI Locator Healing Proposals\n");
            diff.append("# Generated on: ").append(java.time.Instant.now()).append("\n\n");
            for (AISelfHealer.SourcePatch patch : patches) {
                String normPath = (patch.filePath != null ? patch.filePath : "unknown").replace('\\', '/');
                diff.append("--- a/").append(normPath).append("\n");
                diff.append("+++ b/").append(normPath).append("\n");
                diff.append("@@ -1,1 +1,1 @@\n");
                if (patch.fieldName != null) {
                    diff.append("- [field: ").append(patch.fieldName).append("]\n");
                    diff.append("+ [field: ").append(patch.fieldName).append(" = ").append(patch.newLocatorExpression).append("]\n");
                } else if (patch.byMethod != null) {
                    diff.append("- By.").append(patch.byMethod).append("(\"").append(patch.byValue != null ? patch.byValue : "").append("\")\n");
                    diff.append("+ ").append(patch.newLocatorExpression).append("\n");
                } else if (patch.byValue != null) {
                    diff.append("- ").append(patch.byValue).append("\n");
                    diff.append("+ ").append(patch.newLocatorExpression).append("\n");
                } else {
                    diff.append("- [unspecified locator]\n");
                    diff.append("+ ").append(patch.newLocatorExpression).append("\n");
                }
                diff.append("\n");
            }
            java.nio.file.Files.writeString(proposalDir.resolve("healing-locators.patch"), diff.toString());

            Reporter.log("[SOURCE-PATCH] Exported " + patches.size() + " proposal(s) to Test-Output/healing-proposals/", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("[SOURCE-PATCH] Failed to export healing proposals: " + e.getMessage(), LogLevel.WARN);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
