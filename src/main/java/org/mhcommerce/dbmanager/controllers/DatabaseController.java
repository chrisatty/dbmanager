package org.mhcommerce.dbmanager.controllers;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import org.mhcommerce.dbmanager.DatabaseTask;
import org.mhcommerce.dbmanager.exceptions.InvalidRequestException;
import org.mhcommerce.dbmanager.AsyncJobRunner;
import org.mhcommerce.dbmanager.Database;
import org.mhcommerce.dbmanager.requests.ExecuteRequest;
import org.mhcommerce.dbmanager.requests.ForkRequest;
import org.mhcommerce.dbmanager.requests.NewDatabaseRequest;
import org.mhcommerce.dbmanager.services.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("database")
public class DatabaseController {

    private DatabaseService databaseService;
    private AsyncJobRunner asyncRunner;
    private File scriptFolder;

    @Autowired
    public DatabaseController(DatabaseService databaseService, AsyncJobRunner asyncRunner,
                                @Qualifier("scriptFolder") File scriptFolder) {
        this.databaseService = databaseService;
        this.asyncRunner = asyncRunner;
        this.scriptFolder = scriptFolder;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Database create(@RequestBody NewDatabaseRequest newDbRequest) {
        return  databaseService.create(newDbRequest.getName());
    }

    @GetMapping
    public List<String> getDatbases() {
        return databaseService.getAll();
    }

    @PostMapping("/{name}/execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void executeScript(@PathVariable String name, @RequestBody ExecuteRequest executeRequest) {
        File file = Paths.get(scriptFolder.getAbsolutePath() + File.separator + executeRequest.getFile()).toFile();

        // validate request before submitting it
        databaseService.validateExecute(name, file);

        DatabaseTask job = new DatabaseTask(
            () -> { databaseService.execute(name, file); }, 
            name,
            "execute"
        );
    
        if (executeRequest.getCallbackUrl().isPresent()) {
            URL url;
            try {
                url = new URL(executeRequest.getCallbackUrl().get());
            } catch (Exception e) {
                throw new InvalidRequestException("Invalid callback URL " + executeRequest.getCallbackUrl().get());
            }
            asyncRunner.submit(job, url);
        } else {
            asyncRunner.submit(job);
        }
    }

    @PostMapping("/{name}/fork")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void fork(@PathVariable String name, @RequestBody ForkRequest forkRequest) {
        databaseService.validateFork(name, forkRequest.getForkedName());

        DatabaseTask job = new DatabaseTask(
            () -> { return databaseService.fork(name, forkRequest.getForkedName()); }, 
            forkRequest.getForkedName(),
            "fork"
        );
    
        if (forkRequest.getCallbackUrl().isPresent()) {
            URL url;
            try {
                url = new URL(forkRequest.getCallbackUrl().get());
            } catch (Exception e) {
                throw new InvalidRequestException("Invalid callback URL " + forkRequest.getCallbackUrl().get());
            }
            asyncRunner.submit(job, url);
        } else {
            asyncRunner.submit(job);
        }
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        databaseService.delete(name);
    }
}
