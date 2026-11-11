package com.willowwealth.accreditation.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

// Requests have to match the documented shape exactly; Jackson is lenient by default.
@Singleton
public class StrictJsonCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        mapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        // Otherwise "user_id": 123 would quietly become the string "123".
        mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
    }
}
