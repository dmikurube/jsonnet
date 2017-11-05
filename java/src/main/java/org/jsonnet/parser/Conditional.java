package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents if then else.
 *
 * After parsing, branchFalse can be nullptr indicating that no else branch was specified.  The
 * desugarer fills this in with a LiteralNull.
 */
public class Conditional extends AST {
    private final AST cond;
    private final Fodder thenFodder;
    private final AST branchTrue;
    private final Fodder elseFodder;
    private final AST branchFalse;

    public Conditional(final LocationRange lr,
                       final Fodder open_fodder,
                       final AST cond,
                       final Fodder then_fodder,
                       final AST branch_true,
                       final Fodder else_fodder,
                       final AST branch_false) {
        super(lr, ASTType.AST_CONDITIONAL, open_fodder);
        this.cond = cond;
        this.thenFodder = then_fodder;
        this.branchTrue = branch_true;
        this.elseFodder = else_fodder;
        this.branchFalse = branch_false;
    }
}
