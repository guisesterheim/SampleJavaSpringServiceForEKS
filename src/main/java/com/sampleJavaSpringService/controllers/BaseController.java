package com.sampleJavaSpringService.controllers;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class BaseController {

    @Autowired
    protected Logger logger;

    @Value("${app.kubernetes.URLRedirect}")
    protected String kubernetesURLRedirection;

    @Value("${app.version}")
    protected String appVersion;

    public ResponseEntity ok(Object obj){
        return ResponseEntity.ok(obj);
    }

    public ResponseEntity ok(List<?> obj){
        return ResponseEntity.ok(obj);
    }

    public ResponseEntity error(){
        return ResponseEntity.badRequest().build();
    }

    public ResponseEntity error(Object message){
        return ResponseEntity.badRequest().body(message);
    }

}