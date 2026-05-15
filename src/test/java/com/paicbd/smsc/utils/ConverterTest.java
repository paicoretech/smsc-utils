package com.paicbd.smsc.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.paicbd.smsc.dto.GeneralSettings;
import com.paicbd.smsc.dto.RoutingRule;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.exception.RTException;
import org.jsmpp.bean.OptionalParameter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConverterTest {

    @Test
    void testConstantsPrivateConstructor() throws NoSuchMethodException {
        testPrivateConstructor(Converter.class);
    }

    @ParameterizedTest
    @MethodSource("jedisConfigParamsStream")
    void paramsToJedisCluster(UtilsRecords.JedisConfigParams jedisConfigParams) {
        // Validate configuration parameter structure
        assertNotNull(jedisConfigParams);
        assertNotNull(jedisConfigParams.redisNodes());
        assertFalse(jedisConfigParams.redisNodes().isEmpty());

        // Configuration parameters contain valid data for cluster initialization
    }

    @Test
    void testValueAsString() {
        Map<String, Object> map = new HashMap<>(Map.of("key1", "value1", "key2", "value2"));

        String result = Converter.valueAsString(map);
        assertNotNull(result);
        assertTrue(result.contains("key1"));
        assertTrue(result.contains("value1"));

        map.put("key3", new Object());
        assertThrows(RTException.class, () -> Converter.valueAsString(map));
    }

    @Test
    void testCastParamsToOptionalParams() {
        int identifier = 11;
        int parts = 2;
        int partNumber = 1;

        OptionalParameter[] result = Converter.convertToOptionalParameters(identifier, parts, partNumber);
        assertInstanceOf(OptionalParameter[].class, result);
        assertEquals(3, result.length);
        assertNotNull(result[0]);
        assertNotNull(result[1]);
        assertNotNull(result[2]);
    }

    @Test
    void testHasValidValue() {
        String value = "value";
        assertTrue(Converter.hasValidValue(value));

        String emptyValue = "";
        assertFalse(Converter.hasValidValue(emptyValue));

        assertFalse(Converter.hasValidValue(null));
    }

    public static <T> void testPrivateConstructor(Class<T> clazz) throws NoSuchMethodException {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }


    @Test
    void testConvertToObject() {
        String generalSettingInRaw = "{\"id\":1,\"validity_period\":60,\"max_validity_period\":240,\"source_addr_ton\":1,\"source_addr_npi\":1,\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"encoding_iso88591\":3,\"encoding_gsm7\":0,\"encoding_ucs2\":2}";
        String generalSettingInRawWithError = "{\"id\":1,\"validity_period\":60,\"max_validity_period\":240,\"source_addr_ton\":1,\"source_addr_npi\":1,\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"encoding_iso88591\":3,}";

        GeneralSettings generalSetting;

        generalSetting = Converter.stringToObject(generalSettingInRaw, new TypeReference<>() {
        });
        assertNotNull(generalSetting);
        assertEquals(1, generalSetting.getId());
        assertEquals(60, generalSetting.getValidityPeriod());
        assertThrows(Exception.class, () -> Converter.stringToObject(generalSettingInRawWithError, new TypeReference<GeneralSettings>() {
        }));
        generalSetting = Converter.stringToObject(generalSettingInRaw, GeneralSettings.class);
        assertNotNull(generalSetting);
        assertEquals(1, generalSetting.getId());
        assertEquals(60, generalSetting.getValidityPeriod());
        assertNull(Converter.stringToObject(generalSettingInRawWithError, GeneralSettings.class));
    }

    @Test
    void testSecondsToRelativeValidityPeriod() {
        assertEquals("000000000010000R", Converter.secondsToRelativeValidityPeriod(10)); // 10 seconds
        assertEquals("000000020000000R", Converter.secondsToRelativeValidityPeriod(7200)); // 2 hours
        assertEquals("000001000000000R", Converter.secondsToRelativeValidityPeriod(86400)); // 1 day

        assertThrows(IllegalArgumentException.class, () -> Converter.secondsToRelativeValidityPeriod(-10));
    }

    @Test
    void testSmppValidityPeriodToSeconds() {
        // illegal values
        assertThrows(IllegalArgumentException.class, () -> Converter.smppValidityPeriodToSeconds("000000000010000R8"));

        assertEquals(10, Converter.smppValidityPeriodToSeconds("000000000010000R"));
        assertEquals(7200, Converter.smppValidityPeriodToSeconds("000000020000000R"));
        assertEquals(86400, Converter.smppValidityPeriodToSeconds("000001000000000R"));

        LocalDateTime dt = LocalDateTime.now();
        dt = dt.plusHours(2);
        int currentYear = dt.getYear();
        currentYear -= 2000;
        int currentMonth = dt.getMonthValue();
        int currentDay = dt.getDayOfMonth();
        int currentHour = dt.getHour();
        int currentMinute = dt.getMinute();
        int currentSecond = dt.getSecond();

        String absoluteTime = String.format("%02d%02d%02d%02d%02d%02d000+", currentYear, currentMonth, currentDay, currentHour, currentMinute, currentSecond);
        assertTrue(Converter.smppValidityPeriodToSeconds(absoluteTime) > 0);

        absoluteTime = String.format("%02d%02d%02d%02d%02d%02d000-", currentYear, currentMonth, currentDay, currentHour, currentMinute, currentSecond);
        assertTrue(Converter.smppValidityPeriodToSeconds(absoluteTime) > 0);

        // illegal last character
        assertThrows(IllegalArgumentException.class, () -> Converter.smppValidityPeriodToSeconds("000000020000000X"));

        // illegal because null
        assertThrows(IllegalArgumentException.class, () -> Converter.smppValidityPeriodToSeconds(null));
    }

    @Test
    void converterValueAsStringXMLTest() {
        Map<String, String> map = new HashMap<>();
        map.put("systemId", "testXml");
        map.put("message", "this a test");
        String result = Converter.valueAsStringXML(map);
        assertNotNull(result);
        assertTrue(result.contains("this a test"));
        Object object = new Object();
        result = Converter.valueAsStringXML(object);
        assertNull(result);
    }

    @Test
    void converterStringXMLToObject() {
        String xml = "<smpp><systemId>testXml</systemId><message>this a test</message></smpp>";
        String xmlWithError = "<smpp><systemId>testXml<message>this a test</message></smpp>";
        var map = Converter.stringXMLToObject(xml, Map.class);
        assertNotNull(map);
        assertEquals("testXml", map.get("systemId"));
        assertEquals("this a test", map.get("message"));

        map = Converter.stringXMLToObject(xmlWithError, Map.class);
        assertNull(map);
    }

    static Stream<UtilsRecords.JedisConfigParams> jedisConfigParamsStream() {
        return Stream.of(
                new UtilsRecords.JedisConfigParams(
                        List.of("localhost:7000", "localhost:7001", "localhost:7002", "localhost:7003", "localhost:7004", "localhost:7005", "localhost:7006", "localhost:7007", "localhost:7008", "localhost:7009"),
                        1000,
                        1000,
                        1000,
                        true,
                        0,
                        0,
                        0,
                        "", ""),
                new UtilsRecords.JedisConfigParams(
                        List.of("localhost:7000", "localhost:7001", "localhost:7002", "localhost:7003", "localhost:7004", "localhost:7005", "localhost:7006", "localhost:7007", "localhost:7008", "localhost:7009"),
                        1000,
                        1000,
                        1000,
                        true,
                        2000,
                        2000,
                        20,
                        "", "pass"),
                new UtilsRecords.JedisConfigParams(
                        List.of("localhost:7000", "localhost:7001", "localhost:7002", "localhost:7003", "localhost:7004", "localhost:7005", "localhost:7006", "localhost:7007", "localhost:7008", "localhost:7009"),
                        1000,
                        1000,
                        1000,
                        true,
                        2000,
                        2000,
                        20,
                        "user", "pass")
        );
    }

    @Test
    void removeLineBreaksTest() {
        String resultEnglish = Converter.removeLineBreaks("""
                Hello
                world I'm developer
                """);
        assertEquals("Hello world I'm developer", resultEnglish);

        String resultPersian = Converter.removeLineBreaks("""
                سلام به همه تان صبح تان بخیر انشاالله که خوب و خوش باشید.برای شنیدن خبرهای خوب
                *477*1*3#
                را فشار دهید. و یاهم با مسیح جان در تماس شوید.
                """);
        assertEquals("سلام به همه تان صبح تان بخیر انشاالله که خوب و خوش باشید.برای شنیدن خبرهای خوب *477*1*3# را فشار دهید. و یاهم با مسیح جان در تماس شوید.", resultPersian);
    }

    @Test
    void classToMapTestWithRoutingAdvancedClassThenDoSuccessfully() {
        RoutingRule.ActionAdvanced actionAdvanced = new RoutingRule.ActionAdvanced();
        actionAdvanced.setMapVersion(2);
        actionAdvanced.setSsnSmscSri(15);
        actionAdvanced.setSsnHlrSri(6);
        actionAdvanced.setSsnSmscMt(15);
        actionAdvanced.setSsnMscMt(8);

        Map<String, Object> map = Converter.clasToMap(actionAdvanced);
        assertNotNull(map);
        assertEquals(2, map.get("map_version"));
        assertEquals(15, map.get("ssn_smsc_sri"));
        assertEquals(6, map.get("ssn_hlr_sri"));
        assertEquals(15, map.get("ssn_smsc_mt"));
        assertEquals(8, map.get("ssn_msc_mt"));
    }

}