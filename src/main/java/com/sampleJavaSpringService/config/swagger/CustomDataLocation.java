package com.sampleJavaSpringService.config.swagger;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import io.swagger.models.Swagger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponents;
import springfox.documentation.annotations.ApiIgnore;
import springfox.documentation.service.Documentation;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.json.Json;
import springfox.documentation.spring.web.json.JsonSerializer;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.*;
import springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static springfox.documentation.swagger.common.HostNameProvider.componentsFrom;

@Controller
@ApiIgnore
@RequestMapping("${app.kubernetes.URLRedirect}/swagger")
public class CustomDataLocation {

    @Value("${app.kubernetes.URLRedirect}")
    private String kubernetesURLRedirection;

    @Autowired(required = false)
    private SecurityConfiguration securityConfiguration;
    @Autowired(required = false)
    private UiConfiguration uiConfiguration;
    @Autowired
    private JsonSerializer jsonSerializer;
    @Autowired
    private DocumentationCache documentationCache;
    @Autowired
    private ServiceModelToSwagger2Mapper mapper;

    private final String hostNameOverride = "DEFAULT";

    private final SwaggerResourcesProvider swaggerResources;

    @Autowired
    public CustomDataLocation(SwaggerResourcesProvider swaggerResources) {
        this.swaggerResources = swaggerResources;
    }

    @RequestMapping(value = "/")
    public void redirect(HttpServletResponse response) throws IOException { }

    @RequestMapping(value = "/swagger-resources/configuration/security")
    @ResponseBody
    public ResponseEntity<SecurityConfiguration> securityConfiguration() {
        return new ResponseEntity<SecurityConfiguration>(
                Optional.fromNullable(securityConfiguration).or(SecurityConfigurationBuilder.builder().build()), HttpStatus.OK);
    }

    @RequestMapping(value = "/swagger-resources/configuration/ui")
    @ResponseBody
    public ResponseEntity<UiConfiguration> uiConfiguration() {
        return new ResponseEntity<UiConfiguration>(
                Optional.fromNullable(uiConfiguration).or(UiConfigurationBuilder.builder().build()), HttpStatus.OK);
    }

    @RequestMapping(value = "/v2/api-docs")
    @ResponseBody
    public ResponseEntity<Json> apidocsConfiguration(@RequestParam(value = "group", required = false) String swaggerGroup,
                                                                                HttpServletRequest servletRequest) {

        String groupName = Optional.fromNullable(swaggerGroup).or(Docket.DEFAULT_GROUP_NAME);
        Documentation documentation = documentationCache.documentationByGroup(groupName);
        if (documentation == null) {
            //LOGGER.warn("Unable to find specification for group {}", groupName);
            return new ResponseEntity<Json>(HttpStatus.NOT_FOUND);
        }
        Swagger swagger = mapper.mapDocumentation(documentation);
        UriComponents uriComponents = componentsFrom(servletRequest, swagger.getBasePath());
        swagger.basePath(Strings.isNullOrEmpty(uriComponents.getPath()) ? "/" : uriComponents.getPath());
        if (isNullOrEmpty(swagger.getHost())) {
            swagger.host(hostName(uriComponents));
        }
        return new ResponseEntity<Json>(jsonSerializer.toJson(swagger), HttpStatus.OK);
    }

    @RequestMapping(value = "/swagger-resources")
    @ResponseBody
    public ResponseEntity<List<SwaggerResource>> swaggerResources() {
        return new ResponseEntity<List<SwaggerResource>>(swaggerResources.get(), HttpStatus.OK);
    }

    private String hostName(UriComponents uriComponents) {
        if ("DEFAULT".equals(hostNameOverride)) {
            String host = uriComponents.getHost();
            int port = uriComponents.getPort();
            if (port > -1) {
                return String.format("%s:%d", host, port);
            }
            return host;
        }
        return hostNameOverride;
    }
}
