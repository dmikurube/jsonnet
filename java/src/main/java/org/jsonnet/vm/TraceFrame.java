package org.jsonnet.vm;

import org.jsonnet.LocationRange;

/**
 * A single line of a stack trace from a runtime error.
 */
public class TraceFrame {
    public TraceFrame(final LocationRange location, final String name) {
        this.location = location;
        this.name = name;
    }

    public TraceFrame(final LocationRange location) {
        this(location, "");
    }

    public void setName(final String name) {
        this.name = name;
    }

    private final LocationRange location;
    private String name;  // TODO: Immutable...
}
