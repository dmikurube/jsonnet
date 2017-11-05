package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents an assert expression (not an object-level assert).
 *
 * After parsing, message can be nullptr indicating that no message was specified. This AST is
 * elimiated by desugaring.
 */
public class Assert extends AST {
    private final AST cond;
    private final Fodder colonFodder;
    private final AST message;
    private final Fodder semicolonFodder;
    private final AST rest;

    public Assert(final LocationRange lr,
                  final Fodder open_fodder,
                  final AST cond,
                  final Fodder colon_fodder,
                  final AST message,
                  final Fodder semicolon_fodder,
                  final AST rest) {
        super(lr, ASTType.AST_ASSERT, open_fodder);
        this.cond = cond;
        this.colonFodder = colon_fodder;
        this.message = message;
        this.semicolonFodder = semicolon_fodder;
        this.rest = rest;
    }
}
