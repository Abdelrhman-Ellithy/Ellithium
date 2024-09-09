package AutoEllithiumSphere.properties;
public class MainApplication {
    public static void main(String[] args) {
        // Initialize properties at the start of the application
        PropertyInitializer.initializeProperties();
        // Rest of the application logic...
        System.out.println("Application started with properties initialized.");
    }
}
