package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents variables.
 */
public class Var extends AST {
    private final Identifier id;

    public Var(final LocationRange lr,
               final Fodder open_fodder,
               final Identifier id) {
        super(lr, ASTType.AST_VAR, open_fodder);
        this.id = id;
    }

    public Identifier getId() {
        return this.id;
    }
}
