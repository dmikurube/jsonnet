package org.jsonnet.parser;

import java.util.ArrayList;
import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents array constructors [1, 2, 3].
 */
public class Array extends AST {
    public static class Element {
        public AST expr;
        public Fodder commaFodder;

        public Element(final AST expr, final Fodder comma_fodder) {
            this.expr = expr;
            this.commaFodder = comma_fodder;
        }
    }

    public static class Elements extends ArrayList<Element> {
    }

    private final Elements elements;
    private final boolean trailingComma;
    private final Fodder closeFodder;

    public Array(final LocationRange lr,
                 final Fodder open_fodder,
                 final Elements elements,
                 final boolean trailing_comma,
                 final Fodder close_fodder) {
        super(lr, ASTType.AST_ARRAY, open_fodder);
        this.elements = elements;
        this.trailingComma = trailing_comma;
        this.closeFodder = close_fodder;
    }
}
