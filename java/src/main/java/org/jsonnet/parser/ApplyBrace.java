package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents e { }.  Desugared to e + { }.
 */
public class ApplyBrace extends AST {
    private final AST left;
    private final AST right;  // This is always an object or object comprehension.

    public ApplyBrace(final LocationRange lr,
                      final Fodder open_fodder,
                      final AST left,
                      final AST right) {
        super(lr, ASTType.AST_BINARY, open_fodder);
        this.left = left;
        this.right = right;
    }
}
