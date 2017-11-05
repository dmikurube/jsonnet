package org.jsonnet;

public class LocationRange {
    public LocationRange(final String file, final Location begin, final Location end) {
        this.file = file;
        this.begin = begin;
        this.end = end;
    }

    private LocationRange(final String file, final Location location) {
        this(file, location, location);
    }

    /**
     * This is useful for special locations, e.g. manifestation entry point.
     */
    public LocationRange(final String msg) {
        this(msg, new Location());
    }

    public LocationRange() {
        this("");
    }

    public boolean isSet() {
        return this.begin.isSet();
    }

    public String getFile() {
        return this.file;
    }

    public Location getBegin() {
        return this.begin;
    }

    public Location getEnd() {
        return this.end;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (this.file.length() > 0) {
            builder.append(this.file);
        }
        if (this.isSet()) {
            if (this.file.length() > 0) {
                builder.append(":");
            }
            if (this.begin.getLine() == this.end.getLine()) {
                if (this.begin.getColumn() == this.end.getColumn()) {
                    builder.append(this.begin.toString());
                } else {
                    builder.append(this.begin.toString()).append("-").append(this.end.getColumn());
                }
            } else {
                builder.append("(").append(this.begin.toString()).append(")-(").append(this.end.toString()).append(")");
            }
        }
        return builder.toString();
    }

    private final String file;
    private final Location begin;
    private final Location end;
}
