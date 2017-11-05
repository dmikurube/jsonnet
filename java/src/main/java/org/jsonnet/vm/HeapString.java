package org.jsonnet.vm;

/**
 * Stores a simple string on the heap.
 */
public class HeapString extends HeapEntity {
    public HeapString(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    private final String value;
}
