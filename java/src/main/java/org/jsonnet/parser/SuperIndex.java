package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents the super[e] and super.f constructs.
 *
 * Either index or identifier will be set before desugaring.  After desugaring, id will be
 * nullptr.
 */
public class SuperIndex extends AST {
    private final Fodder dotFodder;
    private final AST index;
    private final Fodder idFodder;
    private final Identifier id;

    public SuperIndex(final LocationRange lr,
                      final Fodder open_fodder,
                      final Fodder dot_fodder,
                      final AST index,
                      final Fodder id_fodder,
                      final Identifier id) {
        super(lr, ASTType.AST_SUPER_INDEX, open_fodder);
        this.dotFodder = dot_fodder;
        this.index = index;
        this.idFodder = id_fodder;
        this.id = id;
    }
}
