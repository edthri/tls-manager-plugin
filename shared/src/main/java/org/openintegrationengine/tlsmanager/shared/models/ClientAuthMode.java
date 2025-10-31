package org.openintegrationengine.tlsmanager.shared.models;

import lombok.Getter;

public enum ClientAuthMode implements DisplayTextEnum {
    NONE("None"),
    REQUESTED("Requested"),
    REQUIRED("Required");

    @Getter
    private final String displayText;

    ClientAuthMode(String displayText) {
        this.displayText = displayText;
    }
}
