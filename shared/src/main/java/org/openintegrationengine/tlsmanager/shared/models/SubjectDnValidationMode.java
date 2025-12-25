package org.openintegrationengine.tlsmanager.shared.models;

import lombok.Getter;

public enum SubjectDnValidationMode implements DisplayTextEnum {
    NONE("None"),
    PARTIAL("Partial"),
    EXACT("Exact");

    @Getter
    private final String displayText;

    SubjectDnValidationMode(String displayText) {
        this.displayText = displayText;
    }
}
