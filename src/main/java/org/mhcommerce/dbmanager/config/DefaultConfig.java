package org.mhcommerce.dbmanager.config;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class DefaultConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfig.class);

    @Value("${dbmanager.script.folder}")
    private String scriptFolder;

    @Value("${dbmanager.concurrentThreads}")
    private int concurrentThreads;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("scriptFolder")
    public File scriptFolder() {
        File dir = Paths.get(scriptFolder).toFile();        
        if (!dir.isDirectory()) {
            throw new BeanInitializationException("Cannot read from script folder");
        }
        LOGGER.info("Database script folder set to " + dir.getAbsolutePath());
        return dir;
    }

    @Bean
    public ThreadPoolExecutor threadPool() {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(concurrentThreads);
    }

}