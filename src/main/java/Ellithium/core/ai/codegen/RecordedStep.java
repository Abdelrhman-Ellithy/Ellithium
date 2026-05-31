package Ellithium.core.ai.codegen;

import java.util.List;

public final class RecordedStep {

    private final String id;
    private final String actionType;
    private final String data;
    private final String tagName;
    private final String elementName;
    private final List<LocatorCandidate> candidates;
    private final List<Integer> frameChain;
    private volatile int chosenIndex;

    public RecordedStep(String id, String actionType, String data, String tagName,
                        String elementName, List<LocatorCandidate> candidates) {
        this(id, actionType, data, tagName, elementName, candidates, List.of());
    }

    public RecordedStep(String id, String actionType, String data, String tagName,
                        String elementName, List<LocatorCandidate> candidates, List<Integer> frameChain) {
        this.id = id;
        this.actionType = actionType;
        this.data = data;
        this.tagName = tagName;
        this.elementName = elementName;
        this.candidates = candidates != null ? candidates : List.of();
        this.frameChain = frameChain != null ? frameChain : List.of();
        this.chosenIndex = this.candidates.isEmpty() ? -1 : 0;
    }

    public String getId() { return id; }
    public String getActionType() { return actionType; }
    public String getData() { return data; }
    public String getTagName() { return tagName; }
    public String getElementName() { return elementName; }
    public List<LocatorCandidate> getCandidates() { return candidates; }
    public List<Integer> getFrameChain() { return frameChain; }
    public int getChosenIndex() { return chosenIndex; }

    public LocatorCandidate chosen() {
        return (chosenIndex >= 0 && chosenIndex < candidates.size()) ? candidates.get(chosenIndex) : null;
    }

    public void choose(int index) {
        if (index >= 0 && index < candidates.size()) chosenIndex = index;
    }
}
