package org.mhcommerce.dbmanager.requests;

import java.util.Optional;

public class BackupRequest {

    private Optional<String> callbackUrl;

    public BackupRequest(String callbackUrl) {
        this.callbackUrl = Optional.of(callbackUrl);
    }

    public BackupRequest() {
        this.callbackUrl = Optional.empty();
    }

    public Optional<String> getCallbackUrl() {
        return callbackUrl;
    }
    
}