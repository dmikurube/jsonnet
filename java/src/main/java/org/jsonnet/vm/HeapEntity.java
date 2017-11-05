package org.jsonnet.vm;

/**
 * Supertype of everything that is allocated on the heap.
 */
public class HeapEntity {
    protected byte getMark() {
        return this.mark;
    }

    protected void setMark(byte mark) {
        this.mark = mark;
    }

    // /**
    // * Mark & sweep: advanced by 1 each GC cycle.
    // */
    // typedef unsigned char GarbageCollectionMark;
    // protected GarbageCollectionMark mark;
    private byte mark;
}
