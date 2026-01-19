package com.demo.commons.logging;

import java.util.UUID;

public class CorrelationIdUtil {

    public static final String CORRELATION_ID = "correlation_id";

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}

