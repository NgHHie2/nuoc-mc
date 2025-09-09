package com.example.learnservice.annotation;

import com.example.learnservice.enums.Role;
import java.lang.annotation.*;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    Role[] value(); // cho phép truyền nhiều role
}
