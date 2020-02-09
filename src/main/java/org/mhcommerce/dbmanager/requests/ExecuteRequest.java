package org.mhcommerce.dbmanager.requests;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ExecuteRequest {

    private final String file;
    private final Optional<String> callbackUrl;

    @JsonCreator
    public ExecuteRequest(String file, Optional<String> callbackUrl) {
        this.file = file;
        this.callbackUrl = callbackUrl;
    }

    public String getFile() {
        return file;
    }

    public Optional<String> getCallbackUrl() {
        return callbackUrl;
    }

}