package com.paicbd.smsc.utils;

import lombok.Generated;
import lombok.Getter;

@Getter
@Generated
public enum RegistrationType {
    DE_REGISTRATION(0),
    INITIAL_REGISTRATION(1),
    RE_REGISTRATION(2);

    private final int code;

    RegistrationType(int code) {
        this.code = code;
    }

    public static RegistrationType fromCode(int code) {
        for (RegistrationType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown RegistrationType code: " + code);
    }
}
