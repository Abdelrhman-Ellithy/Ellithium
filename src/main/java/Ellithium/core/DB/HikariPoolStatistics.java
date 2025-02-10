package Ellithium.core.DB;

public class HikariPoolStatistics {
    private final int activeConnections;
    private final int idleConnections;
    private final int totalConnections;
    private final int waitingThreads;

    public HikariPoolStatistics(int activeConnections, int idleConnections, int totalConnections, int waitingThreads) {
        this.activeConnections = activeConnections;
        this.idleConnections = idleConnections;
        this.totalConnections = totalConnections;
        this.waitingThreads = waitingThreads;
    }

    public int getActiveConnections() { return activeConnections; }
    public int getIdleConnections() { return idleConnections; }
    public int getTotalConnections() { return totalConnections; }
    public int getWaitingThreads() { return waitingThreads; }
}