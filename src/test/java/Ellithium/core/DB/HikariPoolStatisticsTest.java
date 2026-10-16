package Ellithium.core.DB;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HikariPoolStatisticsTest {

    @Test
    public void testGettersAndState() {
        HikariPoolStatistics stats = new HikariPoolStatistics(5, 10, 15, 2);
        Assert.assertEquals(stats.getActiveConnections(), 5);
        Assert.assertEquals(stats.getIdleConnections(), 10);
        Assert.assertEquals(stats.getTotalConnections(), 15);
        Assert.assertEquals(stats.getWaitingThreads(), 2);
    }

    @Test
    public void testZeroState() {
        HikariPoolStatistics stats = new HikariPoolStatistics(0, 0, 0, 0);
        Assert.assertEquals(stats.getActiveConnections(), 0);
        Assert.assertEquals(stats.getIdleConnections(), 0);
        Assert.assertEquals(stats.getTotalConnections(), 0);
        Assert.assertEquals(stats.getWaitingThreads(), 0);
    }
}
