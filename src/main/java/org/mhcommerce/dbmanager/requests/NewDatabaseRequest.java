package org.mhcommerce.dbmanager.requests;

import com.fasterxml.jackson.annotation.JsonCreator;

public class NewDatabaseRequest {

    private final String name;

    @JsonCreator
    public NewDatabaseRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}