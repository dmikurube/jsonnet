package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents the $ keyword.
 */
public class Dollar extends AST {
    public Dollar(final LocationRange lr, final Fodder open_fodder) {
        super(lr, ASTType.AST_DOLLAR, open_fodder);
    }
}
