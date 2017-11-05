package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents unary operators.
 */
public class Unary extends AST {
    private final UnaryOp op;
    private final AST expr;

    public Unary(final LocationRange lr,
                 final Fodder open_fodder,
                 final UnaryOp op,
                 final AST expr) {
        super(lr, ASTType.AST_UNARY, open_fodder);
        this.op = op;
        this.expr = expr;
    }
}
