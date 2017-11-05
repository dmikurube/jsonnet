package org.jsonnet.vm;

import org.jsonnet.parser.AST;
import org.jsonnet.parser.Identifier;

/**
 * Hold an unevaluated expression.  This implements lazy semantics.
 */
public class HeapThunk extends HeapEntity {
    public HeapThunk(final Identifier name, final HeapObject self, final int offset, final AST body) {
        this.filled = false;
        this.name = name;
        this.self = self;
        this.offset = offset;
        this.body = body;
        this.upValues = new BindingFrame();
    }

    public void fill(final Value v) {
        this.content = v;
        this.filled = true;
        this.self = null;
        this.upValues.clear();
    }

    public boolean isFilled() {
        return this.filled;
    }

    public Value getContent() {
        return this.content;
    }

    public Identifier getName() {
        return this.name;
    }

    /**
     * Whether or not the thunk was forced.
     */
    private boolean filled;

    /**
     * The result when the thunk was forced, if filled == true.
     */
    private Value content;

    /**
     * Used in error tracebacks.
     */
    private final Identifier name;

    /**
     * The captured environment.
     *
     * Note, this is non-const because we have to add cyclic references to it.
     */
    private final BindingFrame upValues;

    /**
     * The captured self variable, or nullptr if there was none.  \see CallFrame.
     */
    private HeapObject self;

    /**
     * The offset from the captured self variable. \see CallFrame.
     */
    private final int offset;

    /**
     * Evaluated to force the thunk.
     */
    private final AST body;
}
