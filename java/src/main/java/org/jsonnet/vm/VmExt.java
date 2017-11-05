package org.jsonnet.vm;

/**
 * Stores external values / code.
 */
public class VmExt {
    public VmExt(final String data, final boolean isCode) {
        this.data = data;
        this.isCode = isCode;
    }

    public VmExt() {
        this(null, false);
    }

    private final String data;
    private final boolean isCode;
}
