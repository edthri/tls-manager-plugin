package org.openintegrationengine.tlsmanager.shared.models;

import lombok.Getter;

public enum RevocationMode {
    DISABLED("Disabled"),
    SOFT_FAIL("Soft Fail"),
    HARD_FAIL("Hard Fail");

    @Getter
    private final String displayText;

    RevocationMode(String displayText) {
        this.displayText = displayText;
    }
}
