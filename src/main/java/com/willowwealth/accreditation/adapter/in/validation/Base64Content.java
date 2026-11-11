package com.willowwealth.accreditation.adapter.in.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = Base64ContentValidator.class)
public @interface Base64Content {

    String message() default "must be Base64 encoded and at most {maxBytes} bytes once decoded";

    int maxBytes();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
