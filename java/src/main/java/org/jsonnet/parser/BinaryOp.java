package org.jsonnet.parser;

public enum BinaryOp {
    BOP_MULT("*"),
    BOP_DIV("/"),
    BOP_PERCENT("%"),

    BOP_PLUS("+"),
    BOP_MINUS("-"),

    BOP_SHIFT_L("<<"),
    BOP_SHIFT_R(">>"),

    BOP_GREATER(">"),
    BOP_GREATER_EQ(">="),
    BOP_LESS("<"),
    BOP_LESS_EQ("<="),
    BOP_IN("in"),

    BOP_MANIFEST_EQUAL("=="),
    BOP_MANIFEST_UNEQUAL("!="),

    BOP_BITWISE_AND("&"),
    BOP_BITWISE_XOR("^"),
    BOP_BITWISE_OR("|"),

    BOP_AND("&&"),
    BOP_OR("||");

    private BinaryOp(final String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return this.string;
    }

    private final String string;
}
