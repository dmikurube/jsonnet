package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents built-in functions.
 *
 * There is no parse rule to build this AST.  Instead, it is used to build the std object in the
 * interpreter.
 */
public class BuiltinFunction extends AST {
    private final String name;
    private final Identifiers params;

    public BuiltinFunction(final LocationRange lr,
                           final String name,
                           final Identifiers params) {
        super(lr, ASTType.AST_BUILTIN_FUNCTION, new Fodder());
        this.name = name;
        this.params = params;
    }
}
