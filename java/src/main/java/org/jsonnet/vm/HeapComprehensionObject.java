package org.jsonnet.vm;

import java.util.Map;
import org.jsonnet.parser.AST;
import org.jsonnet.parser.Identifier;

/**
 * Objects created by the ObjectComprehensionSimple construct.
 */
public class HeapComprehensionObject extends HeapLeafObject {
    public HeapComprehensionObject(final BindingFrame upValues,
                                   final AST value,
                                   final Identifier id,
                                   final Map<Identifier, HeapThunk> compValues) {
        this.upValues = upValues;
        this.value = value;
        this.id = id;
        this.compValues = compValues;  // TODO: Immutable?
    }

    public Map<Identifier, HeapThunk> getCompValues() {
        return this.compValues;
    }

    /**
     * The captured environment.
     */
    private final BindingFrame upValues;

    /**
     * The expression used to compute the field values.
     */
    private final AST value;

    /**
     * The identifier of bound variable in that construct.
     */
    private final Identifier id;

    /**
     * Binding for id.
     *
     * For each field, holds the value that should be bound to id.  This is the corresponding
     * array element from the original array used to define this object.  This should not really
     * be a thunk, but it makes the implementation easier.
     *
     * It is convenient to make this non-const to allow building up the values one by one, so that
     * the garbage collector can see them at each intermediate point.
     */
    private final Map<Identifier, HeapThunk> compValues;
}
