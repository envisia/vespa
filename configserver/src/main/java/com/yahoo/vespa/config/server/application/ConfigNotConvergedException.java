// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.config.server.application;

/**
 * @author lulf
 */
public class ConfigNotConvergedException extends RuntimeException {
    public ConfigNotConvergedException(String message) {
        super(message);
    }
}
