package org.mhcommerce.dbmanager.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ExecutionException extends RuntimeException {

    public ExecutionException(String msg) {
        super(msg);
    }
    
    public ExecutionException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
    public ExecutionException(Throwable cause) {
        super(cause);
    }
}