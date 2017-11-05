package org.jsonnet;

public class Location {
    public Location(final long line_number, final long column) {
        this.line = line_number;
        this.column = column;
    }

    public Location() {
        this(0, 0);
    }

    public boolean isSet() {
        return line != 0;
    }

    public long getLine() {
        return this.line;
    }

    public long getColumn() {
        return this.column;
    }

    @Override
    public String toString() {
        return this.line + ":" + this.column;
    }

    private final long line;
    private final long column;
}
