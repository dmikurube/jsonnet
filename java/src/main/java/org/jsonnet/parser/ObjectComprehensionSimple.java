package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents post-desugaring object comprehension { [e]: e for x in e }.
 */
public class ObjectComprehensionSimple extends AST {
    private final AST field;
    private final AST value;
    private final Identifier id;
    private final AST array;

    public ObjectComprehensionSimple(final LocationRange lr,
                                     final AST field,
                                     final AST value,
                                     final Identifier id,
                                     final AST array) {
        super(lr, ASTType.AST_OBJECT_COMPREHENSION_SIMPLE, new Fodder());
        this.field = field;
        this.value = value;
        this.id = id;
        this.array = array;
    }
}
