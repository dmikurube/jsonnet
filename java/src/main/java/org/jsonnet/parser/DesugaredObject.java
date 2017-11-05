package org.jsonnet.parser;

import java.util.ArrayList;
import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents object constructors { f: e ... } after desugaring.
 *
 * The assertions either return true or raise an error.
 */
public class DesugaredObject extends AST {
    public static class Field {
        private final ObjectField.Hide hide;
        private final AST name;
        private final AST body;

        public Field(final ObjectField.Hide hide,
                     final AST name,
                     final AST body) {
            this.hide = hide;
            this.name = name;
            this.body = body;
        }
    }

    public static class Fields extends ArrayList<Field> {
    }

    private final ASTs asserts;
    private final Fields fields;

    public DesugaredObject(final LocationRange lr,
                           final ASTs asserts,
                           final Fields fields) {
        super(lr, ASTType.AST_DESUGARED_OBJECT, new Fodder());
        this.asserts = asserts;
        this.fields = fields;
    }
}
