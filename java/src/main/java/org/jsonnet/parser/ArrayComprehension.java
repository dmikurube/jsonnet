package org.jsonnet.parser;

import java.util.ArrayList;
import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents array comprehensions (which are like Python list comprehensions).
 */
public class ArrayComprehension extends AST {
    private final AST body;
    private final Fodder commaFodder;
    private final boolean trailingComma;
    private final ArrayList<ComprehensionSpec> specs;
    private final Fodder closeFodder;

    public ArrayComprehension(final LocationRange lr,
                              final Fodder open_fodder,
                              final AST body,
                              final Fodder comma_fodder,
                              final boolean trailing_comma,
                              final ArrayList<ComprehensionSpec> specs,
                              final Fodder close_fodder) {
        super(lr, ASTType.AST_ARRAY_COMPREHENSION, open_fodder);
        this.body = body;
        this.commaFodder = comma_fodder;
        this.trailingComma = trailing_comma;
        this.specs = specs;
        this.closeFodder = close_fodder;
        // assert(specs.size() > 0);
    }
}
