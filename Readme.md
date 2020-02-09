# Dev By Devs database manager

## Build

You'll need maven and docker installed.

```bash
mvn package
docker build -t dbmanager
```

## Run

An example of how to run the docker container is below with the minimum required environment variables.

```bash
docker run -e "MYSQL_PASSWORD=password" -e "AWS_ACCESS_KEY=accesskey" -e "AWS_SECRET_KEY=secretkey" -e "AWS_BUCKET=bucket" -v /path/to/scripts:/dbscripts -v /var/run/docker.sock:/var/run/docker.sock -p8080:8080 -d dbmanager 
```

### Environment Variables

Note: if there is no default then the environment variable MUST be given

#### MySQL

| Property | Default
| --- | --- |
| MYSQL_HOST | 127.0.0.1 |
| MYSQL_PORT | 3306 |
| MYSQL_USER | root |
| MYSQL_PASSWORD |  |

#### AWS

| Property | Default
| --- | --- |
| AWS_ACCESS_KEY | |
| AWS_SECRET_KEY | |
| AWS_REGION | eu-west-2 |
| AWS_BUCKET | |

#### OTHER

| Property | Default | Description |
| --- | --- | --- |
| SCRIPT_FOLDER | /dbscripts | The folder where database scripts are located |
| SCRIPT_DELETE | true | Whether to delete database scripts after successful execution |
| CONCURRENT_THREADS | 2 | Max number of concurrent threads |
| BACKUP_COMPRESSION | gz | Compression of backup file - as file extension |
| DOCKER_SOCKET | unix:///var/run/docker.sock | Location of the docker socket |


## Endpoints

| Endpoint | Example body | Description |
| --- | --- | --- |
| POST /database | { "name": "mydb" } | Creates a database with associated user with the same name. Password created as docker secret |
| GET /database | | Get the names of all the databases |
| DELETE /database/\<name\>/ | | Delete database and associated user |
| POST /database/\<name\>/execute | { "file": "my_script.sql", "callbackUrl": "http://mycallback.com" } | Execute the given script on a database. This is done asynchronously with a optional callback to a URL once complete. This will also decompress the script file if required |
| POST /database/\<name\>/fork | { "forkedName": "my_new_db", "callbackUrl": "http://mycallback.com" } | Fork a database. This is done asynchronously with a optional callback to a URL once complete |
| POST /database/\<name\>/backup | { "callbackUrl": "http://mycallback.com" } | Backup a database. This is done asynchronously with a optional callback to a URL once complete |

## Callbacks

If a callback URL is given, once the given task is complete, the application will try and POST to the given URL with the following JSON.

### Fork

```json
{
    "name": "database_name",
    "task": "fork",
    "success" : true,
    "start": 1581277195665,
    "end": 1581277195666,
    "message": "This is null unless success is false",
    "result": {
        "host": "databasehost.com",
        "port": 3306,
        "name": "new_db",
        "username": "new_db",
        "secret": "docker_secret_id"
    }
}
```

### Backup

```json
{
    "name": "database_name",
    "task": "backup",
    "success" : true,
    "start": 1581277195665,
    "end": 1581277195666,
    "message": "This is null unless success is false",
    "result": "http://theurltomybackup.com/file.sql.gz"
}
```

### Execute

```json
{
    "name": "database_name",
    "task": "execute",
    "success" : true,
    "start": 1581277195665,
    "end": 1581277195666,
    "message": "This is null unless success is false",
    "result": null
}
```