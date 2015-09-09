package org.eck.mojo.internal;

import org.apache.maven.plugin.logging.Log;

public class Console {
    private Log log;

    public Console(Log log) {
        this.log = log;
    }

    public void log(String message) {
        log.info(message);
    }

    public void info(String message) {
        log.info(message);
    }

    public void error(String message) {
        log.error(message);
    }
}
