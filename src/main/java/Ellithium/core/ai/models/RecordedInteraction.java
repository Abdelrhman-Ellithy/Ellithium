package Ellithium.core.ai.models;

/**
 * Represents a single user interaction captured by the {@code InteractionRecorder}.
 *
 * <p>Each recorded interaction stores:</p>
 * <ul>
 *   <li>The action performed (click, sendData, select, navigate)</li>
 *   <li>The best reconstructed locator for the element</li>
 *   <li>The data sent (text typed, option selected, URL navigated to)</li>
 *   <li>A human-readable semantic name for the element (from AX tree)</li>
 *   <li>A timestamp for ordering</li>
 * </ul>
 */
public class RecordedInteraction {

    private final String actionType;    // "click", "sendData", "selectByText", "navigate"
    private final String locator;       // Best reconstructed locator expression, e.g. "By.id(\"username\")"
    private final String data;          // Text typed, option selected, URL navigated to
    private final String elementName;   // Semantic name from AX tree / aria-label / placeholder
    private final String tagName;       // Element tag: "input", "button", "a", etc.
    private final long timestamp;

    public RecordedInteraction(String actionType, String locator, String data,
                                String elementName, String tagName) {
        this.actionType = actionType;
        this.locator = locator;
        this.data = data;
        this.elementName = elementName;
        this.tagName = tagName;
        this.timestamp = System.currentTimeMillis();
    }

    public String getActionType() { return actionType; }
    public String getLocator() { return locator; }
    public String getData() { return data; }
    public String getElementName() { return elementName; }
    public String getTagName() { return tagName; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(actionType).append(" on <").append(tagName).append(">");
        if (elementName != null && !elementName.isBlank()) {
            sb.append(" \"").append(elementName).append("\"");
        }
        if (locator != null) {
            sb.append(" [").append(locator).append("]");
        }
        if (data != null && !data.isBlank()) {
            sb.append(" with data=\"").append(data).append("\"");
        }
        return sb.toString();
    }
}
