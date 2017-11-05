package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents the e in super construct.
 */
public class InSuper extends AST {
    private final AST element;
    private final Fodder inFodder;
    private final Fodder superFodder;

    public InSuper(final LocationRange lr,
                   final Fodder open_fodder,
                   final AST element,
                   final Fodder in_fodder,
                   final Fodder super_fodder) {
        super(lr, ASTType.AST_IN_SUPER, open_fodder);
        this.element = element;
        this.inFodder = in_fodder;
        this.superFodder = super_fodder;
    }
}
