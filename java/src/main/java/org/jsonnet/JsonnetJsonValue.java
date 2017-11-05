package org.jsonnet;

import java.util.List;
import java.util.Map;

public class JsonnetJsonValue {
    public enum Kind {
        ARRAY,
        BOOL,
        NULL_KIND,
        NUMBER,
        OBJECT,
        STRING,
        ;
    }

    public JsonnetJsonValue(final Kind kind, final String string, final double number) {
        this.kind = kind;
        this.string = string;
        this.number = number;
    }

    public JsonnetJsonValue(final JsonnetJsonValue other) {
        this.kind = other.kind;
        this.string = other.string;
        this.number = other.number;
    }

    private final Kind kind;
    private final String string;
    private final double number;  // Also used for bool (0.0 and 1.0)
    private List<JsonnetJsonValue> elements;
    private Map<String, JsonnetJsonValue> fields;
}
