package org.jsonnet;

public class JsonnetStaticError extends Exception {
    public JsonnetStaticError(final String filename, final Location location, final String msg)
    {
        this.location = new LocationRange(filename, location, location);
        this.msg = msg;
    }

    public JsonnetStaticError(final String msg) {
        this.location = null;
        this.msg = msg;
    }

    public JsonnetStaticError(final LocationRange location, final String msg)
    {
        this.location = location;
        this.msg = msg;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (this.location != null && this.location.isSet()) {
            builder.append(this.location);
            builder.append(":");
        }
        builder.append(" ");
        builder.append(this.msg);
        return builder.toString();
    }

    private final LocationRange location;
    private final String msg;
}

/*
static inline std::ostream &operator<<(std::ostream &o, const StaticError &err)
{
    o << err.toString();
    return o;
}
*/
