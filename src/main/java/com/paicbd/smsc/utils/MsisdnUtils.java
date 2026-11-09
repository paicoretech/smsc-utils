package com.paicbd.smsc.utils;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.Objects;

@UtilityClass
public class MsisdnUtils {

    private static final String MSISDN_PREFIX = "msisdn-";

    public static String extract(String gpsi) {
        if (Objects.isNull(gpsi) || gpsi.isBlank()) {
            return null;
        }

        String normalized = gpsi.trim();
        if (!normalized.toLowerCase(Locale.ROOT).startsWith(MSISDN_PREFIX)) {
            return null;
        }

        return normalized.substring(MSISDN_PREFIX.length());
    }

    public static String normalize(String msisdn) {
        msisdn = msisdn.trim();

        if (msisdn.startsWith(MSISDN_PREFIX)) { return msisdn;}
        if (msisdn.startsWith("+")) { return MSISDN_PREFIX + msisdn.substring(1);}
        return MSISDN_PREFIX + msisdn;
    }
}
