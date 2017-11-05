package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents object constructors { f: e ... }.
 *
 * The trailing comma is only allowed if fields.size() > 0.  Converted to DesugaredObject during
 * desugaring.
 */
public class Object extends AST {
    private final ObjectFields fields;
    private final boolean trailingComma;
    private final Fodder closeFodder;

    public Object(final LocationRange lr,
                  final Fodder open_fodder,
                  final ObjectFields fields,
                  final boolean trailing_comma,
                  final Fodder close_fodder) {
        super(lr, ASTType.AST_OBJECT, open_fodder);
        this.fields = fields;
        this.trailingComma = trailing_comma;
        this.closeFodder = close_fodder;
        /*
        assert(fields.size() > 0 || !trailing_comma);
        if (fields.size() > 0) {
            assert(trailing_comma || fields[fields.size() - 1].commaFodder.size() == 0);
        }
        */
    }
}
