package org.mhcommerce.dbmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class AsyncRunnerTest {

    @Mock
    private RestTemplate restTemplate;

    private static ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    @AfterClass
    public static void shutdown() {
        threadPool.shutdown();
    }

    @Test
    public void testRunnableCompletes() throws Exception {
        AsyncJobRunner asyncRunner = new AsyncJobRunner(restTemplate, threadPool);

        Runnable runnable = () -> { try { Thread.sleep(100L); } catch (Exception e) {} };
        Future<DatabaseTask> future = asyncRunner.submit(new DatabaseTask(runnable, "dbName", "Test description"));
        DatabaseTask task = future.get();

        assertTrue(task.getSuccess());
        assertTrue(task.getStart() < task.getEnd());
        assertNull(task.getResult());
        assertNull(task.getMessage());
    }

    @Test
    public void testCallableCompletes() throws Exception {
        AsyncJobRunner asyncRunner = new AsyncJobRunner(restTemplate, threadPool);

        Callable<String> callable = () -> { try { Thread.sleep(100L); return "test"; } catch (Exception e) { return "test"; } };
        Future<DatabaseTask> future = asyncRunner.submit(new DatabaseTask(callable, "dbName", "Test description"));
        DatabaseTask task = future.get();
        
        assertTrue(task.getSuccess());
        assertTrue(task.getStart() < task.getEnd());
        assertEquals("test", task.getResult());
        assertNull(task.getMessage());
    }

    @Test
    public void testFailsOnException() throws Exception {
        AsyncJobRunner asyncRunner = new AsyncJobRunner(restTemplate, threadPool);
        Future<DatabaseTask> future = asyncRunner.submit(
            new DatabaseTask(() -> { throw new RuntimeException("test message"); }, "dbName", "Test description"));
        DatabaseTask task = future.get();
        assertFalse(task.getSuccess());
        assertEquals("test message", task.getMessage());
    }

    @Test
    public void testCallback() throws Exception {
        AsyncJobRunner asyncRunner = new AsyncJobRunner(restTemplate, threadPool);
        Future<DatabaseTask> future = asyncRunner.submit(
            new DatabaseTask(() -> { }, "dbName", "Test description"), new URL("http://test.com"));
        DatabaseTask task = future.get();
        assertTrue(task.getSuccess());
        verify(restTemplate, times(1)).postForEntity(eq("http://test.com"), eq(task), eq(String.class));
    }
}