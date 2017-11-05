package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents closures.
 */
public class Function extends AST {
    private final Fodder parenLeftFodder;
    private final ArgParams params;
    private final boolean trailingComma;
    private final Fodder parenRightFodder;
    private final AST body;

    public Function(final LocationRange lr,
                    final Fodder open_fodder,
                    final Fodder paren_left_fodder,
                    final ArgParams params,
                    final boolean trailing_comma,
                    final Fodder paren_right_fodder,
                    final AST body) {
        super(lr, ASTType.AST_FUNCTION, open_fodder);
        this.parenLeftFodder = paren_left_fodder;
        this.params = params;
        this.trailingComma = trailing_comma;
        this.parenRightFodder = paren_right_fodder;
        this.body = body;
    }
}
