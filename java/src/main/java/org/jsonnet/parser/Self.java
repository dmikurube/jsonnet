package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents the self keyword.
 */
public class Self extends AST {
    public Self(final LocationRange lr,
                final Fodder open_fodder) {
        super(lr, ASTType.AST_SELF, open_fodder);
    }
}
