package org.mhcommerce.dbmanager.mysql;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@Profile("mysql")
public class MySqlConfig {

    @Value("${dbmanager.mysql.host}")
    private String host;
    
    @Value("${dbmanager.mysql.port}")
    private int port;
    
    @Value("${dbmanager.mysql.username}")
    private String adminUser;
    
    @Value("${dbmanager.mysql.password}")
    private String adminPass;
    

    @Bean
    public JdbcTemplate jdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(String.format("jdbc:mysql://%s:%d", host, port));
        dataSource.setUsername(adminUser);
        dataSource.setPassword(adminPass);
        return new JdbcTemplate(dataSource);
    }

}