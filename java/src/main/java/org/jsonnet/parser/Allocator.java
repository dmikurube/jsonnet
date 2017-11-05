package org.jsonnet.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Allocates interned Identifiers on demand.
 */
public class Allocator {
    private final Map<String, Identifier> internedIdentifiers;

    public Allocator() {
        this.internedIdentifiers = new HashMap<String, Identifier>();
    }

    /**
     * Returns interned identifiers.
     *
     * The location used in the Identifier AST is that of the first one parsed.
     */
    public Identifier makeIdentifier(final String name) {
        final Identifier it = this.internedIdentifiers.get(name);
        if (it != null) {
            return it;
        }
        final Identifier r = new Identifier(name);
        this.internedIdentifiers.put(name, r);
        return r;
    }
}
