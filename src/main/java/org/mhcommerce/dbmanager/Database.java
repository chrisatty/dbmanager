package org.mhcommerce.dbmanager;

public class Database {

    private final String host;
    private final int port;
    private final String name;
    private final String username;
    private final String secret;



    public Database(String host, int port, String name, String username, String secret) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.username = username;
        this.secret = secret;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getSecret() {
        return secret;
    }
}