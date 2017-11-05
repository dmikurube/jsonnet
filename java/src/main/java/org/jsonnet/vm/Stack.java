package org.jsonnet.vm;

import java.util.ArrayList;
import java.util.Map;
import org.jsonnet.JsonnetRuntimeError;
import org.jsonnet.LocationRange;
import org.jsonnet.parser.Identifier;

/**
 * The stack holds all the stack frames and manages the stack frame limit.
 */
public class Stack {
    public Stack(final int limit) {
        this.calls = 0;
        this.limit = limit;
        this.stack = new ArrayList<>();
    }

    public int size() {
        return this.stack.size();
    }

    /**
     * Search for the closest variable in scope that matches the given name.
     */
    public HeapThunk lookUpVar(final Identifier id) {
        for (int i = this.stack.size() - 1; i >= 0; --i) {
            final BindingFrame binds = this.stack.get(i).getBindings();
            final HeapThunk it = binds.get(id);
            if (it != null) {
                return it;
            }
            if (this.stack.get(i).isCall()) {
                break;
            }
        }
        return null;
    }

    /**
     * Mark everything visible from the stack (any frame).
     */
    /*
    void mark(Heap &heap)
    {
        for (const auto &f : stack) {
            f.mark(heap);
        }
    }
    */

    public Frame top() {
        return this.stack.get(this.stack.size() - 1);
    }

    public void pop() {
        synchronized(this.stack) {
            if (this.top().isCall()) {
                this.calls--;
            }
            this.stack.remove(this.stack.size() - 1);
        }
    }

    /**
     * Attempt to find a name for a given heap entity.  This may not be possible, but we try
     * reasonably hard.  We look in the bindings for a variable in the closest scope that
     * happens to point at the entity in question.  Otherwise, the best we can do is use its
     * type.
     */
    public String getName(final int fromHere, final HeapEntity e) {
        String name = "";
        for (int i = fromHere - 1; i >= 0; --i) {
            final Frame f = this.stack.get(i);
            for (Map.Entry<Identifier, HeapThunk> pair : f.getBindings().entrySet()) {
                final HeapThunk thunk = pair.getValue();
                if (!thunk.isFilled()) {
                    continue;
                }
                if (!thunk.getContent().isHeap()) {
                    continue;
                }
                if (e != thunk.getContent().getValueHeapEntity()) {  // TODO: Is "==" fine?
                    continue;
                }
                name = pair.getKey().getName();  // encode_utf8
            }
            // Do not go into the next call frame, keep local reasoning.
            if (f.isCall()) {
                break;
            }
        }

        if (name.equals("")) {
            name = "anonymous";
        }
        if (e instanceof HeapObject) {
            return "object <" + name + ">";
        } else if (e instanceof HeapThunk) {
            final HeapThunk thunk = (HeapThunk) e;
            if (thunk.getName() == null) {
                return "";  // Argument of builtin, or root (since top level functions).
            } else {
                return "thunk <" + thunk.getName().getName() + ">";  // encode_utf8
            }
        } else {
            final HeapClosure func = (HeapClosure) e;
            if (func.getBody() == null) {
                return "builtin function <" + func.getBuiltinName() + ">";
            }
            return "function <" + name + ">";
        }
    }

    /**
     * Dump the stack.
     *
     * This is useful to help debug the VM in gdb.  It is virtual to stop it
     * being removed by the compiler.
     */
    public void dump() {
        for (int i = 0; i < this.stack.size(); ++i) {
            System.out.println("stack[" + i + "] = " + stack.get(i).getLocation() + " (" + stack.get(i).getKind() + ")");
        }
        System.out.println("");
    }

    /**
     * Creates the error object for throwing, and also populates it with the stack trace.
     */
    public JsonnetRuntimeError makeError(final LocationRange loc, final String msg) {
        final ArrayList<TraceFrame> stackTrace = new ArrayList<>();
        stackTrace.add(new TraceFrame(loc));
        for (int i = this.stack.size() - 1; i >= 0; --i) {
            final Frame f = stack.get(i);
            if (f.isCall()) {
                if (f.getContext() != null) {
                    // Give the last line a name.
                    stackTrace.get(stackTrace.size() - 1).setName(this.getName(i, f.getContext()));
                }
                if (f.getLocation().isSet() || f.getLocation().getFile().length() > 0) {
                    stackTrace.add(new TraceFrame(f.getLocation()));
                }
            }
        }
        return new JsonnetRuntimeError(stackTrace, msg);
    }

    /**
     * New (non-call) frame.
     */
    /*
    public template <class... Args>
    void newFrame(Args... args) {
        stack.emplace_back(args...);
    }
    */

    /**
     * If there is a tailstrict annotated frame followed by some locals, pop them all.
     */
    public void tailCallTrimStack() {
        for (int i = this.stack.size() - 1; i >= 0; --i) {
            switch (this.stack.get(i).getKind()) {
                case FRAME_CALL: {
                    if (!this.stack.get(i).isTailCall() || this.stack.get(i).getThunks().size() > 0) {
                        return;
                    }
                    // Remove all stack frames including this one.
                    synchronized (this.stack) {
                        while (this.stack.size() > i) {
                            this.stack.remove(this.stack.size() - 1);
                        }
                    }
                    this.calls--;
                    return;
                }

                case FRAME_LOCAL: break;

                default: return;
            }
        }
    }

    /**
     * New call frame.
     */
    public void newCall(final LocationRange loc,
                        final HeapEntity context,
                        final HeapObject self,
                        final int offset,
                        final BindingFrame upValues) {
        this.tailCallTrimStack();
        if (this.calls >= this.limit) {
            throw this.makeError(loc, "Max stack frames exceeded.");
        }
        this.stack.add(new Frame(FrameKind.FRAME_CALL, loc));
        this.calls++;
        this.top().setContext(context);
        this.top().setSelf(self);
        this.top().setOffset(offset);
        this.top().setBindings(upValues);
        this.top().setTailCall(false);

        /*
#ifndef NDEBUG
        for (const auto &bind : up_values) {
            if (bind.second == nullptr) {
                std::cerr << "INTERNAL ERROR: No binding for variable "
                          << encode_utf8(bind.first->name) << std::endl;
                std::abort();
            }
        }
#endif
        */
    }

    /**
     * Look up the stack to find the self binding.
     */
    // Output in parameter
    /*
    public void getSelfBinding(HeapObject *&self, unsigned &offset) {
        self = nullptr;
        offset = 0;
        for (int i = stack.size() - 1; i >= 0; --i) {
            if (stack[i].isCall()) {
                self = stack[i].self;
                offset = stack[i].offset;
                return;
            }
        }
    }
    */

    /**
     * Look up the stack to see if we're running assertions for this object.
     */
    public boolean alreadyExecutingInvariants(final HeapObject self) {
        for (int i = this.stack.size() - 1; i >= 0; --i) {
            if (this.stack.get(i).getKind() == FrameKind.FRAME_INVARIANTS) {
                if (this.stack.get(i).getSelf() == self) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * How many call frames are on the stack.
     */
    private int calls;

    /**
     * How many call frames should be allowed before aborting the program.
     */
    private final int limit;

    /**
     * The stack frames.
     */
    private final ArrayList<Frame> stack;
}
