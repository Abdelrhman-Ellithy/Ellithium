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
            String key = patch.filePath + "|" + patch.byMethod + "|" + patch.byValue;
            AISelfHealer.SourcePatch existing = uniquePatches.get(key);
            if (existing == null || patch.confidence > existing.confidence) {
                uniquePatches.put(key, patch);
            }
        }
        return uniquePatches;
    }

    void apply() {
        if (queue.isEmpty()) return;
        if (AIConfigLoader.isCI()) {
            Reporter.log("[SOURCE-PATCH] CI environment detected — " + queue.size()
                    + " queued patch(es) discarded (source modification disabled in CI)", LogLevel.WARN);
            queue.clear();
            return;
        }

        Map<String, AISelfHealer.SourcePatch> uniquePatches = resolvePatchConflicts(queue);
        queue.clear();

        Reporter.log("AI Self-Healing: Applying " + uniquePatches.size() + " source patches...", LogLevel.INFO_YELLOW);
        int applied = 0;
        double storeThreshold = AIConfigLoader.getHealingStoreThreshold();
        for (AISelfHealer.SourcePatch patch : uniquePatches.values()) {
            if (patch.confidence < storeThreshold) continue;
            boolean written = false;
            if (patch.fieldName != null) {
                written = JavaSourceModifier.updateLocatorValue(patch.filePath, patch.fieldName, patch.newLocatorExpression);
            } else if (patch.byMethod != null) {
                written = JavaSourceModifier.updateLocatorByOldValue(patch.filePath, patch.byMethod, patch.byValue, patch.newLocatorExpression);
            }
            if (written) applied++;
        }
        Reporter.log("AI Self-Healing: " + applied + "/" + uniquePatches.size() + " source patches applied", LogLevel.INFO_GREEN);
    }
}
