package org.jsonnet.vm;

/**
 * Stack frames.
 *
 * Of these, FRAME_CALL is the most special, as it is the only frame the stack
 * trace (for errors) displays.
 */
public enum FrameKind {
    FRAME_APPLY_TARGET,          // e in e(...)
    FRAME_BINARY_LEFT,           // a in a + b
    FRAME_BINARY_RIGHT,          // b in a + b
    FRAME_BUILTIN_FILTER,        // When executing std.filter, used to hold intermediate state.
    FRAME_BUILTIN_FORCE_THUNKS,  // When forcing builtin args, holds intermediate state.
    FRAME_CALL,                  // Used any time we have switched location in user code.
    FRAME_ERROR,                 // e in error e
    FRAME_IF,                    // e in if e then a else b
    FRAME_IN_SUPER_ELEMENT,      // e in 'e in super'
    FRAME_INDEX_TARGET,          // e in e[x]
    FRAME_INDEX_INDEX,           // e in x[e]
    FRAME_INVARIANTS,            // Caches the thunks that need to be executed one at a time.
    FRAME_LOCAL,                 // Stores thunk bindings as we execute e in local ...; e
    FRAME_OBJECT,                // Stores intermediate state as we execute es in { [e]: ..., [e]: ... }
    FRAME_OBJECT_COMP_ARRAY,     // e in {f:a for x in e]
    FRAME_OBJECT_COMP_ELEMENT,   // Stores intermediate state when building object
    FRAME_STRING_CONCAT,         // Stores intermediate state while co-ercing objects
    FRAME_SUPER_INDEX,           // e in super[e]
    FRAME_UNARY;                 // e in -e
}
