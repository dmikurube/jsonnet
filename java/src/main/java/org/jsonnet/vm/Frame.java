package org.jsonnet.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsonnet.LocationRange;
import org.jsonnet.parser.AST;
import org.jsonnet.parser.Identifier;

/**
 * A frame on the stack.
 *
 * Every time a subterm is evaluated, we first push a new stack frame to
 * store the continuation.
 *
 * The stack frame is a bit like a tagged union, except not as memory
 * efficient.  The set of member variables that are actually used depends on
 * the value of the member varaible kind.
 *
 * If the stack frame is of kind FRAME_CALL, then it counts towards the
 * maximum number of stack frames allowed.  Other stack frames are not
 * counted.  This is because FRAME_CALL exists where there is a branch in
 * the code, e.g. the forcing of a thunk, evaluation of a field, calling a
 * function, etc.
 *
 * The stack is used to mark objects during garbage
 * collection, so HeapObjects not referred to from the stack may be
 * prematurely collected.
 */
public class Frame {
    public Frame(final FrameKind kind, final AST ast) {
        this.kind = kind;
        this.ast = ast;
        this.location = ast.getLocation();
        this.tailCall = false;
        this.elementId = 0;
        this.elements = new HashMap<>();
        this.thunks = new ArrayList<>();
        this.context = null;
        this.self = null;
        this.offset = 0;
        this.bindings = new BindingFrame();
        this.val = new Value(Value.Type.NULL_TYPE);
        this.val2 = new Value(Value.Type.NULL_TYPE);
    }

    public Frame(final FrameKind kind, final LocationRange location) {
        this.kind = kind;
        this.ast = null;
        this.location = location;
        this.tailCall = false;
        this.elementId = 0;
        this.elements = new HashMap<>();
        this.thunks = new ArrayList<>();
        this.context = null;
        this.self = null;
        this.offset = 0;
        this.bindings = new BindingFrame();
        this.val = new Value(Value.Type.NULL_TYPE);
        this.val2 = new Value(Value.Type.NULL_TYPE);
    }

    /**
     * Mark everything visible from this frame.
     */
    /*
    public void mark(final Heap heap) {
        heap.markFrom(this.val);
        heap.markFrom(this.val2);
        if (this.context != null) {  // if (context) in original
            heap.markFrom(this.context);
        }
        if (this.self != null) {
            heap.markFrom(this.self);
        }
        for (Map.Entry<Identifier, HeapThunk> bind : this.bindings.entrySet()) {
            heap.markFrom(bind.getValue());
        }
        for (Map.Entry<Identifier, HeapThunk> el : this.elements.entrySet()) {
            heap.markFrom(el.getValue());
        }
        for (HeapThunk th : this.thunks) {
            heap.markFrom(th);
        }
    }
    */

    public FrameKind getKind() {
        return this.kind;
    }

    public void setKind(final FrameKind kind) {
        this.kind = kind;
    }

    public boolean isCall() {
        return this.kind == FrameKind.FRAME_CALL;
    }

    public LocationRange getLocation() {
        return this.location;
    }

    public boolean isTailCall() {
        return this.tailCall;
    }

    public void setTailCall(final boolean tailCall) {
        this.tailCall = tailCall;
    }

    public void setVal(final Value val) {
        this.val = val;
    }

    public void setVal2(final Value val2) {
        this.val2 = val2;
    }

    public int getElementId() {
        return this.elementId;
    }

    public void setElementId(final int elementId) {
        this.elementId = elementId;
    }

    public List<HeapThunk> getThunks() {
        return this.thunks;
    }

    public void clearThunks() {
        this.thunks.clear();
    }

    public HeapEntity getContext() {
        return this.context;
    }

    public void setContext(final HeapEntity context) {
        this.context = context;
    }

    public HeapObject getSelf() {
        return this.self;
    }

    public void setSelf(final HeapObject self) {
        this.self = self;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public BindingFrame getBindings() {  // TODO: Immutable?
        return this.bindings;
    }

    public void setBindings(final BindingFrame bindings) {  // TODO: Immutable?
        this.bindings = bindings;
    }

    /**
     * Tag (tagged union).
     */
    private FrameKind kind;

    /**
     * The code we were executing before.
     */
    private final AST ast;

    /**
     * The location of the code we were executing before.
     *
     * location == ast->location when ast != nullptr
     */
    private final LocationRange location;

    /**
     * Reuse this stack frame for the purpose of tail call optimization.
     */
    private boolean tailCall;

    /**
     * Used for a variety of purposes.
     */
    private Value val;

    /**
     * Used for a variety of purposes.
     */
    private Value val2;

    /**
     * Used for a variety of purposes.
     */
    // private DesugaredObject::Fields::const_iterator fit;

    /**
     * Used for a variety of purposes.
     */
    // std::map<const Identifier *, HeapSimpleObject::Field> objectFields;

    /**
     * Used for a variety of purposes.
     */
    private int elementId;

    /**
     * Used for a variety of purposes.
     */
    private final Map<Identifier, HeapThunk> elements;

    /**
     * Used for a variety of purposes.
     */
    private final ArrayList<HeapThunk> thunks;

    /**
     * The context is used in error messages to attempt to find a reasonable name for the
     * object, function, or thunk value being executed.  If it is a thunk, it is filled
     * with the value when the frame terminates.
     */
    private HeapEntity context;

    /**
     * The lexically nearest object we are in, or nullptr.  Note
     * that this is not the same as context, because we could be inside a function,
     * inside an object and then context would be the function, but self would still point
     * to the object.
     */
    private HeapObject self;

    /**
     * The "super" level of self.  Sometimes, we look upwards in the
     * inheritance tree, e.g. via an explicit use of super, or because a given field
     * has been inherited.  When evaluating a field from one of these super objects,
     * we need to bind self to the concrete object (so self must point
     * there) but uses of super should be resolved relative to the object whose
     * field we are evaluating.  Thus, we keep a second field for that.  This is
     * usually 0, unless we are evaluating a super object's field.
     */
    private int offset;

    /**
     * A set of variables introduced at this point.
     */
    private BindingFrame bindings;
}
