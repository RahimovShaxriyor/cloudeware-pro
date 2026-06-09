package com.cloudware.config;

import com.cloudware.annotation.Microservice;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class MicroserviceCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(Microservice.class.getName());
        if (attrs == null) return false;
        String required = String.valueOf(attrs.get("value"));
        String current = context.getEnvironment().getProperty("cloudware.service", "");
        return required.equals(current);
    }
}
