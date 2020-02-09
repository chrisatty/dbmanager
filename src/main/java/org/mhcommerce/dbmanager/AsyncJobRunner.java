package org.mhcommerce.dbmanager;

import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AsyncJobRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncJobRunner.class);

    private final ThreadPoolExecutor threadPool;
    private final RestTemplate restTemplate;

    @Autowired
    public AsyncJobRunner(RestTemplate restTemplate, ThreadPoolExecutor threadPool) {
        this.restTemplate = restTemplate;
        this.threadPool = threadPool;
    }

    public Future<DatabaseTask> submit(DatabaseTask job, URL callbackUrl) {

        return threadPool.submit(() -> {
            job.run();
            try {
                LOGGER.debug("Callback to URL " + callbackUrl.toString());
                restTemplate.postForEntity(callbackUrl.toString(), job, String.class);
            } catch (Exception e) {
                LOGGER.warn("Could not post callback to " + callbackUrl.toString(), e);
            }
            return job;
        });
    }

    public Future<DatabaseTask> submit(DatabaseTask job) {
        return threadPool.submit(() -> {
            job.run();
            return job;
        });
    }

    @PreDestroy
    public void stopAll() {
        threadPool.shutdown();
    }
}