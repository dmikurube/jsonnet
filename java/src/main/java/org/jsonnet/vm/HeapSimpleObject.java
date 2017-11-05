package org.jsonnet.vm;

import java.util.ArrayList;
import java.util.Map;
import org.jsonnet.parser.AST;
import org.jsonnet.parser.Identifier;
import org.jsonnet.parser.ObjectField;

/**
 * Objects created via the simple object constructor construct.
 */
public class HeapSimpleObject extends HeapLeafObject {
    public HeapSimpleObject(final BindingFrame upValues,
                            final Map<Identifier, Field> fields,
                            final ArrayList<AST> asserts) {
        this.upValues = upValues;
        this.fields = fields;
        this.asserts = asserts;
    }

    // TODO(dmikurube): Encapsulate.
    public static class Field {
        public ObjectField.Hide getHide() {
            return this.hide;
        }

        /**
         * Will the field appear in output?
         */
        private ObjectField.Hide hide;

        /**
         * Expression that is evaluated when indexing this field.
         */
        private AST body;
    }

    public Map<Identifier, Field> getFields() {
        return this.fields;
    }

    /**
     * The fields.
     *
     * These are evaluated in the captured environment and with self and super bound
     * dynamically.
     */
    private final Map<Identifier, Field> fields;

    /**
     * The object's invariants.
     *
     * These are evaluated in the captured environment with self and super bound.
     */
    private ArrayList<AST> asserts;

    /**
     * The captured environment.
     */
    private final BindingFrame upValues;
}
