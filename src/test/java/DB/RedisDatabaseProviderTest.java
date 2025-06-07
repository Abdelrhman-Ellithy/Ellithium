package DB;

import Ellithium.core.DB.RedisDatabaseProvider;
import com.github.benmanes.caffeine.cache.Cache;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;
import static org.mockito.Mockito.*;

public class RedisDatabaseProviderTest {

    @Mock
    private JedisPool mockJedisPool;
    
    @Mock
    private Jedis mockJedis;

    @Mock
    private Cache<String, Object> mockCache;

    @Mock
    private Pipeline mockPipeline;

    private RedisDatabaseProvider redisProvider;
    private final String TEST_KEY = "testKey";
    private final String TEST_VALUE = "testValue";

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(mockJedisPool.getResource()).thenReturn(mockJedis);
        when(mockJedis.pipelined()).thenReturn(mockPipeline);
        redisProvider = new RedisDatabaseProvider(mockJedisPool, mockCache);
    }

    @Test
    void testSetAndGet() {

        when(mockJedis.set(TEST_KEY, TEST_VALUE)).thenReturn("OK");
        when(mockJedis.get(TEST_KEY)).thenReturn(TEST_VALUE);


        redisProvider.set(TEST_KEY, TEST_VALUE);
        String result = redisProvider.get(TEST_KEY);


        assertEquals(TEST_VALUE, result);
        verify(mockJedis).set(TEST_KEY, TEST_VALUE);
        verify(mockJedis).get(TEST_KEY);
    }

    @Test
    void testHashOperations() {

        Map<String, String> hash = new HashMap<>();
        hash.put("field1", "value1");
        hash.put("field2", "value2");
        
        when(mockJedis.hset(TEST_KEY, hash)).thenReturn(2L);
        when(mockJedis.hgetAll(TEST_KEY)).thenReturn(hash);


        redisProvider.hset(TEST_KEY, hash);
        Map<String, String> result = redisProvider.hgetAll(TEST_KEY);


        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("field1"));
        assertEquals("value2", result.get("field2"));
    }

    @Test
    void testListOperations() {

        List<String> list = List.of("item1", "item2", "item3");
        when(mockJedis.lpush(TEST_KEY, "item1", "item2", "item3")).thenReturn(3L);
        when(mockJedis.lrange(TEST_KEY, 0, -1)).thenReturn(list);


        redisProvider.lpush(TEST_KEY, "item1", "item2", "item3");
        List<String> result = redisProvider.lrange(TEST_KEY, 0, -1);


        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("item1", result.get(0));
    }

    @Test
    void testSetOperations() {

        Set<String> set = Set.of("member1", "member2");
        when(mockJedis.sadd(TEST_KEY, "member1", "member2")).thenReturn(2L);
        when(mockJedis.smembers(TEST_KEY)).thenReturn(set);


        redisProvider.sadd(TEST_KEY, "member1", "member2");
        Set<String> result = redisProvider.smembers(TEST_KEY);


        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("member1"));
        assertTrue(result.contains("member2"));
    }

    @Test
    void testPipelineOperations() {

        when(mockPipeline.syncAndReturnAll()).thenReturn(List.of("OK", "OK"));


        redisProvider.pipeline(commands -> {
            commands.set(TEST_KEY, TEST_VALUE);
            commands.set(TEST_KEY + "2", TEST_VALUE + "2");
        });


        verify(mockJedis).pipelined();
        verify(mockPipeline).set(TEST_KEY, TEST_VALUE);
        verify(mockPipeline).set(TEST_KEY + "2", TEST_VALUE + "2");
        verify(mockPipeline).syncAndReturnAll();
    }

    @Test
    void testDelete() {

        when(mockJedis.del(TEST_KEY)).thenReturn(1L);


        boolean result = redisProvider.delete(TEST_KEY);


        assertTrue(result);
        verify(mockJedis).del(TEST_KEY);
    }

    @Test
    void testExists() {

        when(mockJedis.exists(TEST_KEY)).thenReturn(true);


        boolean result = redisProvider.exists(TEST_KEY);


        assertTrue(result);
        verify(mockJedis).exists(TEST_KEY);
    }

    @Test
    void testExpire() {

        when(mockJedis.expire(TEST_KEY, 60)).thenReturn(1L);


        boolean result = redisProvider.expire(TEST_KEY, 60);


        assertTrue(result);
        verify(mockJedis).expire(TEST_KEY, 60);
    }

    @Test
    void testCloseConnection() {
        redisProvider.closeConnection();
        verify(mockJedisPool).close();
    }
} 