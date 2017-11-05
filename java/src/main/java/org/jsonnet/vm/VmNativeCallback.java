package org.jsonnet.vm;

import java.util.Collections;
import java.util.List;

public class VmNativeCallback {
    public VmNativeCallback(final JsonnetNativeCallback cb, final Object ctx, final List<String> params) {
        this.cb = cb;
        this.ctx = ctx;
        this.params = Collections.unmodifiableList(params);
    }

    private final JsonnetNativeCallback cb;
    private final Object ctx;
    private final List<String> params;
}
