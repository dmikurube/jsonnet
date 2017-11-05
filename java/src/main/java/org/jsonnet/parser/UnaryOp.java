package org.jsonnet.parser;

public enum UnaryOp {
    UOP_NOT("!"),
    UOP_BITWISE_NOT("~"),
    UOP_PLUS("+"),
    UOP_MINUS("-");

    private UnaryOp(final String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return this.string;
    }

    private final String string;
}
