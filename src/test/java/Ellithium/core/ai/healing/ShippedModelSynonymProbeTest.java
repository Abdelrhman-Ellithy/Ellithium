package Ellithium.core.ai.healing;

import org.testng.annotations.Test;

/**
 * Diagnostic probe of the SHIPPED (decrypted, quantized) ONNX model on the four real Arena
 * synonym cases, using the exact query and document strings observed in healing telemetry.
 * Separates model ranking quality from the DOM/candidate machinery: prints cosine(query, correct)
 * vs cosine(query, distractor) per case. Informational — it never fails the build.
 */
public class ShippedModelSynonymProbeTest {

    private static final String[][] CASES = {
            {"type input enter text secret",
             "iframe-password •••••••• password input",
             "iframe-username admin text input"},
            {"click press send form button",
             "form-submit-btn btn btn green btn full button ✅ Submit Test Case",
             "nav link active a 📝\nForms"},
            {"click press begin timer",
             "trigger-enable-btn btn btn blue btn full button ▶ Start Enable Timer",
             "trigger-delayed-btn btn btn cyan btn full mt 3 button ▶ Start Random Delay Timers"},
            {"click press double tap zone",
             "dbl-click-target dbl click zone div 👆 Double-Click Me to Activate",
             "tab-stop-1 submit btn btn ghost btn sm button Tab Stop 1"},
    };

    @Test
    public void probeShippedModelOnArenaSynonymCases() {
        if (!EnsembleHealer.isAvailable()) {
            System.out.println("[PROBE] Tier 2 model unavailable — probe skipped");
            return;
        }
        System.out.println("[PROBE] Shipped-model synonym ranking (cosine correct vs distractor):");
        int ranked = 0;
        for (String[] c : CASES) {
            float[] q  = EnsembleHealer.embed(c[0], true);
            float[] dp = EnsembleHealer.embed(c[1], false);
            float[] dn = EnsembleHealer.embed(c[2], false);
            if (q == null || dp == null || dn == null) {
                System.out.printf("[PROBE] %-35s -> embed failed%n", c[0]);
                continue;
            }
            double pos = dot(q, dp), neg = dot(q, dn);
            boolean ok = pos > neg;
            if (ok) ranked++;
            System.out.printf("[PROBE] %-35s pos=%.4f neg=%.4f margin=%+.4f %s%n",
                    "'" + c[0] + "'", pos, neg, pos - neg, ok ? "RANKED-CORRECT" : "MIS-RANKED");
        }
        System.out.println("[PROBE] correct ranking on " + ranked + "/" + CASES.length + " cases");
    }

    private static double dot(float[] a, float[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }
}
