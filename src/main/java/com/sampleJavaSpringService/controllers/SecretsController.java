package com.sampleJavaSpringService.controllers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;
import com.google.common.base.Charsets;
import com.sampleJavaSpringService.services.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("${app.kubernetes.URLRedirect}/v${app.version}/status")
public class SecretsController extends BaseController {

    @Value("${app.aws_access_key}")
    private String AWS_ACCESS_KEY;

    @Value("${app.aws_secret_key}")
    private String AWS_SECRET_KEY;

    @GetMapping
    @RequestMapping(value = "/dynamicSecret", method = RequestMethod.GET)
    public ResponseEntity getDynamicSecret(){
        logger.debug("Starting "+this.kubernetesURLRedirection+"/api/v"+this.appVersion+"/status/version getAppVersion");
        logger.debug("App version = "+this.appVersion);

        String secretName = "arn:aws:secretsmanager:us-east-1:594483618195:secret:fs_simple_secret-tgSUnW";
        String region = "us-east-1";

        // Create a Secrets Manager client
        AWSSecretsManager client  = AWSSecretsManagerClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)))
                .build();

        String secret, decodedBinarySecret;
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                .withSecretId(secretName);
        GetSecretValueResult getSecretValueResult = null;

        try {
            getSecretValueResult = client.getSecretValue(getSecretValueRequest);
        } catch (Exception e) {
            e.printStackTrace();
            return ok("No value found");
        }

        // Decrypts secret using the associated KMS key.
        // Depending on whether the secret is a string or binary, one of these fields will be populated.
        if (getSecretValueResult.getSecretString() != null) {
            secret = getSecretValueResult.getSecretString();
            return ok(secret);
        }
        else {
            decodedBinarySecret = new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
            return ok(decodedBinarySecret);
        }
    }

    @GetMapping
    @RequestMapping(value = "/localSecret", method = RequestMethod.GET)
    public ResponseEntity getLocalSecrets(){
        logger.debug("Starting "+this.kubernetesURLRedirection+"/api/v"+this.appVersion+"/status/version getAppVersion");
        logger.debug("App version = "+this.appVersion);

        try {
            List<String> lines = Files.readAllLines(new File("/mnt/secrets-store/arn:aws:secretsmanager:us-east-1:594483618195:secret:fs_simple_secret-tgSUnW").toPath(), Charsets.UTF_8);
            return ok(lines);
        }catch(Exception e){
            System.out.println("Error reading files");
        }

        return ok("No credentials found")
    }
}