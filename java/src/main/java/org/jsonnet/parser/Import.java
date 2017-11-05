package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents import "file".
 */
public class Import extends AST {
    private final LiteralString file;

    public Import(final LocationRange lr,
                  final Fodder open_fodder,
                  final LiteralString file) {
        super(lr, ASTType.AST_IMPORT, open_fodder);
        this.file = file;
    }
}
