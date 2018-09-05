package com.capitalone.dashboard.auth.access;

import java.lang.annotation.*;

/**
 * Annotation to mark Rest API endpoints that can be accessed without auth
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface PermitAll {
}
