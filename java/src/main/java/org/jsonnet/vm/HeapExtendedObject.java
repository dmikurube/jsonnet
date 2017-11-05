package org.jsonnet.vm;

/**
 * Objects created by the + construct.
 */
public class HeapExtendedObject extends HeapObject {
    public HeapExtendedObject(final HeapObject left, final HeapObject right) {
        this.left = left;
        this.right = right;
    }

    public HeapObject getLeft() {
        return this.left;
    }

    public HeapObject getRight() {
        return this.right;
    }

    /**
     * The left hand side of the construct.
     */
    private final HeapObject left;

    /**
     * The right hand side of the construct.
     */
    private final HeapObject right;
}
