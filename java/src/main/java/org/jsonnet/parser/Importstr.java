package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents importstr "file".
 */
public class Importstr extends AST {
    private final LiteralString file;

    public Importstr(final LocationRange lr,
                     final Fodder open_fodder,
                     final LiteralString file) {
        super(lr, ASTType.AST_IMPORTSTR, open_fodder);
        this.file = file;
    }
}
