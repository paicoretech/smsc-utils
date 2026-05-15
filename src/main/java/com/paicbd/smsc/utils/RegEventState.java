package com.paicbd.smsc.utils;

import lombok.Generated;
import lombok.Getter;

@Getter
@Generated
public enum RegEventState {
    PENDING(0),
    ACTIVE(1),
    FAILED(2),
    EXPIRED(3);

    private final int code;

    RegEventState(int code) {
        this.code = code;
    }

    public static RegEventState fromCode(int code) {
        for (RegEventState state : values()) {
            if (state.code == code) return state;
        }
        throw new IllegalArgumentException("Unknown RegEventState code: " + code);
    }
}
