package org.mhcommerce.dbmanager.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerConfig {


    @Value("${dbmanager.docker.socket}")
    private String dockerSocket;

    @Bean
    public DockerClient dockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(dockerSocket)
            .withDockerTlsVerify(dockerSocket.startsWith("tcp"))
            .build();

        return DockerClientBuilder.getInstance(config).build();
    }
}