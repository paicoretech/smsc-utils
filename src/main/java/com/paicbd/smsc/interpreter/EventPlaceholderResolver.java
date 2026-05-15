package com.paicbd.smsc.interpreter;

import com.paicbd.smsc.dto.CallbackHeaderHttp;
import com.paicbd.smsc.dto.MessageEvent;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class EventPlaceholderResolver {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("([^.\\[\\]]+)|(\\[(\\d+)])");
    private static final Object NOT_APPLICABLE = new Object();

    public static List<CallbackHeaderHttp> getCallbackHeadersFor(
            List<PayloadMapper> interpreter,
            String direction,
            String eventType
    ) {
        if (interpreter == null || interpreter.isEmpty()) {
            return Collections.emptyList();
        }

        return interpreter.stream()
                .filter(Objects::nonNull)
                .filter(pm -> eventType != null && eventType.equalsIgnoreCase(pm.getEventType()))
                .filter(pm -> direction != null && direction.equalsIgnoreCase(pm.getDirection()))
                .map(PayloadMapper::getCallbackHeadersHttp)
                .filter(list -> list != null && !list.isEmpty())
                .findFirst()
                .orElse(Collections.emptyList());
    }

    public static String replaceEventPlaceholder(String input, MessageEvent event) {
        if (input == null || event == null) {
            return input;
        }

        Pattern pattern = Pattern.compile("\\{\\{([^}]+)}}");
        Matcher matcher = pattern.matcher(input);

        StringBuilder out = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            out.append(input, last, matcher.start());
            String rawPath = matcher.group(1);
            String path = (rawPath == null) ? null : rawPath.trim();
            Object value = (path == null || path.isEmpty()) ? null : resolvePath(event, path);

            if (value == null) {
                out.append(matcher.group(0));
            } else {
                out.append(value);
            }
            last = matcher.end();
        }
        out.append(input, last, input.length());
        return out.toString();
    }

    private static Object resolvePath(Object root, String path) {
        Object current = root;
        for (Token t : tokenize(path)) {
            if (current == null) return null;
            current = (t.kind == TokenKind.KEY)
                    ? resolveKey(current, t.text)
                    : resolveIndex(current, t.index);
        }
        return current;
    }

    private static Object resolveKey(Object obj, String name) {
        if (obj == null || name == null) return null;

        Object size = trySize(obj, name);
        if (size != null) return size;

        Object mapVal = tryMapKey(obj, name);
        if (mapVal != NOT_APPLICABLE) return mapVal;

        Object exact = tryExactAccessor(obj, name);
        if (exact != NOT_APPLICABLE) return exact;

        Object bean = tryBeanGetter(obj, name);
        if (bean != NOT_APPLICABLE) return bean;

        Object fld = tryPublicField(obj, name);
        if (fld != NOT_APPLICABLE) return fld;

        return null;
    }

    private static Object resolveIndex(Object obj, int idx) {
        if (obj == null || idx < 0) return null;

        if (obj instanceof List<?> list) {
            return idx < list.size() ? list.get(idx) : null;
        }
        if (obj.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(obj);
            return idx < len ? java.lang.reflect.Array.get(obj, idx) : null;
        }
        return null;
    }

    private static Object trySize(Object obj, String name) {
        if (!"size".equals(name)) return null;
        if (obj instanceof Map<?, ?> map) return map.size();
        if (obj instanceof Collection<?> col) return col.size();
        if (obj.getClass().isArray()) return java.lang.reflect.Array.getLength(obj);
        return 0;
    }

    private static Object tryMapKey(Object obj, String name) {
        if (obj instanceof Map<?, ?> map) {
            return map.get(name);
        }
        return NOT_APPLICABLE;
    }

    private static Object tryExactAccessor(Object obj, String name) {
        Class<?> cls = obj.getClass();
        Method exact = findNoArgMethod(cls, name);
        if (exact == null) return NOT_APPLICABLE;

        try {
            return exact.invoke(obj);
        } catch (Exception e) {
            log.debug("Exact accessor invoke failed: {}.{}()", cls.getName(), name, e);
            return null;
        }
    }

    private static Object tryBeanGetter(Object obj, String name) {
        Class<?> cls = obj.getClass();
        String cap = capitalize(name);
        String[] candidates = new String[] { "get" + cap, "is" + cap };

        for (String methodName : candidates) {
            Method m = findNoArgMethod(cls, methodName);
            if (m != null) {
                try {
                    return m.invoke(obj);
                } catch (Exception e) {
                    log.debug("Bean getter invoke failed: {}.{}()", cls.getName(), methodName, e);
                    return null;
                }
            }
        }
        return NOT_APPLICABLE;
    }

    private static Object tryPublicField(Object obj, String name) {
        Class<?> cls = obj.getClass();
        Field f = findPublicField(cls, name);
        if (f == null) return NOT_APPLICABLE;
        return safeGetField(f, obj);
    }

    static Object safeGetField(Field f, Object obj) {
        try {
            return f.get(obj);
        } catch (Exception e) {
            log.debug("Public field access failed: {}.{}", f.getDeclaringClass().getName(), f.getName(), e);
            return null;
        }
    }

    private static Method findNoArgMethod(Class<?> cls, String name) {
        for (Method m : cls.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 0) {
                return m;
            }
        }
        return null;
    }

    private static Field findPublicField(Class<?> cls, String name) {
        for (Field f : cls.getFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    private static List<Token> tokenize(String path) {
        List<Token> tokens = new ArrayList<>();
        if (path == null || path.isBlank()) return tokens;

        Matcher m = TOKEN_PATTERN.matcher(path);
        while (m.find()) {
            String key = m.group(1);
            if (key != null) {
                tokens.add(new Token(key));
                continue;
            }
            String indexNum = m.group(3);
            if (indexNum != null) {
                try {
                    tokens.add(new Token(Integer.parseInt(indexNum)));
                } catch (NumberFormatException ignore) {
                    // skip invalid index token; resolution will fail and placeholder remains intact
                }
            }
        }
        return tokens;
    }

    private enum TokenKind { KEY, INDEX }

    private static final class Token {
        final TokenKind kind;
        final String text;
        final int index;
        Token(String text) { this.kind = TokenKind.KEY; this.text = text; this.index = -1; }
        Token(int index)   { this.kind = TokenKind.INDEX; this.text = null; this.index = index; }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
