package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents error e.
 */
public class Error extends AST {
    private final AST expr;

    public Error(final LocationRange lr, final Fodder open_fodder, final AST expr) {
        super(lr, ASTType.AST_ERROR, open_fodder);
        this.expr = expr;
    }
}
