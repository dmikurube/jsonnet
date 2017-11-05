package org.jsonnet.vm;

import java.util.ArrayList;
import org.jsonnet.parser.AST;
import org.jsonnet.parser.Identifier;

/**
 * Stores the function itself and also the captured environment.
 *
 * Either body is non-null and builtinName is "", or body is null and builtin refers to a built-in
 * function.  In the former case, the closure represents a user function, otherwise calling it
 * will trigger the builtin function to execute.  Params is empty when the function is a
 * builtin.
 */
public class HeapClosure extends HeapEntity {
    public HeapClosure(final BindingFrame upValues,
                       final HeapObject self,
                       final int offset,
                       final Params params,
                       final AST body,
                       final String builtinName) {
        this.upValues = upValues;
        this.self = self;
        this.offset = offset;
        this.params = params;
        this.body = body;
        this.builtinName = builtinName;
    }

    public static class Param {
        public Param(final Identifier id, final AST def) {
            this.id = id;
            this.def = def;
        }

        public Identifier getId() {
            return this.id;
        }

        final Identifier id;
        final AST def;
    }

    public static class Params extends ArrayList<Param> {
    }

    public BindingFrame getUpValues() {
        return this.upValues;
    }

    public HeapObject getSelf() {
        return this.self;
    }

    public int getOffset() {
        return this.offset;
    }

    public Params getParams() {
        return this.params;
    }

    public AST getBody() {
        return this.body;
    }

    public String getBuiltinName() {
        return this.builtinName;
    }

    /**
     * The captured environment.
     */
    private final BindingFrame upValues;

    /**
     * The captured self variable, or nullptr if there was none.  \see Frame.
k     */
    private HeapObject self;

    /**
     * The offset from the captured self variable.  \see Frame.
     */
    private int offset;

    private final Params params;
    private final AST body;
    private final String builtinName;
}
