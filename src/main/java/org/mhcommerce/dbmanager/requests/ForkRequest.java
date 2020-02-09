package org.mhcommerce.dbmanager.requests;

import java.util.Optional;

public class ForkRequest {

    private String forkedName;
    private Optional<String> callbackUrl;

    public ForkRequest(String forkedName, Optional<String> callbackUrl) {
        this.forkedName = forkedName;
        this.callbackUrl = callbackUrl;
    }

    public String getForkedName() {
        return forkedName;
    }

    public Optional<String> getCallbackUrl() {
        return callbackUrl;
    }
    
}