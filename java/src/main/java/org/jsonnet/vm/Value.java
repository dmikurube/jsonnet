package org.jsonnet.vm;

import java.util.ArrayList;
import org.jsonnet.parser.AST;

/**
 * Tagged union of all values.
 *
 * Primitives (<= 8 bytes) are copied by value.  Otherwise a pointer to a HeapEntity is used.
 */
public class Value {
    private Value(final Type t, final HeapEntity vHeapEntity, final double vDouble, final boolean vBoolean) {
        this.t = t;
        this.vHeapEntity = vHeapEntity;
        this.vDouble = vDouble;
        this.vBoolean = vBoolean;
    }

    private Value(final Type t, final HeapEntity vHeapEntity) {
        this(t, vHeapEntity, 0.0, false);
    }

    public Value(final Type t) {
        this.t = t;
    }

    public static Value makeBoolean(final boolean v) {
        return new Value(Type.BOOLEAN, null, 0.0, v);
    }

    public static Value makeNumber(final double v) {
        return new Value(Type.NUMBER, null, v, false);
    }

    public static Value makeNull() {
        return new Value(Type.NULL_TYPE, null, 0.0, false);
    }

    public static Value makeArray(final ArrayList<HeapThunk> v) {
        return new Value(Type.ARRAY, new HeapArray(v));
    }

    public static Value makeClosure(final BindingFrame env,
                                    final HeapObject self,
                                    final int offset,
                                    final HeapClosure.Params params,
                                    final AST body) {
        return new Value(Type.FUNCTION, new HeapClosure(env, self, offset, params, body, ""));
    }

    public static Value makeBuiltin(final String name, final HeapClosure.Params params) {
        return new Value(Type.FUNCTION,
                         new HeapClosure(new BindingFrame(), null, 0, params, null, name));
    }

    public static Value makeString(final String v) {
        return new Value(Type.STRING, new HeapString(v));
    }

    public enum Type {
        NULL_TYPE(false, 0x0, "null"),  // Unfortunately NULL is a macro in C.
        BOOLEAN(false, 0x1, "boolean"),
        NUMBER(false, 0x2, "number"),

        ARRAY(true, 0x10, "array"),
        FUNCTION(true, 0x11, "function"),
        OBJECT(true, 0x12, "object"),
        STRING(true, 0x13, "string"),
        ;

        private Type(final boolean isHeap, final int id, final String string) {
            this.isHeap = isHeap;
            this.id = id;
            this.string = string;
        }

        @Override
        public String toString() {
            return this.string;
        }

        public boolean isHeap() {
            return this.isHeap;
        }

        private final boolean isHeap;
        private final int id;
        private final String string;
    }

    @Override
    public String toString() {
        return this.t.toString();
    }

    public Type getType() {
        return this.t;
    }

    public boolean isHeap() {
        return this.t.isHeap();
    }

    public HeapEntity getValueHeapEntity() {
        return this.vHeapEntity;
    }

    public double getValueDouble() {
        return this.vDouble;
    }

    public boolean getValueBoolean() {
        return this.vBoolean;
    }

    private Type t;

    private HeapEntity vHeapEntity;
    private double vDouble;
    private boolean vBoolean;
}
