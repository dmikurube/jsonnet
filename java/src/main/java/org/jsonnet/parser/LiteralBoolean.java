package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents true and false.
 */
public class LiteralBoolean extends AST {
    private final boolean value;

    public LiteralBoolean(final LocationRange lr,
                          final Fodder open_fodder,
                          final boolean value) {
        super(lr, ASTType.AST_LITERAL_BOOLEAN, open_fodder);
        this.value = value;
    }
}
