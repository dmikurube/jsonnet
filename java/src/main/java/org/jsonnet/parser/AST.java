package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * All AST nodes are subtypes of this class.
 */
public abstract class AST {
    private final LocationRange location;
    private final ASTType type;
    private final Fodder openFodder;
    private Identifiers freeVariables;

    public AST(final LocationRange location, final ASTType type, final Fodder open_fodder) {
        this.location = location;
        this.type = type;
        this.openFodder = open_fodder;
    }

    public LocationRange getLocation() {
        return this.location;
    }

    public ASTType getType() {
        return this.type;
    }

    public Fodder getOpenFodder() {
        return this.openFodder;
    }
}
