package com.paicbd.smsc.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.exception.RTException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jsmpp.bean.OptionalParameter;
import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class Converter {
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule());
    private static final ObjectMapper mapperXML = new XmlMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerModule(new JavaTimeModule());

    private Converter() {
        throw new IllegalStateException("Utility class");
    }

    public static JedisCluster paramsToJedisCluster(UtilsRecords.JedisConfigParams params) {
        try {
            JedisCluster jedisCluster;
            Set<HostAndPort> jedisClusterNodes = new HashSet<>();
            params.redisNodes().forEach(node -> {
                String[] nodePart = node.split(":");
                jedisClusterNodes.add(new HostAndPort(nodePart[0], Integer.parseInt(nodePart[1])));
            });

            GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(params.maxTotal());
            poolConfig.setMinIdle(params.minIdle());
            poolConfig.setMaxIdle(params.maxIdle());
            poolConfig.setBlockWhenExhausted(params.blockWhenExhausted());

            boolean hasUserAndPassword = !params.user().isEmpty() && !params.password().isEmpty();
            boolean hasOnlyPassword = params.user().isEmpty() && !params.password().isEmpty();
            int connectionTimeout = params.connectionTimeout() > 0 ? params.connectionTimeout() : 2000;
            int soTimeout = params.soTimeout() > 0 ? params.soTimeout() : 2000;
            int maxAttempts = params.maxAttempts() > 0 ? params.maxAttempts() : 20;

            if (hasUserAndPassword) {
                jedisCluster = new JedisCluster(jedisClusterNodes, connectionTimeout, soTimeout, maxAttempts, params.user(), params.password(), null, poolConfig);
            } else if (hasOnlyPassword) {
                jedisCluster = new JedisCluster(jedisClusterNodes, connectionTimeout, soTimeout, maxAttempts, params.password(), poolConfig);
            } else {
                jedisCluster = new JedisCluster(jedisClusterNodes, connectionTimeout, soTimeout, maxAttempts, poolConfig);
            }

            log.info("JedisCluster instance was created successfully with params: {}", params);
            return jedisCluster;
        } catch (Exception e) {
            log.error("An error occurred while creating JedisCluster instance: {}", e.getMessage());
            return null;
        }
    }

    public static String valueAsString(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("An error occurred while converting object to string: {}", e.getMessage());
            throw new RTException("An error occurred while converting object to string", e);
        }
    }

    public static <T> T deepCopy(T original, Class<T> clazz) {
        return mapper.convertValue(original, clazz);
    }

    public static Map<String, Object> clasToMap(Object original) {
        return mapper.convertValue(original, new TypeReference<>() {});
    }

    public static <T> T stringToObject(String json, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(json, typeReference);
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("An error occurred while deserializing Object: {}", e.getMessage(), e);
            throw new RTException("An error occurred while deserializing Object", e);
        }
    }

    public static <T> T stringToObject(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("An error occurred while deserializing Object returning NULL", e);
            return null;
        }
    }

    public static OptionalParameter[] convertToOptionalParameters(int identifier, int parts, int partNumber) {
        List<OptionalParameter> optionalParameters = new ArrayList<>();
        optionalParameters.add(new OptionalParameter.Sar_msg_ref_num((byte) identifier));
        optionalParameters.add(new OptionalParameter.Sar_total_segments((byte) parts));
        optionalParameters.add(new OptionalParameter.Sar_segment_seqnum((byte) partNumber));
        return optionalParameters.toArray(new OptionalParameter[0]);
    }

    public static boolean hasValidValue(String value) {
        return value != null && !value.isEmpty();
    }

    public static String secondsToRelativeValidityPeriod(long totalSeconds) {
        if (totalSeconds < 0) {
            throw new IllegalArgumentException("Total seconds cannot be negative.");
        }

        long years = totalSeconds / (365L * 24 * 60 * 60);
        totalSeconds %= 365L * 24 * 60 * 60;

        long months = totalSeconds / (30L * 24 * 60 * 60);
        totalSeconds %= 30L * 24 * 60 * 60;

        long days = totalSeconds / (24 * 60 * 60);
        totalSeconds %= 24 * 60 * 60;

        long hours = totalSeconds / (60 * 60);
        totalSeconds %= 60 * 60;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        String formattedValues = String.format("%02d%02d%02d%02d%02d%02d000R",
                years, months, days, hours, minutes, seconds);
        log.debug("Formatted values: {}", formattedValues);
        return formattedValues;
    }

    public static long smppValidityPeriodToSeconds(String smppTime) {
        if (smppTime == null || smppTime.length() != 16) {
            throw new IllegalArgumentException("Invalid SMPP time format.");
        }

        char lastChar = smppTime.charAt(15);

        if (lastChar == 'R') {
            return parseRelativeTimeToSeconds(smppTime);
        }

        if (lastChar == '+' || lastChar == '-') {
            return parseAbsoluteTimeToSeconds(smppTime);
        }

        throw new IllegalArgumentException("Unsupported SMPP time format: " + smppTime);
    }

    private static long parseRelativeTimeToSeconds(String smppTime) {
        DateTimeRecord dateTimeRecord = createDateTimeMap(smppTime, false);
        long totalSeconds = (dateTimeRecord.year() * 365L * 24 * 60 * 60) +
                (dateTimeRecord.month() * 30L * 24 * 60 * 60) +
                (dateTimeRecord.day() * 24L * 60 * 60) +
                (dateTimeRecord.hour() * 60L * 60) +
                (dateTimeRecord.minute() * 60L) +
                dateTimeRecord.second();
        log.debug("SMPP validity period: {} converted to seconds: {}", smppTime, totalSeconds);
        return totalSeconds;
    }

    public static long parseAbsoluteTimeToSeconds(String absoluteTime) {
        DateTimeRecord dateTimeRecord = createDateTimeMap(absoluteTime, true);
        int quarterHourOffset = Integer.parseInt(absoluteTime.substring(13, 15));
        char timezoneSign = absoluteTime.charAt(15);  // '+' o '-'
        LocalDateTime parsedDateTime = getLocalDateTime(dateTimeRecord, quarterHourOffset, timezoneSign);

        LocalDateTime currentTime = LocalDateTime.now();
        Duration durationDifference = Duration.between(currentTime, parsedDateTime);
        return durationDifference.getSeconds();
    }

    private static LocalDateTime getLocalDateTime(DateTimeRecord dateTimeRecord, int quarterHourOffset, char timezoneSign) {
        LocalDateTime parsedDateTime = LocalDateTime.of(
                dateTimeRecord.year(),
                dateTimeRecord.month(),
                dateTimeRecord.day(),
                dateTimeRecord.hour(),
                dateTimeRecord.minute(),
                dateTimeRecord.second()
        );

        int offsetInMinutes = quarterHourOffset * 15;
        if (timezoneSign == '+') {
            parsedDateTime = parsedDateTime.plusMinutes(offsetInMinutes);
        } else {
            parsedDateTime = parsedDateTime.minusMinutes(offsetInMinutes);
        }
        return parsedDateTime;
    }

    public static DateTimeRecord createDateTimeMap(String dateTime, boolean addMile) {
        int year = Integer.parseInt(dateTime.substring(0, 2));
        year += addMile ? 2000 : 0;
        int month = Integer.parseInt(dateTime.substring(2, 4));
        int day = Integer.parseInt(dateTime.substring(4, 6));
        int hour = Integer.parseInt(dateTime.substring(6, 8));
        int minute = Integer.parseInt(dateTime.substring(8, 10));
        int second = Integer.parseInt(dateTime.substring(10, 12));
        return new DateTimeRecord(year, month, day, hour, minute, second);
    }

    public record DateTimeRecord(
            int year,
            int month,
            int day,
            int hour,
            int minute,
            int second
    ) {
    }

    public static String valueAsStringXML(Object value) {
        try {
            return mapperXML.writeValueAsString(value);
        } catch (Exception e) {
            log.error("An error occurred while deserializing Object returning NULL", e);
            return null;
        }
    }

    public static <T> T stringXMLToObject(String xml, Class<T> clazz) {
        try {
            return mapperXML.readValue(xml, clazz);
        } catch (Exception e) {
            log.error("An error occurred while deserializing String XML returning NULL", e);
            return null;
        }
    }

    public static String removeLineBreaks(String string) {
        String oneLineString = string.replaceAll("\\r?\\n", " ");
        return oneLineString.trim();
    }
}
