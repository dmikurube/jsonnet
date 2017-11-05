package org.jsonnet.parser;

import org.jsonnet.lexer.Fodder;

/**
 * Used in Object & Array Comprehensions.
 */
public class ComprehensionSpec {
    public enum Kind {
        FOR,
        IF;
    }

    private final Kind kind;
    private final Fodder openFodder;
    private final Fodder varFodder;       // {} when kind != SPEC_FOR.
    private final Identifier var;         // Null when kind != SPEC_FOR.
    private final Fodder inFodder;        // {} when kind != SPEC_FOR.
    private final AST expr;

    public ComprehensionSpec(final Kind kind,
                             final Fodder open_fodder,
                             final Fodder var_fodder,
                             final Identifier var,
                             final Fodder in_fodder,
                             final AST expr) {
        this.kind = kind;
        this.openFodder = open_fodder;
        this.varFodder = var_fodder;
        this.var = var;
        this.inFodder = in_fodder;
        this.expr = expr;
    }
}
