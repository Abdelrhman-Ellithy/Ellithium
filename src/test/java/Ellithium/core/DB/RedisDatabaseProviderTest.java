package Ellithium.core.DB;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RedisDatabaseProviderTest {

    private JedisPool mockPool;
    private Jedis mockJedis;
    private Cache<String, Object> cache;
    private RedisDatabaseProvider redisProvider;

    @BeforeMethod
    public void setUp() {
        mockPool = mock(JedisPool.class);
        mockJedis = mock(Jedis.class);
        when(mockPool.getResource()).thenReturn(mockJedis);

        cache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();

        redisProvider = new RedisDatabaseProvider(mockPool, cache);
    }

    @Test
    public void testConstructorValidation() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new RedisDatabaseProvider(null, cache);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new RedisDatabaseProvider(mockPool, null);
        });
    }

    @Test
    public void testExecuteQueryCacheMissAndHit() {
        when(mockJedis.get("user:101")).thenReturn("{\"name\":\"Alice\"}");

        // First call - cache miss
        Object result = redisProvider.executeQuery("user:101");
        Assert.assertEquals(result, "{\"name\":\"Alice\"}");
        verify(mockJedis, times(1)).get("user:101");

        // Second call - cache hit
        Object cachedResult = redisProvider.executeQuery("user:101");
        Assert.assertEquals(cachedResult, "{\"name\":\"Alice\"}");
        verify(mockJedis, times(1)).get("user:101"); // Not called again
    }

    @Test
    public void testExecuteQueryExceptionHandling() {
        when(mockPool.getResource()).thenThrow(new RuntimeException("Connection refused"));
        Object result = redisProvider.executeQuery("user:999");
        Assert.assertNull(result);
    }

    @Test
    public void testCacheOperations() {
        redisProvider.addToCache("key1", "val1");
        Assert.assertEquals(redisProvider.getFromCache("key1"), "val1");

        redisProvider.clearCache("key1");
        Assert.assertNull(redisProvider.getFromCache("key1"));

        redisProvider.addToCache("key2", "val2");
        redisProvider.clearAllCaches();
        Assert.assertNull(redisProvider.getFromCache("key2"));
    }

    @Test
    public void testIsHealthy() {
        when(mockJedis.ping()).thenReturn("PONG");
        Assert.assertTrue(redisProvider.isHealthy());

        when(mockJedis.ping()).thenReturn("ERROR");
        Assert.assertFalse(redisProvider.isHealthy());

        when(mockPool.getResource()).thenThrow(new RuntimeException("Pool exhausted"));
        Assert.assertFalse(redisProvider.isHealthy());
    }

    @Test
    public void testExistsAndDelete() {
        when(mockJedis.exists("session:1")).thenReturn(true);
        Assert.assertTrue(redisProvider.exists("session:1"));

        when(mockJedis.del("session:1")).thenReturn(1L);
        Assert.assertTrue(redisProvider.delete("session:1"));

        when(mockJedis.del("session:unknown")).thenReturn(0L);
        Assert.assertFalse(redisProvider.delete("session:unknown"));

        // Error branches
        when(mockJedis.exists(anyString())).thenThrow(new RuntimeException("Failure"));
        Assert.assertFalse(redisProvider.exists("session:err"));

        when(mockJedis.del(anyString())).thenThrow(new RuntimeException("Failure"));
        Assert.assertFalse(redisProvider.delete("session:err"));
    }

    @Test
    public void testExpire() {
        when(mockJedis.expire("token:1", 300)).thenReturn(1L);
        Assert.assertTrue(redisProvider.expire("token:1", 300));

        when(mockJedis.expire("token:1", 300)).thenReturn(0L);
        Assert.assertFalse(redisProvider.expire("token:1", 300));

        when(mockJedis.expire(anyString(), anyInt())).thenThrow(new RuntimeException("Failure"));
        Assert.assertFalse(redisProvider.expire("token:err", 300));
    }

    @Test
    public void testSetAndGet() {
        redisProvider.set("status", "ONLINE");
        verify(mockJedis).set("status", "ONLINE");

        when(mockJedis.get("status")).thenReturn("ONLINE");
        Assert.assertEquals(redisProvider.get("status"), "ONLINE");

        // Error on set throws NoSQLRuntimeException
        doThrow(new RuntimeException("Failed")).when(mockJedis).set("errKey", "errVal");
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            redisProvider.set("errKey", "errVal");
        });

        // Error on get returns null
        when(mockJedis.get("errKey")).thenThrow(new RuntimeException("Failed"));
        Assert.assertNull(redisProvider.get("errKey"));
    }

    @Test
    public void testSetWithExpiry() {
        redisProvider.setWithExpiry("tempKey", "tempVal", 60);
        verify(mockJedis).set(eq("tempKey"), eq("tempVal"), any(SetParams.class));

        doThrow(new RuntimeException("Failed")).when(mockJedis).set(eq("errKey"), eq("errVal"), any(SetParams.class));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            redisProvider.setWithExpiry("errKey", "errVal", 60);
        });
    }

    @Test
    public void testHashOperations() {
        Map<String, String> userMap = Map.of("name", "Bob", "role", "admin");
        redisProvider.hset("user:hash", userMap);
        verify(mockJedis).hset("user:hash", userMap);

        when(mockJedis.hgetAll("user:hash")).thenReturn(userMap);
        Map<String, String> retrieved = redisProvider.hgetAll("user:hash");
        Assert.assertEquals(retrieved, userMap);

        doThrow(new RuntimeException("Failed")).when(mockJedis).hset(eq("errHash"), anyMap());
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            redisProvider.hset("errHash", userMap);
        });

        when(mockJedis.hgetAll("errHash")).thenThrow(new RuntimeException("Failed"));
        Assert.assertTrue(redisProvider.hgetAll("errHash").isEmpty());
    }

    @Test
    public void testListOperations() {
        redisProvider.lpush("queue", "task1", "task2");
        verify(mockJedis).lpush("queue", "task1", "task2");

        when(mockJedis.lrange("queue", 0, -1)).thenReturn(List.of("task2", "task1"));
        List<String> list = redisProvider.lrange("queue", 0, -1);
        Assert.assertEquals(list, List.of("task2", "task1"));

        doThrow(new RuntimeException("Failed")).when(mockJedis).lpush(eq("errQueue"), any(String[].class));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            redisProvider.lpush("errQueue", "t1");
        });

        when(mockJedis.lrange(eq("errQueue"), anyInt(), anyInt())).thenThrow(new RuntimeException("Failed"));
        Assert.assertTrue(redisProvider.lrange("errQueue", 0, 5).isEmpty());
    }

    @Test
    public void testSetMembersOperations() {
        redisProvider.sadd("tags", "java", "automation");
        verify(mockJedis).sadd("tags", "java", "automation");

        when(mockJedis.smembers("tags")).thenReturn(Set.of("java", "automation"));
        Set<String> members = redisProvider.smembers("tags");
        Assert.assertEquals(members, Set.of("java", "automation"));

        doThrow(new RuntimeException("Failed")).when(mockJedis).sadd(eq("errTags"), any(String[].class));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            redisProvider.sadd("errTags", "t1");
        });

        when(mockJedis.smembers("errTags")).thenThrow(new RuntimeException("Failed"));
        Assert.assertTrue(redisProvider.smembers("errTags").isEmpty());
    }

    @Test
    public void testPipeline() {
        Pipeline mockPipeline = mock(Pipeline.class);
        when(mockJedis.pipelined()).thenReturn(mockPipeline);

        redisProvider.pipeline(p -> {
            p.set("k1", "v1");
            p.set("k2", "v2");
        });
        verify(mockPipeline).syncAndReturnAll();

        when(mockJedis.pipelined()).thenThrow(new RuntimeException("Pipeline error"));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            redisProvider.pipeline(p -> p.get("k"));
        });
    }

    @Test
    public void testCloseConnection() {
        when(mockPool.isClosed()).thenReturn(false);
        redisProvider.closeConnection();
        verify(mockPool).close();
    }
}
