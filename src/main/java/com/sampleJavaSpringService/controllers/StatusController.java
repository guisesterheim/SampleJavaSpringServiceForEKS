package com.sampleJavaSpringService.controllers;

import com.sampleJavaSpringService.services.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.kubernetes.URLRedirect}/v${app.version}/status")
public class StatusController extends BaseController {

    @Autowired
    private HealthCheckService healthCheckService;

    @GetMapping
    @RequestMapping(value = "/version", method = RequestMethod.GET)
    public ResponseEntity getAppVersion(){
        logger.debug("Starting "+this.kubernetesURLRedirection+"/api/v"+this.appVersion+"/status/version getAppVersion");
        logger.debug("App version = "+this.appVersion);
        return ok(this.appVersion);
    }

    @GetMapping
    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public ResponseEntity health(){
        try {
            logger.debug("Starting "+this.kubernetesURLRedirection+"/api/v"+this.appVersion+"/status/health healthCheck");

            boolean success = healthCheckService.healthCheck();

            logger.info("Finished healthCheck successfully");

            if(success)
                return ok("OK");
            return error("Error on healtcheck");
        }catch(Exception e) {
            logger.error("Error on healthcheck", e);
            return error(e.getMessage());
        }
    }
}