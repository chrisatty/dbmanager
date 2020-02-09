package org.mhcommerce.dbmanager.file.archivers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.mhcommerce.dbmanager.exceptions.ExecutionException;
import org.mhcommerce.dbmanager.file.FileArchiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

@Component
@Profile("aws")
public class S3Archiver implements FileArchiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Archiver.class);


    private AmazonS3 s3client;
    private String bucketName;

    @Autowired
    public S3Archiver(AmazonS3 s3client, @Value("${dbmanager.aws.bucket}") String bucketName) {
        this.s3client = s3client;
        this.bucketName = bucketName;
    }

    @Override
    public URI archive(File file) {
        LOGGER.info("Archiving file " + file.getName() + " to AWS bucket " + bucketName);

        s3client.putObject(new PutObjectRequest(bucketName, file.getName(), file)
                .withCannedAcl(CannedAccessControlList.BucketOwnerFullControl));
        try {
            return s3client.getUrl(bucketName, file.getName()).toURI();
        } catch (URISyntaxException e) {
            throw new ExecutionException("Could not obtain S3 URL", e);
        }
    }
}