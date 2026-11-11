package com.willowwealth.accreditation.adapter.in.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Base64;

public class Base64ContentValidator implements ConstraintValidator<Base64Content, String> {

    private int maxBytes;

    @Override
    public void initialize(Base64Content annotation) {
        maxBytes = annotation.maxBytes();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            return Base64.getDecoder().decode(value).length <= maxBytes;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
