package com.paicbd.smsc.utils;

import lombok.Getter;
import org.jsmpp.bean.TypeOfNumber;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;

@Getter
@Generated
public enum CustomTypeOfNumber {

    UNKNOWN(TypeOfNumber.UNKNOWN, NatureOfAddress.UNKNOWN, AddressNature.unknown, org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber.Unknown),
    INTERNATIONAL(TypeOfNumber.INTERNATIONAL, NatureOfAddress.INTERNATIONAL, AddressNature.international_number, org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber.InternationalNumber),
    NATIONAL(TypeOfNumber.NATIONAL, NatureOfAddress.NATIONAL, AddressNature.national_significant_number, org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber.NationalNumber),
    NETWORK_SPECIFIC(TypeOfNumber.NETWORK_SPECIFIC, NatureOfAddress.INTERNATIONAL, AddressNature.network_specific_number, org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber.NetworkSpecificNumber),
    SUBSCRIBER_NUMBER(TypeOfNumber.SUBSCRIBER_NUMBER, NatureOfAddress.SUBSCRIBER, AddressNature.subscriber_number, org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber.SubscriberNumber),
    ALPHANUMERIC(TypeOfNumber.ALPHANUMERIC, NatureOfAddress.INTERNATIONAL, AddressNature.reserved, org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber.Alphanumeric),
    ABBREVIATED(TypeOfNumber.ABBREVIATED, NatureOfAddress.INTERNATIONAL, AddressNature.abbreviated_number, org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber.AbbreviatedNumber);

    private final TypeOfNumber smscValue;
    private final NatureOfAddress indicatorValue;
    private final AddressNature primitiveValue;
    private final org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber smsTpduValue;

    CustomTypeOfNumber(TypeOfNumber smscValue, NatureOfAddress indicatorValue, AddressNature primitiveValue,
                       org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber smsTpduValue) {
        this.smscValue = smscValue;
        this.indicatorValue = indicatorValue;
        this.primitiveValue = primitiveValue;
        this.smsTpduValue = smsTpduValue;

    }

    public static CustomTypeOfNumber fromSmsc(byte value) {
        var smscTypeOfNumber = TypeOfNumber.valueOf(value);
        return switch (smscTypeOfNumber) {
            case UNKNOWN -> CustomTypeOfNumber.UNKNOWN;
            case INTERNATIONAL -> CustomTypeOfNumber.INTERNATIONAL;
            case NATIONAL -> CustomTypeOfNumber.NATIONAL;
            case NETWORK_SPECIFIC -> CustomTypeOfNumber.NETWORK_SPECIFIC;
            case SUBSCRIBER_NUMBER -> CustomTypeOfNumber.SUBSCRIBER_NUMBER;
            case ALPHANUMERIC -> CustomTypeOfNumber.ALPHANUMERIC;
            case ABBREVIATED -> CustomTypeOfNumber.ABBREVIATED;
        };
    }

    public static CustomTypeOfNumber fromSmsTpdu(org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber smsTpduTypeOfNumber) {
        for (CustomTypeOfNumber enumValue : values()) {
            if (enumValue.getSmsTpduValue().equals(smsTpduTypeOfNumber)) {
                return enumValue;
            }
        }
        return UNKNOWN;
    }

    public static CustomTypeOfNumber fromPrimitive(AddressNature primitiveNatureOfAddress) {
        for (CustomTypeOfNumber enumValue : values()) {
            if (enumValue.getPrimitiveValue().equals(primitiveNatureOfAddress)) {
                return enumValue;
            }
        }
        return UNKNOWN;
    }

    public static CustomTypeOfNumber fromIndicator(NatureOfAddress natureOfAddress) {
        for (CustomTypeOfNumber enumValue : values()) {
            if (enumValue.getIndicatorValue().equals(natureOfAddress)) {
                return enumValue;
            }
        }
        return UNKNOWN;
    }


}
