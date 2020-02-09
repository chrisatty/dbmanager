package org.mhcommerce.dbmanager.controllers;

import org.mhcommerce.dbmanager.DatabaseTask;

import java.net.URL;

import org.mhcommerce.dbmanager.AsyncJobRunner;
import org.mhcommerce.dbmanager.requests.BackupRequest;
import org.mhcommerce.dbmanager.services.BackupService;
import org.mhcommerce.dbmanager.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
public class BackupController {

    private BackupService backupService;
    private AsyncJobRunner asyncRunner;

    @Autowired
    public BackupController(BackupService backupService, AsyncJobRunner asyncRunner) {
        this.backupService = backupService;
        this.asyncRunner = asyncRunner;
    }

    
    @PostMapping("/database/{name}/backup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void backup(@PathVariable String name, @RequestBody(required=false) BackupRequest backupRequest) {
        backupService.validateRequest(name);

        DatabaseTask job = new DatabaseTask(
            () -> { backupService.backup(name); }, 
            name,
            "backup"
        );
    
        if (backupRequest != null && backupRequest.getCallbackUrl().isPresent()) {
            URL url;
            try {
                url = new URL(backupRequest.getCallbackUrl().get());
            } catch (Exception e) {
                throw new InvalidRequestException("Invalid callback URL " + backupRequest.getCallbackUrl().get());
            }
            asyncRunner.submit(job, url);
        } else {
            asyncRunner.submit(job);
        }
    }

}
