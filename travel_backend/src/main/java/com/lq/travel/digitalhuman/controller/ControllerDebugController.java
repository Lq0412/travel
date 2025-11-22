package com.lq.travel.digitalhuman.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 接口调试Controller - 查看所有注册的接口
 */
@RestController
@RequestMapping("/debug")
public class ControllerDebugController {

    @Autowired
    private WebApplicationContext applicationContext;

    @GetMapping("/routes")
    public Map<String, Object> getAllRoutes() {
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();

        Map<String, Object> result = new HashMap<>();
        result.put("totalRoutes", handlerMethods.size());
        result.put("contextPath", applicationContext.getApplicationName());
        
        Map<String, String> routes = new HashMap<>();
        for (RequestMappingInfo info : handlerMethods.keySet()) {
            Set<String> paths = info.getPatternsCondition().getPatterns();
            Set<org.springframework.web.bind.annotation.RequestMethod> methods = info.getMethodsCondition().getMethods();
            
            for (String path : paths) {
                for (org.springframework.web.bind.annotation.RequestMethod method : methods) {
                    routes.put(method.name() + " " + path, 
                        handlerMethods.get(info).getBean().toString());
                }
            }
        }
        
        result.put("routes", routes);
        return result;
    }
}

