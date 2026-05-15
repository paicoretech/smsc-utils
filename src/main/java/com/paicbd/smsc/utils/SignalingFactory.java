package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.restcomm.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserData;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriod;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriodFormat;
import org.restcomm.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.restcomm.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.restcomm.protocols.ss7.map.smstpdu.UserDataImpl;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class SignalingFactory {

    protected UserData buildUserDataForMt(MessageEvent event, DataCodingScheme dataCodingScheme) {
        Integer dataCoding = Optional.ofNullable(event.getDataCoding()).orElse(0);
        if (EncodingUtils.GSM7_DATA_CODINGS.contains(dataCoding)) {
            String sm = Objects.equals(event.getDataCoding(), 0) ? event.getShortMessage()
                    : EncodingUtils.decodeMessage(event.getMessageBytes(), EncodingUtils.GSM7);
            var udh = new UserDataHeaderImpl(event.getUdhBytes());
            // setting null to gsm8Charset, since the smsc is not using this
            return new UserDataImpl(sm, dataCodingScheme, udh, null);
        }

        byte[] messageBytes = event.getUdhLength() > 0 ?
                EncodingUtils.prepend(event.getUdhBytes(), event.getMessageBytes()) :
                event.getMessageBytes();
        return new UserDataImpl(messageBytes, dataCodingScheme, messageBytes.length, event.getUdhLength() > 0, null);
    }

    protected void processUdhAndShortMessage(MessageEvent messageMo, UserData userData, boolean isUdh, int dataCoding, boolean isValidDataCoding) {
        byte[] encodedData = userData.getEncodedData();
        String decodedShortMessage;
        byte[] shortMessageBytes;

        int encodingType = EncodingUtils.getEncodingTypeFromDataCoding(dataCoding);

        if (isUdh) {
            EncodingUtils.parseUdh(encodedData, messageMo);
        } else {
            messageMo.setUdhLength(0);
            messageMo.setUdhBytes(new byte[0]);
        }

        if (isValidDataCoding) {
            decodedShortMessage = userData.getDecodedMessage();
        } else {
            decodedShortMessage = isUdh
                    ? EncodingUtils.bytesToHex(EncodingUtils.getCleanedBytes(encodedData))
                    : EncodingUtils.bytesToHex(encodedData);
        }
        shortMessageBytes = isUdh
                ? EncodingUtils.getCleanedBytes(encodedData)
                : EncodingUtils.encodeMessage(decodedShortMessage, encodingType);

        messageMo.setShortMessage(decodedShortMessage);
        messageMo.setMessageBytes(shortMessageBytes);
    }

    protected void setValidityPeriod(MessageEvent messageMo, SmsSubmitTpdu smsSubmitTpdu) {
        ValidityPeriod validityPeriod = smsSubmitTpdu.getValidityPeriod();
        ValidityPeriodFormat validityPeriodFormat = smsSubmitTpdu.getValidityPeriodFormat();

        if (Objects.nonNull(validityPeriod) && Objects.nonNull(validityPeriodFormat)) {
            switch (validityPeriodFormat) {
                case fieldPresentRelativeFormat -> {
                    long secondsValidityPeriod = (long) (validityPeriod.getRelativeFormatHours() * 3600);
                    messageMo.setValidityPeriod(secondsValidityPeriod);
                }
                case fieldPresentAbsoluteFormat -> {
                    AbsoluteTimeStamp absoluteTimeStamp = validityPeriod.getAbsoluteFormatValue();
                    Calendar calendarDate = toCalendar(absoluteTimeStamp);
                    long timeInMillis = calendarDate.getTimeInMillis();
                    int atsTimeZoneOffset = absoluteTimeStamp.getTimeZone() * 15 * 60;
                    int localTimeZoneOffset = -calendarDate.getTimeZone().getOffset(timeInMillis) / 1000;
                    long timeZoneDifferenceInMillis = (localTimeZoneOffset - atsTimeZoneOffset) * 1000L;
                    long adjustedTimeInMillis = timeInMillis + timeZoneDifferenceInMillis;
                    Calendar adjustedCalendar = Calendar.getInstance();
                    adjustedCalendar.setTimeInMillis(adjustedTimeInMillis);
                    long differenceInSeconds = (adjustedCalendar.getTimeInMillis() - calendarDate.getTimeInMillis()) / 1000;
                    messageMo.setValidityPeriod(differenceInSeconds);
                }
                default -> {
                    log.warn("Received unsupported ValidityPeriodFormat: {} - we set 80s", validityPeriodFormat);
                    messageMo.setValidityPeriod(80);
                }
            }
        }
    }

    protected AbsoluteTimeStamp getAbsoluteTimeStampImpl() {
        Calendar calendar = new GregorianCalendar();
        return toAbsoluteTimeStamp(calendar);
    }

    private Calendar toCalendar(AbsoluteTimeStamp absoluteTimeStamp) {
        Calendar calendarDate = Calendar.getInstance();
        calendarDate.set(Calendar.YEAR, absoluteTimeStamp.getYear());
        calendarDate.set(Calendar.MONTH, absoluteTimeStamp.getMonth() - 1);
        calendarDate.set(Calendar.DAY_OF_MONTH, absoluteTimeStamp.getDay());
        calendarDate.set(Calendar.HOUR_OF_DAY, absoluteTimeStamp.getHour());
        calendarDate.set(Calendar.MINUTE, absoluteTimeStamp.getMinute());
        calendarDate.set(Calendar.SECOND, absoluteTimeStamp.getSecond());
        calendarDate.set(Calendar.MILLISECOND, 0);
        return calendarDate;
    }

    public AbsoluteTimeStamp epochUtcTimeToAbsoluteTimeStampImpl(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return toAbsoluteTimeStamp(calendar);
    }

    private AbsoluteTimeStamp toAbsoluteTimeStamp(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int mon = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int h = calendar.get(Calendar.HOUR);
        int m = calendar.get(Calendar.MINUTE);
        int s = calendar.get(Calendar.SECOND);
        int tz = calendar.get(Calendar.ZONE_OFFSET);
        return new AbsoluteTimeStampImpl(year - 2000, mon, day, h, m, s, tz / 1000 / 60 / 15);
    }


}
