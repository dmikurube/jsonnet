package org.jsonnet;

import java.util.Collections;
import java.util.List;
import org.jsonnet.vm.TraceFrame;

/**
 * Exception that is thrown by the interpreter when it reaches an error construct, or divide by
 * zero, array bounds error, dynamic type error, etc.
 */
public class JsonnetRuntimeError extends RuntimeException {
    public JsonnetRuntimeError(final List<TraceFrame> stackTrace, final String msg) {
        this.stackTrace = Collections.unmodifiableList(stackTrace);
        this.msg = msg;
    }

    private final List<TraceFrame> stackTrace;
    private final String msg;
}
