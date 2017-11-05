package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Either an arg in a function apply, or a param in a closure / other function definition.
 *
 * They happen to have exactly the same structure.
 *
 * In the case of an arg, the id is optional and the expr is required.  Presence of the id indicates
 * that this is a named rather than positional argument.
 *
 * In the case of a param, the id is required and if expr is given, it is a default argument to be
 * used when no argument is bound to the param.
 */
public class ArgParam {
    private Fodder idFodder;       // Empty if no id.
    private Identifier id;        // nullptr if there isn't one
    private final Fodder eqFodder;       // Empty if no id or no expr.
    private AST expr;             // nullptr if there wasn't one.
    private final Fodder commaFodder;    // Before the comma (if there is a comma).

    // Only has id
    public ArgParam(final Fodder id_fodder, final Identifier id, final Fodder comma_fodder) {
        this(id_fodder, id, null, null, comma_fodder);
    }

    // Only has expr
    public ArgParam(final AST expr, final Fodder comma_fodder)
    {
        this(null, null, null, expr, comma_fodder);
    }

    // Has both id and expr
    public ArgParam(final Fodder id_fodder,
                    final Identifier id,
                    final Fodder eq_fodder,
                    final AST expr,
                    final Fodder comma_fodder)
    {
        this.idFodder = id_fodder;
        this.id = id;
        this.eqFodder = eq_fodder;
        this.expr = expr;
        this.commaFodder = comma_fodder;
    }

    public Fodder getIdFodder() {
        return this.idFodder;
    }

    // This is intentionally package-only from Parser so that ArgParam would be immutable finally.
    void setIdFodder(final Fodder idFodder) {
        this.idFodder = idFodder;
    }

    public Identifier getId() {
        return this.id;
    }

    // This is intentionally package-only from Parser so that ArgParam would be immutable finally.
    void setId(final Identifier id) {
        this.id = id;
    }

    public AST getExpr() {
        return this.expr;
    }

    // This is intentionally package-only from Parser so that ArgParam would be immutable finally.
    void setExpr(final AST expr) {
        this.expr = expr;
    }
}
