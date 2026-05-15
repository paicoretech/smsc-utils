package com.paicbd.smsc.interpreter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.paicbd.smsc.exception.SerializationException;
import com.paicbd.smsc.utils.Generated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class ObjectsInterpreter {
    private static final XmlMapper XML_MAPPER;
    private static final ObjectMapper OBJECT_MAPPER;
    private static final ObjectMapper CAMEL_CASE_MAPPER;
    private static final String SERIALIZATION_ERROR_MESSAGE;

    static {
        XML_MAPPER = new XmlMapper();
        OBJECT_MAPPER = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        CAMEL_CASE_MAPPER = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        SERIALIZATION_ERROR_MESSAGE = "Error during JSON to serialized string conversion to ";
    }

    @Generated
    private ObjectsInterpreter() {
        throw new IllegalStateException("Utility class");
    }

    public static String jsonNodeToSerializedString(
            JsonNode inputJson,
            Stream<FieldMapping> fieldMappings,
            Class<?> targetClass,
            PayloadFormat responseType,
            String xmlRootName) {
        Assert.notNull(inputJson, "Input JSON must not be null");
        Assert.notNull(fieldMappings, "Field mappings must not be null");
        Assert.notNull(targetClass, "Target class must not be null");
        Assert.notNull(responseType, "Response type must not be null");

        String jsonResult = targetClass.isRecord() ?
                processJavaRecord(inputJson, fieldMappings, targetClass) :
                processJavaObject(inputJson, fieldMappings, targetClass);

        return Objects.equals(PayloadFormat.JSON, responseType) ?
                jsonResult :
                convertJsonToXml(jsonResult, xmlRootName);
    }

    private static String processJavaObject(JsonNode inputJson, Stream<FieldMapping> fieldMappings, Class<?> targetClass) {
        try {
            boolean isJsonNode = JsonNode.class.equals(targetClass);
            if (isJsonNode) {
                var result = transformJsonNode(inputJson, fieldMappings);
                return OBJECT_MAPPER.valueToTree(result).toString();
            }

            Object instance = targetClass.getDeclaredConstructor().newInstance();
            fieldMappings.forEach(mapping -> mapField(inputJson, mapping, targetClass, instance));
            return OBJECT_MAPPER.writeValueAsString(instance);
        } catch (Exception e) {
            log.error("Error processing Java Object: {}", targetClass.getSimpleName(), e);
            throw new SerializationException(SERIALIZATION_ERROR_MESSAGE + targetClass.getSimpleName(), e);
        }
    }

    private static String processJavaRecord(JsonNode inputJson, Stream<FieldMapping> fieldMappings, Class<?> targetClass) {
        try {
            var constructor = targetClass.getDeclaredConstructors()[0];
            var parameters = constructor.getParameters();
            Object[] args = new Object[parameters.length];

            fieldMappings.forEach(mapping -> mapRecordField(inputJson, mapping, parameters, args));

            var recordInstance = constructor.newInstance(args);
            return OBJECT_MAPPER.writeValueAsString(recordInstance);
        } catch (Exception e) {
            log.error("Error converting JSON to Record: {}", targetClass.getSimpleName(), e);
            throw new SerializationException(SERIALIZATION_ERROR_MESSAGE + targetClass.getSimpleName(), e);
        }
    }

    private static void mapField(JsonNode inputJson, FieldMapping mapping, Class<?> targetClass, Object instance) {
        try {
            PropertyDescriptor descriptor = new PropertyDescriptor(mapping.targetProperty(), targetClass);
            Method setter = descriptor.getWriteMethod();
            Assert.notNull(setter, "No setter found for property " + mapping.targetProperty());

            JsonNode jsonValue = resolveNestedProperty(inputJson, mapping.sourceProperty());
            if (jsonValue != null && !jsonValue.isNull()) {
                var parameterType = setter.getParameterTypes()[0];
                Object value = convertValueWithSupport(jsonValue, mapping, parameterType);
                setter.invoke(instance, value);
            }
        } catch (Exception e) {
            log.error("Error mapping property {} with value {}", mapping.sourceProperty(), resolveNestedProperty(inputJson, mapping.sourceProperty()), e);
        }
    }

    private static Object convertValueWithSupport(JsonNode jsonValue, FieldMapping mapping, Class<?> parameterType) {
        try {
            if (DataType.BYTE.equals(mapping.dataType())) {
                String rawValue = jsonValue.asText();
                if (rawValue.startsWith("0x") || rawValue.startsWith("0X")) {
                    var parsed = Byte.parseByte(rawValue.substring(2), 16);
                    return (int) parsed;
                }
                return (int) Byte.parseByte(rawValue);
            }

            return OBJECT_MAPPER.convertValue(jsonValue, parameterType);
        } catch (Exception ex) {
            log.warn("Error converting value {} to type {}", jsonValue, parameterType, ex);
            throw ex;
        }
    }

    private static void mapRecordField(JsonNode inputJson, FieldMapping mapping, Parameter[] parameters, Object[] args) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(mapping.targetProperty())) {
                processParameter(inputJson, mapping, parameters[i], args, i);
                break;
            }
        }
    }

    private static void processParameter(JsonNode inputJson, FieldMapping mapping, Parameter parameter, Object[] args, int index) {
        try {
            JsonNode jsonValue = resolveNestedProperty(inputJson, mapping.sourceProperty());
            if (jsonValue == null || jsonValue.isNull()) {
                log.warn("Property {} not found or null in input JSON", mapping.sourceProperty());
                return;
            }

            args[index] = mapValueToType(jsonValue, parameter);
        } catch (Exception e) {
            log.error("Error mapping property {} to parameter {}", mapping.sourceProperty(), parameter.getName(), e);
        }
    }

    private static Object mapValueToType(JsonNode jsonValue, Parameter parameter) {
        Class<?> type = parameter.getType();

        if (List.class.isAssignableFrom(type)) {
            return mapToList(jsonValue, parameter);
        }

        return OBJECT_MAPPER.convertValue(jsonValue, type);
    }

    private static List<?> mapToList(JsonNode jsonValue, Parameter parameter) {
        if (!jsonValue.isArray()) {
            log.warn("Expected array for property, but found {}", jsonValue.getNodeType());
            return Collections.emptyList();
        }

        ParameterizedType genericType = (ParameterizedType) parameter.getParameterizedType();
        Class<?> listElementType = (Class<?>) genericType.getActualTypeArguments()[0];
        return OBJECT_MAPPER.convertValue(jsonValue,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, listElementType));
    }

    public static String convertJsonToXml(String json, String rootName) {
        try {
            Assert.notNull(rootName, "Root name must not be null for XML conversion");
            JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
            return XML_MAPPER.writer().withRootName(rootName).writeValueAsString(jsonNode);
        } catch (Exception e) {
            log.error("Error converting JSON to XML", e);
            throw new SerializationException("Error while converting JSON to XML", e);
        }
    }

    private static JsonNode resolveNestedProperty(JsonNode rootNode, String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return null;
        }

        JsonNode currentNode = rootNode;
        for (String segment : propertyPath.split("\\.")) {
            currentNode = Optional.of(currentNode)
                    .map(node -> node.get(segment))
                    .orElse(null);
            if (currentNode == null || currentNode.isNull()) {
                return null;
            }
        }

        return currentNode;
    }

    private static JsonNode transformJsonNode(JsonNode inputJson, Stream<FieldMapping> fieldMappings) {
        ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();

        fieldMappings.forEach(fieldMapping -> {
            String[] keys = fieldMapping.targetProperty().split("\\.");
            JsonNode currentNode = rootNode;

            for (int i = 0; i < keys.length - 1; i++) {
                String key = keys[i];
                if (!currentNode.has(key) || !currentNode.get(key).isObject()) {
                    ((ObjectNode) currentNode).set(key, OBJECT_MAPPER.createObjectNode());
                }
                currentNode = currentNode.get(key);
            }

            String leafKey = keys[keys.length - 1];
            JsonNode valueNode = inputJson.at("/" + fieldMapping.sourceProperty().replace('.', '/'));
            Object transformedValue = transformValue(valueNode, fieldMapping.dataType());

            if (transformedValue != null) {
                ((ObjectNode) currentNode).putPOJO(leafKey, transformedValue);
            }
        });

        return rootNode;
    }

    public static JsonNode objectToCamelCaseJsonNode(Object object) {
        return CAMEL_CASE_MAPPER.valueToTree(object);
    }

    public static JsonNode xmlStringToJsonNode(String xml) {
        try {
            return XML_MAPPER.readTree(xml);
        } catch (Exception e) {
            log.error("Error converting XML to JSON", e);
            return null;
        }
    }

    private static Object transformValue(JsonNode valueNode, DataType dataType) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        return switch (dataType) {
            case BYTE -> transformToByte(valueNode);
            case INT -> valueNode.asInt();
            case STRING -> valueNode.asText();
            case BOOLEAN -> valueNode.asBoolean();
            case LONG -> valueNode.asLong();
            case DOUBLE -> valueNode.asDouble();
            case LIST -> valueNode;
            default -> throw new IllegalArgumentException("Unsupported type: " + dataType);
        };
    }

    private static String transformToByte(JsonNode valueNode) {
        byte byteValue;

        if (valueNode.isTextual()) {
            String textValue = valueNode.asText().replace("0x", "");
            byteValue = Byte.parseByte(textValue, 16);
        } else {
            byteValue = (byte) valueNode.asInt();
        }

        return String.format("0x%02X", byteValue);
    }
}
