package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents (e), which is desugared.
 */
public class Parens extends AST {
    private final AST expr;
    private final Fodder closeFodder;

    public Parens(final LocationRange lr,
                  final Fodder open_fodder,
                  final AST expr,
                  final Fodder close_fodder) {
        super(lr, ASTType.AST_PARENS, open_fodder);
        this.expr = expr;
        this.closeFodder = close_fodder;
    }
}
