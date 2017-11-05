package org.jsonnet.desugarer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class BuiltinDecl {
    private final String name;
    private final List<String> params;

    static final long MAX_BUILTIN = 26;

    private BuiltinDecl() {
        this.name = null;
        this.params = new ArrayList<>();
    }

    private BuiltinDecl(final String name, final String... params) {
        this.name = name;
        this.params = Collections.unmodifiableList(Arrays.asList(params));
    }

    static BuiltinDecl jsonnet_builtin_decl(final long builtin) {
        switch ((int) builtin) {
            case 0: return new BuiltinDecl("makeArray", "sz", "func");
            case 1: return new BuiltinDecl("pow", "x", "n");
            case 2: return new BuiltinDecl("floor", "x");
            case 3: return new BuiltinDecl("ceil", "x");
            case 4: return new BuiltinDecl("sqrt", "x");
            case 5: return new BuiltinDecl("sin", "x");
            case 6: return new BuiltinDecl("cos", "x");
            case 7: return new BuiltinDecl("tan", "x");
            case 8: return new BuiltinDecl("asin", "x");
            case 9: return new BuiltinDecl("acos", "x");
            case 10: return new BuiltinDecl("atan", "x");
            case 11: return new BuiltinDecl("type", "x");
            case 12: return new BuiltinDecl("filter", "func", "arr");
            case 13: return new BuiltinDecl("objectHasEx", "obj", "f", "inc_hidden");
            case 14: return new BuiltinDecl("length", "x");
            case 15: return new BuiltinDecl("objectFieldsEx", "obj", "inc_hidden");
            case 16: return new BuiltinDecl("codepoint", "str");
            case 17: return new BuiltinDecl("char", "n");
            case 18: return new BuiltinDecl("log", "n");
            case 19: return new BuiltinDecl("exp", "n");
            case 20: return new BuiltinDecl("mantissa", "n");
            case 21: return new BuiltinDecl("exponent", "n");
            case 22: return new BuiltinDecl("modulo", "a", "b");
            case 23: return new BuiltinDecl("extVar", "x");
            case 24: return new BuiltinDecl("primitiveEquals", "a", "b");
            case 25: return new BuiltinDecl("native", "name");
            case 26: return new BuiltinDecl("md5", "str");
            default:
                throw new RuntimeException("INTERNAL ERROR: Unrecognized builtin function: " + builtin);
        }
    }
}
