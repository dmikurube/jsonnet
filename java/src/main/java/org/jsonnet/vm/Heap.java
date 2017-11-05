package org.jsonnet.vm;

import java.util.ArrayList;

/**
 * The heap does memory management, i.e. garbage collection.
 */
public class Heap {
    public Heap(int gcTuneMinObjects, double gcTuneGrowthTrigger) {
        this.gcTuneMinObjects = gcTuneMinObjects;
        this.gcTuneGrowthTrigger = gcTuneGrowthTrigger;
        this.lastMark = 0;
        this.lastNumEntities = 0;
        this.numEntities = 0;
    }

    /*
    ~Heap()
    {
        // Nothing is marked, everything will be collected.
        sweep();
    }
    */

    /**
     * Garbage collection: Mark v, and entities reachable from v.
     */
    public void markFrom(final Value v) {
        if (v.isHeap()) {
            markFrom(v.getValueHeapEntity());
        }
    }

    private static class State {
        private State(final HeapEntity ent) {
            this.ent = ent;
            this.children = new ArrayList<>();
        }
        private final HeapEntity ent;
        private final ArrayList<HeapEntity> children;
    }

    /**
     * Garbage collection: Mark heap entities reachable from the given heap entity.
     */
    final void markFrom(final HeapEntity from) {
        assert(from != null);
        // /**
        // * Mark & sweep: advanced by 1 each GC cycle.
        // */
        // typedef unsigned char GarbageCollectionMark;
        // const GarbageCollectionMark thisMark = lastMark + 1;
        final int thisMark = lastMark + 1;
        /*
        struct State {
            HeapEntity *ent;
            std::vector<HeapEntity *> children;
            State(HeapEntity *ent) : ent(ent) {}
        };

        std::vector<State> stack;
        stack.emplace_back(from);

        while (stack.size() > 0) {
            size_t curr_index = stack.size() - 1;
            State &s = stack[curr_index];
            HeapEntity *curr = s.ent;
            if (curr->mark != thisMark) {
                curr->mark = thisMark;

                if (auto *obj = dynamic_cast<HeapSimpleObject *>(curr)) {
                    for (auto upv : obj->upValues)
                        addIfHeapEntity(upv.second, s.children);

                } else if (auto *obj = dynamic_cast<HeapExtendedObject *>(curr)) {
                    addIfHeapEntity(obj->left, s.children);
                    addIfHeapEntity(obj->right, s.children);

                } else if (auto *obj = dynamic_cast<HeapComprehensionObject *>(curr)) {
                    for (auto upv : obj->upValues)
                        addIfHeapEntity(upv.second, s.children);
                    for (auto upv : obj->compValues)
                        addIfHeapEntity(upv.second, s.children);

                } else if (auto *arr = dynamic_cast<HeapArray *>(curr)) {
                    for (auto el : arr->elements)
                        addIfHeapEntity(el, s.children);

                } else if (auto *func = dynamic_cast<HeapClosure *>(curr)) {
                    for (auto upv : func->upValues)
                        addIfHeapEntity(upv.second, s.children);
                    if (func->self)
                        addIfHeapEntity(func->self, s.children);

                } else if (auto *thunk = dynamic_cast<HeapThunk *>(curr)) {
                    if (thunk->filled) {
                        if (thunk->content.isHeap())
                            addIfHeapEntity(thunk->content.v.h, s.children);
                    } else {
                        for (auto upv : thunk->upValues)
                            addIfHeapEntity(upv.second, s.children);
                        if (thunk->self)
                            addIfHeapEntity(thunk->self, s.children);
                    }
                }
            }

            if (s.children.size() > 0) {
                HeapEntity *next = s.children[s.children.size() - 1];
                s.children.pop_back();
                stack.emplace_back(next);  // CAUTION: s invalidated here
            } else {
                stack.pop_back();  // CAUTION: s invalidated here
            }
        }
        */
    }

    /**
     * Delete everything that was not marked since the last collection
     */
    void sweep() {
        /*
        lastMark++;
        // Heap shrinks during this loop.  Do not cache entities.size().
        for (unsigned long i = 0; i < entities.size(); ++i) {
            HeapEntity *x = entities[i];
            if (x->mark != lastMark) {
                delete x;
                if (i != entities.size() - 1) {
                    // Swap it with the back.
                    entities[i] = entities[entities.size() - 1];
                }
                entities.pop_back();
                --i;
            }
        }
        lastNumEntities = numEntities = entities.size();
        */
    }

    /**
     * Is it time to initiate a GC cycle?
     */
    public boolean checkHeap() {
        return this.numEntities > this.gcTuneMinObjects &&
               this.numEntities > this.gcTuneGrowthTrigger * this.lastNumEntities;
    }

    /**
     * Allocate a heap entity.
     *
     * If the heap is large enough (\see gcTuneMinObjects) and has grown by enough since the
     * last collection cycle (\see gcTuneGrowthTrigger), a collection cycle is performed.
     */
    /*
    template <class T, class... Args>
    T *makeEntity(Args &&... args)
    {
        T *r = new T(std::forward<Args>(args)...);
        entities.push_back(r);
        r->mark = lastMark;
        numEntities = entities.size();
        return r;
    }
    */

    /**
     * How many objects must exist in the heap before we bother doing garbage collection?
     */
    private final int gcTuneMinObjects;

    /**
     * How much must the heap have grown since the last cycle to trigger a collection?
     */
    private final double gcTuneGrowthTrigger;

    /**
     * Value used to mark entities at the last garbage collection cycle.
     */
    // /**
    // * Mark & sweep: advanced by 1 each GC cycle.
    // */
    // typedef unsigned char GarbageCollectionMark;
    // private GarbageCollectionMark lastMark;
    private int lastMark;

    /**
     * The heap entities (strings, arrays, objects, functions, etc).
     *
     * Not all may be reachable, all should have o->mark == this->lastMark.  Entities are
     * removed from the heap via O(1) swap with last element, so the ordering of entities is
     * arbitrary and changes every garbage collection cycle.
     */
    private ArrayList<HeapEntity> entities;

    /**
     * The number of heap entities at the last garbage collection cycle.
     */
    private long lastNumEntities;

    /**
     * The number of heap entities now.
     */
    private long numEntities;

    /**
     * Add the HeapEntity inside v to vec, if the value exists on the heap.
     */
    private void addIfHeapEntity(final Value v, final ArrayList<HeapEntity> vec) {
        if (v.isHeap()) {
            vec.add(v.getValueHeapEntity());
        }
    }

    /**
     * Add the HeapEntity inside v to vec, if the value exists on the heap.
     */
    private void addIfHeapEntity(final HeapEntity v, final ArrayList<HeapEntity> vec) {
        vec.add(v);
    }
}
