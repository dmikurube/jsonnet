package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents function calls.
 */
public class Apply extends AST {
    private final AST target;
    private final Fodder fodderL;
    private final ArgParams args;
    private final boolean trailingComma;
    private final Fodder fodderR;
    private final Fodder tailstrictFodder;
    private final boolean tailstrict;

    public Apply(final LocationRange lr,
                 final Fodder open_fodder,
                 final AST target,
                 final Fodder fodder_l,
                 final ArgParams args,
                 final boolean trailing_comma,
                 final Fodder fodder_r,
                 final Fodder tailstrict_fodder,
                 final boolean tailstrict)
    {
        super(lr, ASTType.AST_APPLY, open_fodder);
        this.target = target;
        this.fodderL = fodder_l;
        this.args = args;
        this.trailingComma = trailing_comma;
        this.fodderR = fodder_r;
        this.tailstrictFodder = tailstrict_fodder;
        this.tailstrict = tailstrict;
    }
}
