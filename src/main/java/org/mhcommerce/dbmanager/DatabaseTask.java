package org.mhcommerce.dbmanager;

import java.util.concurrent.Callable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTask.class);

    @JsonIgnore
    private final Runnable runnable;
    @JsonIgnore
    private final Callable<? extends Object> callable;

    private final String name;
    private final String task;
    private long start;
    private long end;
    private boolean success = false; // false until we've actually executed
    private String message;
    private Object result = null;

    public DatabaseTask(Runnable runnable, String name, String task) {
        this.runnable = runnable;
        this.callable = null;
        this.name = name;
        this.task = task;
    }

    public DatabaseTask(Callable<? extends Object> callable, String name, String task) {
        this.callable = callable;
        this.runnable = null;
        this.name = name;
        this.task = task;
    }

    public String getName() {
        return name;
    }

    public String getTask() {
        return task;
    }

    public void run() {
        start = System.currentTimeMillis();
        LOGGER.info("Running " + task + " on database " + name);
        try {
            if (runnable != null) {
                runnable.run();
            } else {
                result = callable.call();
            }
            success = true;
        } catch(Exception e) {
            LOGGER.error("Async exceution failed", e);
            message = e.getMessage();
        } finally {
            end = System.currentTimeMillis();
        }
    }

    public Object getResult() {
        return result;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}