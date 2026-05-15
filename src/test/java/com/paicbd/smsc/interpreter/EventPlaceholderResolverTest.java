package com.paicbd.smsc.interpreter;

import com.paicbd.smsc.dto.CallbackHeaderHttp;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.MessagePart;
import com.paicbd.smsc.dto.UtilsRecords;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventPlaceholderResolverTest {

    @DisplayName("replaceEventPlaceholder: multiple scenarios")
    @ParameterizedTest(name = "{index} ⇒ \"{0}\" with props {1} → \"{2}\"")
    @MethodSource("cases")
    void replaceEventPlaceholderCases(String input, Map<String, Object> props, String expected) throws Exception {
        MessageEvent event = (props == null) ? null : new MessageEvent();
        if (event != null) {
            applyProps(event, props);
        }

        String actual = EventPlaceholderResolver.replaceEventPlaceholder(input, event);
        assertEquals(expected, actual);
    }

    /**
     * Covered scenarios:
     * 1) Placeholder exists and has a value → replaced.
     * 2) No placeholder → unchanged.
     * 3) Placeholder references a non-existing property → unchanged.
     * 4) Placeholder references a property that exists but is empty → replaced with empty string.
     * 5) Input = null → returns null.
     * 6) Placeholder with spaces inside {{ ... }} → trimmed and replaced.
     * 7) Event = null → input returned unchanged.
     */
    private static Stream<Arguments> cases() {
        return Stream.of(
                // 1) Exists: sequenceNumber = 42 → "M42"
                Arguments.of(
                        "M{{sequenceNumber}}",
                        Map.of("sequenceNumber", 42),
                        "M42"
                ),
                // 2) No placeholder → unchanged
                Arguments.of(
                        "Hello World",
                        Map.of("sequenceNumber", 99),
                        "Hello World"
                ),
                // 3) Unknown property → unchanged
                Arguments.of(
                        "X{{doesNotExist}}Y",
                        Map.of("sequenceNumber", 1),
                        "X{{doesNotExist}}Y"
                ),
                // 4a) Placeholder with non-existing property 'status' → unchanged
                Arguments.of(
                        "A{{status}}Z",
                        Map.of(),
                        "A{{status}}Z"
                ),
                // 4b) Placeholder with empty property 'status' → replaced with empty string
                Arguments.of(
                        "A{{status}}Z",
                        Map.of("status", ""),
                        "AZ"
                ),
                // 5) Input = null → null
                Arguments.of(
                        null,
                        Map.of("sequenceNumber", 7),
                        null
                ),
                // 6) Trim inside braces → works
                Arguments.of(
                        "N{{  sequenceNumber  }}",
                        Map.of("sequenceNumber", 123),
                        "N123"
                ),
                // 7) Event = null → unchanged
                Arguments.of(
                        "M{{sequenceNumber}}",
                        null,
                        "M{{sequenceNumber}}"
                )
        );
    }

    @DisplayName("replaceEventPlaceholder: nested & indexed paths (maps, lists, size)")
    @ParameterizedTest(name = "{index} ⇒ \"{0}\" → \"{2}\"")
    @MethodSource({"nestedCases", "arrayCases"})
    void replaceEventPlaceholderNestedCases(String input, Map<String, Object> props, String expected) throws Exception {
        MessageEvent event = new MessageEvent();
        applyProps(event, props);

        String actual = EventPlaceholderResolver.replaceEventPlaceholder(input, event);
        assertEquals(expected, actual);
    }

    /**
     * Nested scenarios covered:
     * - Map keys: customParams.customClientId
     * - List indexing: messageParts[1].shortMessage
     * - Record accessors in list items: optionalParameters[0].tag / [1].value
     * - Pseudo property 'size' on lists/maps: messageParts.size, customParams.size
     * - Out-of-bounds index → placeholder remains unchanged
     */
    static Stream<Arguments> nestedCases() {
        MessagePart p0 = buildMessagePart(1, "hello-0", "ref-0", 2);
        MessagePart p1 = buildMessagePart(2, "hello-1", "ref-1", 2);

        List<MessagePart> parts = List.of(p0, p1);

        // Build optionalParameters list (record UtilsRecords.OptionalParameter(short tag, String value))
        List<UtilsRecords.OptionalParameter> optParams = List.of(
                new UtilsRecords.OptionalParameter((short) 5, "v5"),
                new UtilsRecords.OptionalParameter((short) 9, "v9")
        );

        // Build customParams map
        Map<String, Object> customParams = Map.of(
                "customClientId", "INNO",
                "flags", Map.of("dlr", true, "refund", false)
        );

        Map<String, Object> cpWithExploder = Map.of("exploder", new Exploder());

        return Stream.of(
                // customParams level-1
                Arguments.of(
                        "C{{customParams.customClientId}}",
                        Map.of("customParams", customParams),
                        "CINNO"
                ),
                // customParams nested map
                Arguments.of(
                        "F{{customParams.flags.dlr}}-{{customParams.flags.refund}}",
                        Map.of("customParams", customParams),
                        "Ftrue-false"
                ),
                // messageParts index 1
                Arguments.of(
                        "MP{{messageParts[1].shortMessage}}",
                        Map.of("messageParts", parts),
                        "MPhello-1"
                ),
                // messageParts.size
                Arguments.of(
                        "SZ{{messageParts.size}}",
                        Map.of("messageParts", parts),
                        "SZ2"
                ),
                // customParams.size (number of top-level keys)
                Arguments.of(
                        "KS{{customParams.size}}",
                        Map.of("customParams", customParams),
                        "KS2"
                ),
                // optionalParameters: record-style accessors (tag(), value())
                Arguments.of(
                        "OP{{optionalParameters[0].tag}}-{{optionalParameters[1].value}}",
                        Map.of("optionalParameters", optParams),
                        "OP5-v9"
                ),
                // out-of-bounds index → placeholder remains intact
                Arguments.of(
                        "OOB{{messageParts[99].shortMessage}}",
                        Map.of("messageParts", parts),
                        "OOB{{messageParts[99].shortMessage}}"
                ),
                // Exact accessor: explode() throws → caught → returns null → placeholder remains intact
                Arguments.of(
                        "E{{customParams.exploder.explode}}",
                        Map.of("customParams", cpWithExploder),
                        "E{{customParams.exploder.explode}}"
                ),
                // tryBeanGetter: finds getTrouble(), invocation throws → caught → returns null → placeholder remains intact
                Arguments.of(
                        "E{{customParams.bean.trouble}}",
                        Map.of("customParams", Map.of("bean", new BeanExploderGet())),
                        "E{{customParams.bean.trouble}}"
                ),
                // tryBeanGetter: finds isActive(), invocation throws → caught → returns null → placeholder remains intact
                Arguments.of(
                        "I{{customParams.bool.active}}",
                        Map.of("customParams", Map.of("bool", new BeanExploderIs())),
                        "I{{customParams.bool.active}}"
                ),
                // field access: resolve public field "value" via findPublicField → returns "ok"
                Arguments.of(
                        "V{{customParams.box.value}}",
                        Map.of("customParams", Map.of("box", new PublicFieldBox())),
                        "Vok"
                ),
                // field access: no public field named "value" → loop iterates, no match → null → placeholder stays intact
                Arguments.of(
                        "N{{customParams.mis.value}}",
                        Map.of("customParams", Map.of("mis", new OtherPublicFieldBox())),
                        "N{{customParams.mis.value}}"
                )
        );
    }

    private static MessagePart buildMessagePart(int seq, String shortMsg, String ref, int total) {
        String messageId = System.currentTimeMillis() + "-" + System.nanoTime();
        return MessagePart.builder()
                .messageId(messageId)
                .shortMessage(shortMsg)
                .msgReferenceNumber(ref)
                .totalSegment(total)
                .segmentSequence(seq)
                .build();
    }

    static Stream<Arguments> arrayCases() {
        return Stream.of(
                // 1) Valid index into byte[] (array branch hit, returns boxed Byte -> "20")
                Arguments.of(
                        "B{{messageBytes[1]}}",
                        Map.of("messageBytes", new byte[]{10, 20, 30}),
                        "B20"
                ),
                // 2) Out-of-bounds index on byte[] (array branch hit, returns null -> placeholder intact)
                Arguments.of(
                        "A{{messageBytes[99]}}",
                        Map.of("messageBytes", new byte[]{1, 2, 3}),
                        "A{{messageBytes[99]}}"
                ),
                // 3) Indexing on non-indexable (String) -> not list/array -> final `return null` -> placeholder intact
                Arguments.of(
                        "S{{systemId[0]}}",
                        Map.of("systemId", "ABC"),
                        "S{{systemId[0]}}"
                ),
                // 4) .size sobre array -> cubre obj.getClass().isArray()
                Arguments.of(
                        "Z{{messageBytes.size}}",
                        Map.of("messageBytes", new byte[]{1, 2, 3, 4}),
                        "Z4"
                ),
                // 5) .size sobre objeto no Map/Collection/array -> cubre return 0
                Arguments.of(
                        "Q{{systemId.size}}",
                        Map.of("systemId", "ABC"),
                        "Q0"
                )
        );
    }

    /**
     * Assigns properties to the MessageEvent using setter convention:
     * for key "sequenceNumber" it calls "setSequenceNumber(<type>)".
     */
    private void applyProps(MessageEvent event, Map<String, Object> props) throws Exception {
        if (props == null) return;
        for (Map.Entry<String, Object> e : props.entrySet()) {
            String setter = "set" + capitalize(e.getKey());
            Method m = findSetter(event.getClass(), setter);
            m.invoke(event, e.getValue());
        }
    }

    private static Method findSetter(Class<?> type, String name) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1) {
                return m;
            }
        }
        throw new IllegalArgumentException("Setter not found: " + name + " on " + type.getName());
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static class PublicFieldBox {
        public String value = "ok";
    }

    static class OtherPublicFieldBox {
        public String other = "nope";
    }

    static class Exploder {
        public String explode() {
            throw new RuntimeException("boom");
        }
    }

    static class BeanExploderGet {
        public String getTrouble() {
            throw new RuntimeException("boom-get");
        }
    }

    static class BeanExploderIs {
        public boolean isActive() {
            throw new RuntimeException("boom-is");
        }
    }

    @DisplayName("safeGetField: returns field value on happy path")
    @Test
    void safeGetFieldReturnsValue() throws Exception {
        PublicFieldBox box = new PublicFieldBox();
        Field f = PublicFieldBox.class.getField("value");
        Object out = EventPlaceholderResolver.safeGetField(f, box);
        assertEquals("ok", out);
    }

    @DisplayName("safeGetField: covers catch when Field.get throws")
    @Test
    void safeGetFieldCatchIsCovered() throws Exception {
        Field field = mock(Field.class);
        when(field.getDeclaringClass()).thenReturn((Class) PublicFieldBox.class);
        when(field.getName()).thenReturn("value");
        when(field.get(any())).thenThrow(new IllegalAccessException("denied"));

        Object out = EventPlaceholderResolver.safeGetField(field, new PublicFieldBox());
        assertNull(out);
    }

    @DisplayName("getCallbackHeadersFor: parameterized scenarios")
    @ParameterizedTest(name = "{index} ▶ {0}")
    @MethodSource("casesHeaders")
    void getCallbackHeadersFor_param(
            String scenario,
            List<PayloadMapper> interpreter,
            String direction,
            String eventType,
            List<CallbackHeaderHttp> expected
    ) {
        List<CallbackHeaderHttp> out = EventPlaceholderResolver.getCallbackHeadersFor(
                interpreter, direction, eventType
        );
        assertEquals(expected, out);
    }

    static Stream<Arguments> casesHeaders() {
        // Reusable mocks for clarity
        CallbackHeaderHttp callbackHeaderHttp1 = mock(CallbackHeaderHttp.class);
        CallbackHeaderHttp callbackHeaderHttp2 = mock(CallbackHeaderHttp.class);
        CallbackHeaderHttp callbackHeaderHttp3 = mock(CallbackHeaderHttp.class);

        PayloadMapper matchEmpty =
                payloadMapper("message", "output", emptyList());
        PayloadMapper matchNonEmpty =
                payloadMapper("message", "output", List.of(callbackHeaderHttp1, callbackHeaderHttp2));
        PayloadMapper thirdShouldNotBeUsed =
                payloadMapper("message", "output", List.of(callbackHeaderHttp3));
        PayloadMapper noMatch =
                payloadMapper("response", "input", List.of(callbackHeaderHttp1));
        PayloadMapper headersNull =
                payloadMapper("message", "output", null);
        PayloadMapper caseInsensitive =
                payloadMapper("MeSsAgE", "oUtPuT", List.of(callbackHeaderHttp1));

        return Stream.of(
                Arguments.of(
                        "Returns empty list when interpreter is null",
                        null,
                        "OUTPUT",
                        "message",
                        emptyList()
                ),
                Arguments.of(
                        "Returns empty list when interpreter is empty",
                        emptyList(),
                        "OUTPUT",
                        "message",
                        emptyList()
                ),
                Arguments.of(
                        "No match by eventType/direction → empty",
                        List.of(noMatch),
                        "OUTPUT",
                        "message",
                        emptyList()
                ),
                Arguments.of(
                        "Case-insensitive match of eventType and direction",
                        List.of(caseInsensitive),
                        "OUTPUT",
                        "MESSAGE",
                        List.of(callbackHeaderHttp1)
                ),
                Arguments.of(
                        "Picks the first mapper with non-empty headers (skips first empty)",
                        List.of(matchEmpty, matchNonEmpty, thirdShouldNotBeUsed),
                        "output",
                        "message",
                        List.of(callbackHeaderHttp1, callbackHeaderHttp2)
                ),
                Arguments.of(
                        "Ignores null list items and null headers",
                        // Arrays.asList is used here since List.of does not allow null
                        java.util.Arrays.asList(null, headersNull, matchNonEmpty),
                        "OUTPUT",
                        "MESSAGE",
                        List.of(callbackHeaderHttp1, callbackHeaderHttp2)
                ),
                Arguments.of(
                        "eventType is null → no match",
                        List.of(matchNonEmpty),
                        "output",
                        null,
                        emptyList()
                ),
                Arguments.of(
                        "direction is null → no match",
                        List.of(matchNonEmpty),
                        null,
                        "message",
                        emptyList()
                )
        );
    }

    private static PayloadMapper payloadMapper(String eventType, String direction, List<CallbackHeaderHttp> headers) {
        PayloadMapper mock = mock(PayloadMapper.class);
        when(mock.getEventType()).thenReturn(eventType);
        when(mock.getDirection()).thenReturn(direction);
        when(mock.getCallbackHeadersHttp()).thenReturn(headers);
        return mock;
    }
}
