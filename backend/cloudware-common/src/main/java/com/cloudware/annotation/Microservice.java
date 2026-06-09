package com.cloudware.annotation;

import com.cloudware.config.MicroserviceCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(MicroserviceCondition.class)
public @interface Microservice {
    String value();
}
