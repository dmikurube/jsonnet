package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents the null keyword.
 */
public class LiteralNull extends AST {
    public LiteralNull(final LocationRange lr,
                       final Fodder open_fodder) {
        super(lr, ASTType.AST_LITERAL_NULL, open_fodder);
    }
}
