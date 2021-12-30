package com.sampleJavaSpringService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.kubernetes.URLRedirect}")
    private String kubernetesURLRedirection;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("redirect:"+this.kubernetesURLRedirection+"/swagger/swagger-ui.html");
    }
}