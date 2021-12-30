package com.sampleJavaSpringService.services;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HealthCheckService {

    @Autowired
    private Logger logger;

    public boolean healthCheck(){
        logger.debug("Starting HealthCheckService healthcheck");

        logger.debug("Finished health check");
        return true;
    }

}