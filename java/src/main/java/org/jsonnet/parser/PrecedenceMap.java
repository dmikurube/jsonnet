package org.jsonnet.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PrecedenceMap {
    // Precedences used by various compilation units are defined here.
    public static final int APPLY_PRECEDENCE = 2;         // Function calls and indexing.
    public static final int UNARY_PRECEDENCE = 4;         // Logical and bitwise negation, unary + -
    public static final int BEFORE_ELSE_PRECEDENCE = 15;  // True branch of an if.
    public static final int MAX_PRECEDENCE = 16;          // Local, If, Import, Function, Error

    /**
     * These are the binary operator precedences, unary precedence is given by
     * UNARY_PRECEDENCE.
     */
    private static Map<BinaryOp, Integer> build_precedence_map() {
        final Map<BinaryOp, Integer> r = new HashMap<BinaryOp, Integer>();

        r.put(BinaryOp.BOP_MULT, 5);
        r.put(BinaryOp.BOP_DIV, 5);
        r.put(BinaryOp.BOP_PERCENT, 5);

        r.put(BinaryOp.BOP_PLUS, 6);
        r.put(BinaryOp.BOP_MINUS, 6);

        r.put(BinaryOp.BOP_SHIFT_L, 7);
        r.put(BinaryOp.BOP_SHIFT_R, 7);

        r.put(BinaryOp.BOP_GREATER, 8);
        r.put(BinaryOp.BOP_GREATER_EQ, 8);
        r.put(BinaryOp.BOP_LESS, 8);
        r.put(BinaryOp.BOP_LESS_EQ, 8);
        r.put(BinaryOp.BOP_IN, 8);

        r.put(BinaryOp.BOP_MANIFEST_EQUAL, 9);
        r.put(BinaryOp.BOP_MANIFEST_UNEQUAL, 9);

        r.put(BinaryOp.BOP_BITWISE_AND, 10);

        r.put(BinaryOp.BOP_BITWISE_XOR, 11);

        r.put(BinaryOp.BOP_BITWISE_OR, 12);

        r.put(BinaryOp.BOP_AND, 13);

        r.put(BinaryOp.BOP_OR, 14);

        return Collections.unmodifiableMap(r);
    }

    private static Map<String, UnaryOp> build_unary_map() {
        final Map<String, UnaryOp> r = new HashMap<String, UnaryOp>();
        r.put("!", UnaryOp.UOP_NOT);
        r.put("~", UnaryOp.UOP_BITWISE_NOT);
        r.put("+", UnaryOp.UOP_PLUS);
        r.put("-", UnaryOp.UOP_MINUS);
        return Collections.unmodifiableMap(r);
    }

    private static Map<String, BinaryOp> build_binary_map() {
        final Map<String, BinaryOp> r = new HashMap<String, BinaryOp>();

        r.put("*", BinaryOp.BOP_MULT);
        r.put("/", BinaryOp.BOP_DIV);
        r.put("%", BinaryOp.BOP_PERCENT);

        r.put("+", BinaryOp.BOP_PLUS);
        r.put("-", BinaryOp.BOP_MINUS);

        r.put("<<", BinaryOp.BOP_SHIFT_L);
        r.put(">>", BinaryOp.BOP_SHIFT_R);

        r.put(">", BinaryOp.BOP_GREATER);
        r.put(">=", BinaryOp.BOP_GREATER_EQ);
        r.put("<", BinaryOp.BOP_LESS);
        r.put("<=", BinaryOp.BOP_LESS_EQ);
        r.put("in", BinaryOp.BOP_IN);

        r.put("==", BinaryOp.BOP_MANIFEST_EQUAL);
        r.put("!=", BinaryOp.BOP_MANIFEST_UNEQUAL);

        r.put("&", BinaryOp.BOP_BITWISE_AND);
        r.put("^", BinaryOp.BOP_BITWISE_XOR);
        r.put("|", BinaryOp.BOP_BITWISE_OR);

        r.put("&&", BinaryOp.BOP_AND);
        r.put("||", BinaryOp.BOP_OR);
        return Collections.unmodifiableMap(r);
    }

    public static final Map<BinaryOp, Integer> PRECEDENCE_MAP = build_precedence_map();
    public static final Map<String, UnaryOp> UNARY_MAP = build_unary_map();
    public static final Map<String, BinaryOp> BINARY_MAP = build_binary_map();
}
