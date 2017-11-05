package org.jsonnet.parser;

/**
 * Represents a variable / parameter / field name.
 */
public class Identifier {
    public Identifier(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(final java.lang.Object otherObject) {
        if (!(otherObject instanceof Identifier)) {
            return false;
        }
        final Identifier other = (Identifier)otherObject;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return ("Id:" + this.name).hashCode();
    }

    public String getName() {
        return this.name;
    }

    private final String name;
}
