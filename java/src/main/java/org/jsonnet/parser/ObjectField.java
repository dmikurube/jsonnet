package org.jsonnet.parser;

import org.jsonnet.lexer.Fodder;

public class ObjectField {
    // Depending on the kind of Jsonnet field, the fields of this C++ class are used for storing
    // different parts of the AST.
    public enum Kind {
        // <fodder1> 'assert' <expr2>
        // [ <opFodder> : <expr3> ]
        // <commaFodder>
        ASSERT,

        // <fodder1> id
        // [ <fodderL> '(' <params> <fodderR> ')' ]
        // <opFodder> [+]:[:[:]] <expr2>
        // <commaFodder>
        FIELD_ID,

        // <fodder1> '[' <expr1> <fodder2> ']'
        // [ <fodderL> '(' <params> <fodderR> ')' ]
        // <opFodder> [+]:[:[:]] <expr2>
        // <commaFodder>
        FIELD_EXPR,

        // <expr1>
        // <fodderL> '(' <params> <fodderR> ')'
        // <opFodder> [+]:[:[:]] <expr2>
        // <commaFodder>
        FIELD_STR,

        // <fodder1> 'local' <fodder2> id
        // [ <fodderL> '(' <params> <fodderR> ')' ]
        // [ <opFodder> = <expr2> ]
        // <commaFodder>
        LOCAL,
    }

    // NOTE TO SELF: sort out fodder1-4, then modify desugarer (maybe) parser and unparser.

    public enum Hide {
        HIDDEN,   // f:: e
        INHERIT,  // f: e
        VISIBLE,  // f::: e
    }

    private final Kind kind;
    private final Fodder fodder1, fodder2, fodderL, fodderR;
    private final Hide hide;    // (ignore if kind != FIELD_something
    private final boolean superSugar;   // +:  (ignore if kind != FIELD_something)
    private final boolean methodSugar;  // f(x, y, z): ...  (ignore if kind  == ASSERT)
    private final AST expr1;        // Not in scope of the object
    private final Identifier id;
    private final ArgParams params;    // If methodSugar == true then holds the params.
    private final boolean trailingComma;  // If methodSugar == true then remembers the trailing comma.
    private final Fodder opFodder;     // Before the : or =
    private final AST expr2, expr3;  // In scope of the object (can see self).
    private final Fodder commaFodder;  // If this field is followed by a comma, this is its fodder.

    public ObjectField(final Kind kind,
                       final Fodder fodder1,
                       final Fodder fodder2,
                       final Fodder fodder_l,
                       final Fodder fodder_r,
                       final Hide hide,
                       final boolean super_sugar,
                       final boolean method_sugar,
                       final AST expr1,
                       final Identifier id,
                       final ArgParams params,
                       final boolean trailing_comma,
                       final Fodder op_fodder,
                       final AST expr2,
                       final AST expr3,
                       final Fodder comma_fodder) {
        // Enforce what is written in comments above.
        this.kind = kind;
        this.fodder1 = fodder1;
        this.fodder2 = fodder2;
        this.fodderL = fodder_l;
        this.fodderR = fodder_r;
        this.hide = hide;
        this.superSugar = super_sugar;
        this.methodSugar = method_sugar;
        this.expr1 = expr1;
        this.id = id;
        this.params = params;
        this.trailingComma = trailing_comma;
        this.opFodder = op_fodder;
        this.expr2 = expr2;
        this.expr3 = expr3;
        this.commaFodder = comma_fodder;
        /*
        assert(kind != ASSERT || (hide == VISIBLE && !superSugar && !methodSugar));
        assert(kind != LOCAL || (hide == VISIBLE && !superSugar));
        assert(kind != FIELD_ID || (id != nullptr && expr1 == nullptr));
        assert(kind == FIELD_ID || kind == LOCAL || id == nullptr);
        assert(methodSugar || (params.size() == 0 && !trailingComma));
        assert(kind == ASSERT || expr3 == nullptr);
        */
    }

    // For when we don't know if it's a function or not.
    public static ObjectField Local(final Fodder fodder1,
                                    final Fodder fodder2,
                                    final Fodder fodder_l,
                                    final Fodder fodder_r,
                                    final boolean method_sugar,
                                    final Identifier id,
                                    final ArgParams params,
                                    final boolean trailing_comma,
                                    final Fodder op_fodder,
                                    final AST body,
                                    final Fodder comma_fodder) {
        return new ObjectField(Kind.LOCAL,
                               fodder1,
                               fodder2,
                               fodder_l,
                               fodder_r,
                               Hide.VISIBLE,
                               false,
                               method_sugar,
                               null,
                               id,
                               params,
                               trailing_comma,
                               op_fodder,
                               body,
                               null,
                               comma_fodder);
    }

    public static ObjectField Local(final Fodder fodder1,
                                    final Fodder fodder2,
                                    final Identifier id,
                                    final Fodder op_fodder,
                                    final AST body,
                                    final Fodder comma_fodder) {
        return new ObjectField(Kind.LOCAL,
                               fodder1,
                               fodder2,
                               new Fodder(),
                               new Fodder(),
                               Hide.VISIBLE,
                               false,
                               false,
                               null,
                               id,
                               new ArgParams(),
                               false,
                               op_fodder,
                               body,
                               null,
                               comma_fodder);
    }

    public static ObjectField LocalMethod(final Fodder fodder1,
                                          final Fodder fodder2,
                                          final Fodder fodder_l,
                                          final Fodder fodder_r,
                                          final Identifier id,
                                          final ArgParams params,
                                          final boolean trailing_comma,
                                          final Fodder op_fodder,
                                          final AST body,
                                          final Fodder comma_fodder) {
        return new ObjectField(Kind.LOCAL,
                               fodder1,
                               fodder2,
                               fodder_l,
                               fodder_r,
                               Hide.VISIBLE,
                               false,
                               true,
                               null,
                               id,
                               params,
                               trailing_comma,
                               op_fodder,
                               body,
                               null,
                               comma_fodder);
    }

    public static ObjectField Assert(final Fodder fodder1,
                                     final AST body,
                                     final Fodder op_fodder,
                                     final AST msg,
                                     final Fodder comma_fodder) {
        return new ObjectField(Kind.ASSERT,
                               fodder1,
                               new Fodder(),
                               new Fodder(),
                               new Fodder(),
                               Hide.VISIBLE,
                               false,
                               false,
                               null,
                               null,
                               new ArgParams(),
                               false,
                               op_fodder,
                               body,
                               msg,
                               comma_fodder);
    }

    public Kind getKind() {
        return this.kind;
    }

    public Hide getHide() {
        return this.hide;
    }
}
