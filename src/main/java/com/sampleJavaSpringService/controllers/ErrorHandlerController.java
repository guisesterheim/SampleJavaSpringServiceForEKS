package com.sampleJavaSpringService.controllers;

import com.sampleJavaSpringService.exceptions.ApiException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@ApiIgnore
@ControllerAdvice(basePackageClasses = StatusController.class)
public class ErrorHandlerController implements ErrorController {

    @Value("${app.kubernetes.URLRedirect}")
    private String kubernetesURLRedirection;

    @Override
    public String getErrorPath() {
        return "/error";
    }

    @RequestMapping("/error")
    public void handleErrorWithRedirect(HttpServletResponse response) throws IOException {
        response.sendRedirect(this.kubernetesURLRedirection+"/swagger/swagger-ui.html");
    }

    @RequestMapping(value = "/")
    public void redirect(HttpServletResponse response) throws IOException {
        response.sendRedirect(this.kubernetesURLRedirection+"/swagger/swagger-ui.html");
    }

    @Autowired
    private Logger logger;

    @ExceptionHandler(Exception.class)
    @ResponseBody
    ResponseEntity<?> handleControllerException(HttpServletRequest request, Throwable ex) {
        logger.error("Error in controller");
        logger.error("Error Throwable: ", ex);

        //TODO: send a notification on slack

        List<String> errors = Arrays.asList(ex.getStackTrace())
                .stream()
                .map(stackTraceElement -> stackTraceElement.toString())
                .collect(Collectors.toList());

        ApiException apiException = new ApiException(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
        return new ResponseEntity<ApiException>(apiException, new HttpHeaders(), apiException.getStatus());
    }
}