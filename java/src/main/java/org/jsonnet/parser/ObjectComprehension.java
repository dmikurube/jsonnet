package org.jsonnet.parser;

import java.util.ArrayList;
import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents object comprehension { [e]: e for x in e for.. if... }.
 */
public class ObjectComprehension extends AST {
    private final ObjectFields fields;
    private final boolean trailingComma;
    private final ArrayList<ComprehensionSpec> specs;
    private final Fodder closeFodder;

    public ObjectComprehension(final LocationRange lr,
                               final Fodder open_fodder,
                               final ObjectFields fields,
                               final boolean trailing_comma,
                               final ArrayList<ComprehensionSpec> specs,
                               final Fodder close_fodder) {
        super(lr, ASTType.AST_OBJECT_COMPREHENSION, open_fodder);
        this.fields = fields;
        this.trailingComma = trailing_comma;
        this.specs = specs;
        this.closeFodder = close_fodder;
    }
}
