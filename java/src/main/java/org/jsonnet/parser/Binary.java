package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents binary operators.
 */
public class Binary extends AST {
    private final AST left;
    private final Fodder opFodder;
    private final BinaryOp op;
    private final AST right;

    public Binary(final LocationRange lr,
                  final Fodder open_fodder,
                  final AST left,
                  final Fodder op_fodder,
                  final BinaryOp op,
                  final AST right) {
        super(lr, ASTType.AST_BINARY, open_fodder);
        this.left = left;
        this.opFodder = op_fodder;
        this.op = op;
        this.right = right;
    }
}
