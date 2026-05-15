package com.paicbd.smsc.utils;

import lombok.Getter;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.restcomm.protocols.ss7.indicator.NumberingPlan;
import org.restcomm.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;

@Getter
@Generated
public enum CustomNumberingPlanIndicator {

    UNKNOWN(NumberingPlanIndicator.UNKNOWN, NumberingPlan.UNKNOWN, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.unknown, NumberingPlanIdentification.Unknown),
    ISDN(NumberingPlanIndicator.ISDN, NumberingPlan.ISDN_TELEPHONY, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, NumberingPlanIdentification.ISDNTelephoneNumberingPlan),
    DATA(NumberingPlanIndicator.DATA, NumberingPlan.DATA, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.data, NumberingPlanIdentification.DataNumberingPlan),
    TELEX(NumberingPlanIndicator.TELEX, NumberingPlan.TELEX, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.telex, NumberingPlanIdentification.TelexNumberingPlan),
    LAND_MOBILE(NumberingPlanIndicator.LAND_MOBILE, NumberingPlan.LAND_MOBILE, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.land_mobile, NumberingPlanIdentification.ServiceCentreSpecificPlan2),
    NATIONAL(NumberingPlanIndicator.NATIONAL, NumberingPlan.GENERIC, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.national, NumberingPlanIdentification.NationalNumberingPlan),
    PRIVATE(NumberingPlanIndicator.PRIVATE, NumberingPlan.PRIVATE, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.private_plan, NumberingPlanIdentification.PrivateNumberingPlan),
    ERMES(NumberingPlanIndicator.ERMES, NumberingPlan.UNKNOWN, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.unknown, NumberingPlanIdentification.ERMESNumberingPlan),
    INTERNET(NumberingPlanIndicator.INTERNET, NumberingPlan.UNKNOWN, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.unknown, NumberingPlanIdentification.Unknown),
    WAP(NumberingPlanIndicator.WAP, NumberingPlan.UNKNOWN, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.unknown, NumberingPlanIdentification.Unknown);

    private final NumberingPlanIndicator smscValue;
    private final NumberingPlan indicatorValue;
    private final org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan primitiveValue;
    private final NumberingPlanIdentification smsTpduValue;

    CustomNumberingPlanIndicator(NumberingPlanIndicator smscValue, NumberingPlan indicatorValue,
                                 org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan primitiveValue,
                                 NumberingPlanIdentification smsTpduValue) {
        this.smscValue = smscValue;
        this.indicatorValue = indicatorValue;
        this.primitiveValue = primitiveValue;
        this.smsTpduValue = smsTpduValue;
    }


    public static CustomNumberingPlanIndicator fromSmsc(byte value) {
        var smscNumberingPlanIndicator = NumberingPlanIndicator.valueOf(value);
        return switch (smscNumberingPlanIndicator) {
            case UNKNOWN -> CustomNumberingPlanIndicator.UNKNOWN;
            case ISDN -> CustomNumberingPlanIndicator.ISDN;
            case DATA -> CustomNumberingPlanIndicator.DATA;
            case TELEX -> CustomNumberingPlanIndicator.TELEX;
            case LAND_MOBILE -> CustomNumberingPlanIndicator.LAND_MOBILE;
            case NATIONAL -> CustomNumberingPlanIndicator.NATIONAL;
            case PRIVATE -> CustomNumberingPlanIndicator.PRIVATE;
            case ERMES -> CustomNumberingPlanIndicator.ERMES;
            case INTERNET -> CustomNumberingPlanIndicator.INTERNET;
            case WAP -> CustomNumberingPlanIndicator.WAP;
        };
    }

    public static CustomNumberingPlanIndicator fromSmsTpdu(NumberingPlanIdentification smsTpduNumberingPlanIdentification) {
        for (CustomNumberingPlanIndicator enumValue : values()) {
            if (enumValue.getSmsTpduValue().equals(smsTpduNumberingPlanIdentification)) {
                return enumValue;
            }
        }
        return UNKNOWN;
    }

    public static CustomNumberingPlanIndicator fromPrimitive(org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan primitiveNumberingPlan) {
        for (CustomNumberingPlanIndicator enumValue : values()) {
            if (enumValue.getPrimitiveValue().equals(primitiveNumberingPlan)) {
                return enumValue;
            }
        }
        return UNKNOWN;
    }

    public static CustomNumberingPlanIndicator fromIndicator(NumberingPlan numberingPlan) {
        for (CustomNumberingPlanIndicator enumValue : values()) {
            if (enumValue.getIndicatorValue().equals(numberingPlan)) {
                return enumValue;
            }
        }
        return UNKNOWN;
    }
}
