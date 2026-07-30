package com.paicbd.smsc.interpreter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paicbd.smsc.dto.Gateway;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.exception.RTException;
import com.paicbd.smsc.utils.EncodingUtils;
import com.paicbd.smsc.utils.SmppUtils;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PayloadInterpreter {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<Class<?>, UnaryOperator<Object>> CONVERTERS = new HashMap<>();
    private static final Pattern PLACEHOLDER_PLAIN = Pattern.compile("\\{\\{([^}]+)}}");
    private static final Pattern PLACEHOLDER_JSON  = Pattern.compile("\"\\{\\{([^}]+)}}\"");
    private static final Pattern INNER_HEAD        = Pattern.compile("^(\\w+(?:\\.\\w+){0,5}):([A-Z_-]+)");
    private static final Pattern INNER_TAIL_NAMED  = Pattern.compile("^:(\\w+)(?:\\|([\\w=,]+))?$");
    private static final Pattern INNER_TAIL_LIST   = Pattern.compile("^\\s+([\\w=,]+)$");
    private static final Pattern EMPTY_TAG_WITH_QUOTES = Pattern.compile("<([\\w:-]+)>\\s*\"\"\\s*</\\1>");
    private static final Pattern EMPTY_TAG            = Pattern.compile("<([\\w:-]+)>\\s*</\\1>");

    static {
        CONVERTERS.put(String.class, value -> "null".equals(value) ? null : value.toString());
        CONVERTERS.put(int.class, value -> Integer.parseInt(value.toString()));
        CONVERTERS.put(Integer.class, value -> parseInt(value.toString()));
        CONVERTERS.put(long.class, value -> Long.parseLong(value.toString()));
        CONVERTERS.put(Long.class, value -> Long.parseLong(value.toString()));
        CONVERTERS.put(byte.class, value -> Byte.parseByte(value.toString()));
        CONVERTERS.put(boolean.class, value -> parseBoolean(value.toString()));
        CONVERTERS.put(Boolean.class, value -> parseBoolean(value.toString()));
        CONVERTERS.put(Map.class, value -> value);
    }

    @Generated
    private PayloadInterpreter() {
        throw new IllegalStateException("Utility class");
    }

    // ************************************************************************
    // Related with body for send
    // ************************************************************************
    public static String interpretPayloadForSend(String payload, MessageEvent event, PayloadFormat type)
            throws JsonProcessingException {

        final boolean isXml = PayloadFormat.XML.equals(type);
        final Pattern placeholder = PayloadFormat.JSON.equals(type) ? PLACEHOLDER_JSON : PLACEHOLDER_PLAIN;

        Matcher matcher = placeholder.matcher(payload);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String replacement = computeReplacementForMatch(matcher, event, isXml);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        String out = result.toString();

        if (isXml) {
            out = EMPTY_TAG_WITH_QUOTES.matcher(out).replaceAll("");
            out = EMPTY_TAG.matcher(out).replaceAll("");
        }

        return out;
    }

    private static String computeReplacementForMatch(Matcher matcher, MessageEvent event, boolean isXml)
            throws JsonProcessingException {

        ParsedSpec spec = parseInnerSpec(matcher.group(1), isXml);
        if (spec == null) return matcher.group(0);

        Object value = getProperty(event, spec.propertyName());
        if (value == null) return isXml ? "" : "\"\"";

        value = maybeHexShortMessage(value, spec.propertyName(), spec.propertyType(), event);
        return formatValue(value, spec.propertyType(), isXml, spec.elementName(), spec.renameMap());
    }

    private static ParsedSpec parseInnerSpec(String inner, boolean isXml) {
        Matcher mHead = INNER_HEAD.matcher(inner);
        if (!mHead.find()) return null;

        String propertyName = mHead.group(1);
        String typeToken    = mHead.group(2);

        final DataType propertyType;
        try {
            propertyType = DataType.valueOf(typeToken);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unsupported data type '" + typeToken + "' in placeholder: " + inner, ex);
        }

        String rest = inner.substring(mHead.end());
        String elementName = null;
        String renameSpec  = null;

        if (!rest.isEmpty()) {
            Matcher mNamed = INNER_TAIL_NAMED.matcher(rest);
            if (mNamed.find()) {
                elementName = mNamed.group(1);
                renameSpec  = mNamed.group(2);
            } else {
                Matcher mList = INNER_TAIL_LIST.matcher(rest);
                if (mList.find()) renameSpec = mList.group(1);
            }
        }

        Map<String,String> renameMap =
                (propertyType == DataType.LIST) ? buildRenameMap(renameSpec) : Collections.emptyMap();

        if (isXml && propertyType == DataType.LIST && (elementName == null || elementName.isBlank())) {
            elementName = "item";
        }

        return new ParsedSpec(propertyName, propertyType, elementName, renameMap);
    }

    private static Object maybeHexShortMessage(Object value, String propertyName, DataType type, MessageEvent event) {
        if (!"shortMessage".equals(propertyName)) return value;
        boolean needsHex = (type == DataType.HEX) || (value != null && EncodingUtils.isHexadecimal(value.toString()));
        if (!needsHex) return value;

        String hex = EncodingUtils.bytesToHex(event.getMessageBytes());
        if (event.getUdhLength() > 0) {
            hex = EncodingUtils.bytesToHex(event.getUdhBytes()) + hex;
        }
        return hex;
    }

    private static Map<String, String> buildRenameMap(String renameSpec) {
        if (renameSpec == null || renameSpec.isBlank()) return Collections.emptyMap();

        Map<String, String> tmp = new LinkedHashMap<>();
        for (String pair : renameSpec.split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && eq < pair.length() - 1) {
                String k = pair.substring(0, eq).trim();
                String v = pair.substring(eq + 1).trim();
                if (!k.isEmpty() && !v.isEmpty()) {
                    tmp.put(k, v);
                }
            }
        }
        return tmp;
    }

    private static String formatValue(Object value, DataType type, boolean isXml, String elementName, Map<String,String> renameMap) throws JsonProcessingException {
        log.debug("Formatting value: {} of type: {} as: {}", value, type, isXml ? "XML" : "JSON");
        return switch (type) {
            case STRING, HEX -> isXml ? value.toString() : "\"" + value + "\"";
            case HEX_STRING -> isXml
                    ? String.format("0x%02X", Integer.parseInt(value.toString()))
                    : String.format("\"0x%02X\"", Integer.parseInt(value.toString()));
            case INT, DOUBLE, BYTE, LONG, MAP -> value.toString();
            case BOOLEAN -> formatBoolean(value);
            case LIST -> formatList(value, isXml, elementName, renameMap);
        };
    }

    private static String formatBoolean(Object value) {
        log.debug("Formatting BOOLEAN value: {}", value);
        if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Integer integer) {
            return integer == 1 ? "true" : "false";
        } else {
            log.error("Unsupported type for BOOLEAN: {}", value.getClass().getSimpleName());
            return "";
        }
    }

    private static String formatList(Object value, boolean isXml, String elementName, Map<String,String> renameMap) throws JsonProcessingException {
        if (value instanceof Collection<?> col) {
            if (isXml) {
                return formatAsXmlList(new ArrayList<>(col), elementName, renameMap);
            }

            final Map<String, String> renames = (renameMap != null) ? renameMap : Map.of();

            List<Map<String, Object>> out = new ArrayList<>(col.size());
            for (Object item : col) {
                Map<String, Object> raw = mapper.convertValue(item, new TypeReference<Map<String, Object>>() {});
                Map<String, Object> renamed = LinkedHashMap.newLinkedHashMap(raw.size());

                for (Map.Entry<String, Object> e : raw.entrySet()) {
                    String key = renames.getOrDefault(e.getKey(), e.getKey());
                    renamed.put(key, e.getValue());
                }
                out.add(renamed);
            }
            return mapper.writeValueAsString(out);
        }

        return mapper.writeValueAsString(value);
    }

    private static String formatAsXmlList(List<?> list, String elementName, Map<String,String> renameMap) {
        StringBuilder result = new StringBuilder();
        for (Object item : list) {
            result.append(formatAsXmlItem(item, elementName, renameMap));
        }
        return result.toString();
    }

    private static String formatAsXmlItem(Object item, String elementName, Map<String,String> renameMap) {
        String en = (elementName == null || elementName.isBlank()) ? "optionalParam" : elementName;

        Map<String, Object> raw = mapper.convertValue(item, new TypeReference<Map<String, Object>>() {});
        StringBuilder itemXml = new StringBuilder();
        itemXml.append("<").append(en).append(">");

        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Object v = e.getValue();
            if (v != null) {
                String text = String.valueOf(v);
                if (!text.isBlank()) {
                    String fieldName = renameMap.getOrDefault(e.getKey(), e.getKey());
                    itemXml.append("<").append(fieldName).append(">")
                            .append(xmlEscape(text))
                            .append("</").append(fieldName).append(">");
                }
            }
        }

        itemXml.append("</").append(en).append(">");
        return itemXml.toString();
    }

    private static String xmlEscape(String s) {
        if (s == null) return "";
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static Object getProperty(MessageEvent event, String propertyName) {
        log.debug("Getting property: {} from event: {}", propertyName, event);
        try {
            Field field = event.getClass().getDeclaredField(propertyName.split("\\.")[0]);
            if (field.getType().equals(Map.class)) {
                Method method = event.getClass().getMethod("get" + capitalize(propertyName.split("\\.")[0]));
                Object mapObject = method.invoke(event);

                if (mapObject instanceof Map<?, ?> map) {
                    if (!propertyName.contains(".")) {
                        return mapper.writeValueAsString(map);
                    }

                    String nested = propertyName.substring(propertyName.indexOf(".") + 1);
                    return getNestedValue(map, nested);
                }
                return null;
            }

            if (field.getType().equals(boolean.class)) {
                String methodName = propertyName.startsWith("is") ? propertyName : "is" + capitalize(propertyName);
                return MessageEvent.class.getMethod(methodName).invoke(event);
            }

            return MessageEvent.class.getMethod("get" + capitalize(propertyName)).invoke(event);
        } catch (Exception e) {
            log.error("Failed to get property: {}", propertyName, e);
            return null;
        }
    }

    private static Object getNestedValue(Map<?, ?> map, String propertyName) throws JsonProcessingException {
        String[] keys = propertyName.split("\\.");

        Object current = map;
        for (String key : keys) {
            if (current instanceof Map<?, ?> currentMap) {
                current = currentMap.get(key);
            } else {
                log.error("Invalid property path: {}", propertyName);
                return "";
            }
        }
        if (current instanceof String str) {
            return str;
        }

        return mapper.writeValueAsString(current);
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // ************************************************************************
    // Related with received body
    // ************************************************************************
    public static void interpreterPayloadForReceive(String payload, String fieldsMapper, MessageEvent event, PayloadFormat type) throws JsonProcessingException {
        Pattern patternList = Pattern.compile("\\{\\{(\\w+(?:\\.\\w+){0,5}):([A-Z_-]+)(?::(\\w+))?}}");

        JsonNode payloadAsNode;
        JsonNode fieldsMapperAsNode;

        if (PayloadFormat.XML.equals(type)) {
            payloadAsNode = ObjectsInterpreter.xmlStringToJsonNode(payload);
            fieldsMapperAsNode = ObjectsInterpreter.xmlStringToJsonNode(fieldsMapper);
        } else {
            payloadAsNode = mapper.readTree(payload);
            fieldsMapperAsNode = mapper.readTree(fieldsMapper);
        }

        Assert.notNull(payloadAsNode, "Payload node is null");
        Assert.notNull(fieldsMapperAsNode, "Fields mapper node is null");
        processNode(fieldsMapperAsNode, payloadAsNode, event, patternList, type);
    }

    private static void processNode(
            JsonNode mapperNode, JsonNode payloadNode, MessageEvent event, Pattern pattern, PayloadFormat type) {
        if (!mapperNode.isObject()) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = mapperNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            processField(field, payloadNode, event, pattern, type);
        }
    }

    @Generated
    private static void processField(
            Map.Entry<String, JsonNode> field, JsonNode payloadNode, MessageEvent event, Pattern pattern, PayloadFormat type) {
        String fieldName = field.getKey();
        JsonNode mapperFieldValue = field.getValue();
        JsonNode payloadFieldValue = payloadNode.get(fieldName);

        if (mapperFieldValue.isObject()) {
            processNode(mapperFieldValue, payloadFieldValue, event, pattern, type);
        } else if (mapperFieldValue.isTextual()) {
            processTextualField(mapperFieldValue.asText(), payloadFieldValue, event, pattern, type);
        }
    }

    private static void processTextualField(
            String mapperTextValue, JsonNode payloadFieldValue, MessageEvent event, Pattern pattern, PayloadFormat type) {
        Matcher matcher = pattern.matcher(mapperTextValue);
        if (matcher.matches()) {
            String propertyName = matcher.group(1);
            DataType propertyType = DataType.valueOf(matcher.group(2));

            String elementName = null;
            if (PayloadFormat.XML.equals(type) && "LIST".equals(propertyType.name())) {
                elementName = matcher.group(3);
            }

            handleMatchedField(payloadFieldValue, event, propertyName, propertyType, elementName);
        }
    }

    private static void handleMatchedField(
            JsonNode payloadFieldValue, MessageEvent event, String propertyName, DataType propertyType, String elementName) {
        if (payloadFieldValue == null) {
            return;
        }

        String payloadValue = payloadFieldValue.asText();
        Object convertedValue = null;
        if (payloadValue.isEmpty()) {
            payloadValue = payloadFieldValue.toString();
            try {
                if (DataType.LIST.equals(propertyType)) {
                    convertedValue = processOptionalParamsValue(payloadFieldValue, elementName);
                } else {
                    convertedValue = mapper.readValue(payloadValue, Object.class);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to process optional parameters value: {}", payloadValue, e);
            }
        } else {
            convertedValue = convertValue(payloadValue, propertyType);
        }

        setEventProperty(event, propertyName, convertedValue);
    }

    private static List<UtilsRecords.OptionalParameter> processOptionalParamsValue(JsonNode optParamNode, String elementName) throws JsonProcessingException {
        JsonNode optParamsArray = (elementName != null) ? optParamNode.get(elementName) : optParamNode;
        if (optParamsArray == null || optParamsArray.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, String>> mapList = mapper.readValue(optParamsArray.traverse(),
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            return mapList.stream()
                    .map(UtilsRecords.OptionalParameter::new)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to process optional parameters value: {}", optParamNode, e);
            return Collections.emptyList();
        }
    }

    private static void setEventProperty(MessageEvent event, String propertyName, Object value) {
        try {
            if (propertyName.contains(".")) {
                String[] parts = propertyName.split("\\.", 2);
                String mapFieldName = parts[0];
                String mapKey = parts[1];

                String getterName = "get" + capitalize(mapFieldName);
                Method getter = findNoArgMethod(event.getClass(), getterName);
                Object mapObject = getter.invoke(event);

                if (mapObject == null) {
                    mapObject = new HashMap<String, Object>();
                    String setterName = "set" + capitalize(mapFieldName);
                    Method setter = findSingleArgMethod(event.getClass(), setterName);
                    setter.invoke(event, mapObject);
                }

                if (mapObject instanceof Map<?, ?> propertyMap) {
                    Map<String, Object> typed = (Map<String, Object>) propertyMap;
                    typed.put(mapKey, value);
                    return;
                } else {
                    throw new RTException(mapFieldName + " is not a Map type");
                }
            }

            Field field = event.getClass().getDeclaredField(propertyName);
            Class<?> fieldType = field.getType();

            if (fieldType.equals(boolean.class) && propertyName.startsWith("is")) {
                propertyName = propertyName.substring(2);
            }

            String setterName = "set" + capitalize(propertyName);
            Method setter = findSingleArgMethod(event.getClass(), setterName);

            Class<?> targetType = setter.getParameterTypes()[0];
            Object convertedValue = convertToTargetType(value, targetType);
            setter.invoke(event, convertedValue);
        } catch (Exception e) {
            throw new RTException("Failed to set property: " + propertyName, e);
        }
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Method not found: " + name + " on " + type.getName(), e);
        }
    }

    private static Method findSingleArgMethod(Class<?> type, String name) {
        return Arrays.stream(type.getMethods())
                .filter(m -> m.getName().equals(name) && m.getParameterCount() == 1)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "1-arg method not found: " + name + " on " + type.getName()));
    }

    private static Object convertValue(String value, DataType type) {
        return switch (type) {
            case HEX_STRING -> Integer.parseInt(value.replace("0x", ""), 16);
            case INT -> Integer.parseInt(value);
            case LONG -> Long.parseLong(value);
            case BYTE -> Byte.parseByte(value);
            case BOOLEAN -> Boolean.parseBoolean(value);
            case LIST -> Arrays.asList(value.split(","));
            default -> value;
        };
    }

    public static Object convertToTargetType(Object value, Class<?> targetType) {
        try {
            UnaryOperator<Object> converter = CONVERTERS.get(targetType);
            if (converter != null) {
                return converter.apply(value);
            }
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert value '" + value + "' to type: " + targetType, e);
        }
    }

    private static int parseInt(String value) {
        value = value.trim();
        if (value.startsWith("0x") || value.startsWith("0X")) {
            return Integer.parseInt(value.substring(2), 16);
        }

        return switch (value.toLowerCase()) {
            case "true" -> 1;
            case "false" -> 0;
            default -> Integer.parseInt(value);
        };
    }

    private static boolean parseBoolean(String value) {
        if (value.startsWith("0x")) {
            return Integer.parseInt(value.replace("0x", ""), 16) == 1;
        }
        try {
            return Integer.parseInt(value) == 1;
        } catch (NumberFormatException e) {
            log.error("Failed to parse boolean value: {}", value, e);
        }
        return Boolean.parseBoolean(value);
    }

    public static void convertShortMessageByEncodingType(MessageEvent event, Gateway gateway) {
        byte[] bytes;
        String shortMessage = event.getShortMessage();
        if (Objects.isNull(shortMessage) || shortMessage.isBlank()) {
            return;
        }

        int encodingType = SmppUtils.determineEncodingType(event.getDataCoding(), gateway);
        int esmeClass = Optional.ofNullable(event.getEsmClass()).orElse(0);
        boolean containsUdh = EncodingUtils.udhEsmeClassValues.contains(esmeClass);
        if (EncodingUtils.isHexadecimal(shortMessage)) {
            bytes = EncodingUtils.hexToBytes(shortMessage);

            // udh process
            if (containsUdh) {
                EncodingUtils.parseUdh(bytes, event);
                bytes = EncodingUtils.getCleanedBytes(bytes);
                shortMessage = EncodingUtils.bytesToHex(bytes);
            }
        } else {
            bytes = EncodingUtils.encodeMessage(shortMessage, encodingType);
        }
        event.setMessageBytes(bytes);
        event.setShortMessage(shortMessage);
        if (EncodingUtils.isValidDataCoding(event.getDataCoding())) {
            String sms = EncodingUtils.decodeMessage(bytes, encodingType);
            event.setShortMessage(sms);
        }
    }
}
