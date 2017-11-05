package org.jsonnet.vm;

import java.util.ArrayList;
import java.util.List;

public class HeapArray extends HeapEntity {
    public HeapArray(final ArrayList<HeapThunk> elements) {
        this.elements = elements;
    }

    public List<HeapThunk> getElements() {
        // TODO: Immutable
        return this.elements;
    }

    // It is convenient for this to not be const, so that we can add elements to it one at a
    // time after creation.  Thus, elements are not GCed as the array is being
    // created.
    private final ArrayList<HeapThunk> elements;
}
