package org.jsonnet.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import org.jsonnet.JsonnetStaticError;
import org.jsonnet.lexer.Lexer;
import org.jsonnet.lexer.Tokens;
import org.junit.Test;

public class TestParser {
    // Checks whether the provided snippet parses successfully.
    // TODO(dzc): Update this test to check the parsed AST against an expected AST.
    private void testParse(final String snippet) throws JsonnetStaticError {
        try {
            Tokens tokens = Lexer.jsonnet_lex("test", snippet.getBytes(StandardCharsets.UTF_8));
            Allocator allocator = new Allocator();
            AST ast = Parser.jsonnet_parse(allocator, tokens);
            // (void)ast;
        } catch (JsonnetStaticError ex) {
            System.err.println("Static error: " + ex.toString());
            System.err.println("Snippet:");
            System.err.println(snippet);
            throw ex;
        }
    }

    @Test
    public void testLiterals() throws JsonnetStaticError {
        testParse("true");
        testParse("1");
        testParse("1.2e3");
        testParse("!true");
        testParse("null");
        testParse("\"world\"");
        testParse("\"world\"");
        testParse("|||\n   world\n|||");
    }

    @Test
    public void testExpressions() throws JsonnetStaticError {
        testParse("$.foo.bar");
        testParse("self.foo.bar");
        testParse("super.foo.bar");
        testParse("super[1]");
        testParse("error \"Error!\"");

        testParse("foo(bar)");
        testParse("foo(bar) tailstrict");
        testParse("foo.bar");
        testParse("foo[bar]");

        testParse("true || false");
        testParse("0 && 1 || 0");
        testParse("0 && (1 || 0)");
    }

    @Test
    public void testLocal() throws JsonnetStaticError {
        testParse("(local foo = \"bar\"; foo)");
        testParse("local foo(bar) = bar; foo(1)");
        testParse("({ local foo = \"bar\", baz: 1})");
        testParse("{ local foo(bar) = bar, baz: foo(1)}");
    }

    @Test
    public void testArray() throws JsonnetStaticError {
        testParse("[]");
        testParse("[a, b, c]");
        testParse("[x for x in [1,2,3] ]");
        testParse("[x for x in [1,2,3] if x <= 2]");
        testParse("[x+y for x in [1,2,3] if x <= 2 for y in [4, 5, 6]]");
    }

    @Test
    public void testTuple() throws JsonnetStaticError {
        testParse("{ foo(bar, baz): bar+baz }");

        testParse("{ [\"foo\" + \"bar\"]: 3 }");
        testParse("{ [\"field\" + x]: x for x in [1, 2, 3] }");
        testParse("{ local y = x, [\"field\" + x]: x for x in [1, 2, 3] }");
        testParse("{ [\"field\" + x]: x for x in [1, 2, 3] if x <= 2 }");
        testParse("{ [\"field\" + x + y]:" +
                  " x + y for x in [1, 2, 3] if x <= 2 for y in [4, 5, 6]}");

        testParse("{}");
        testParse("{ hello: \"world\" }");
        testParse("{ hello +: \"world\" }");
        testParse("{\n" +
                  "  hello: \"world\",\n" +
                  "  \"name\":: joe,\n" +
                  "  'mood'::: \"happy\",\n" +
                  "  |||\n" +
                  "    key type\n" +
                  "|||: \"block\",\n" +
                  "}");

        testParse("assert true: 'woah!'; true");
        testParse("{ assert true: 'woah!', foo: bar }");

        testParse("if n > 1 then 'foos' else 'foo'");

        testParse("local foo = function(x) x + 1; true");

        testParse("import 'foo.jsonnet'");
        testParse("importstr 'foo.text'");

        testParse("{a: b} + {c: d}");
        testParse("{a: b}{c: d}");
    }

    void testParseError(final String snippet, final String expectedError) throws JsonnetStaticError {
        try {
            final Tokens tokens = Lexer.jsonnet_lex("test", snippet.getBytes(StandardCharsets.UTF_8));
            final Allocator allocator = new Allocator();
            final AST ast = Parser.jsonnet_parse(allocator, tokens);
            // (void)ast;
        } catch (JsonnetStaticError ex) {
            System.err.println("Snippet:");
            System.err.println(snippet);
            if (!ex.toString().equals(expectedError)) {
                throw ex;
            }
        }
    }

    @Test
    public void testInvalidFunctionCall() throws JsonnetStaticError {
        testParseError("function(a, b c)",
                       "test:1:15: Expected a comma before next function parameter.");
        testParseError("function(a, 1)", "test:1:13: Could not parse parameter here.");
        testParseError("a b", "test:1:3: Did not expect: (IDENTIFIER, \"b\")");
        testParseError("foo(a, bar(a b))",
                       "test:1:14: Expected a comma before next function argument.");
    }

    @Test
    public void testInvalidLocal() throws JsonnetStaticError {
        testParseError("local", "test:1:6: Expected token IDENTIFIER but got end of file");
        testParseError("local foo = 1, foo = 2; true", "test:1:16-18: Duplicate local var: foo");
        testParseError("local foo(a b) = a; true",
                       "test:1:13: Expected a comma before next function parameter.");
        testParseError("local foo(a): a; true", "test:1:13: Expected operator = but got :");
        testParseError("local foo(a) = bar(a b); true",
                       "test:1:22: Expected a comma before next function argument.");
        testParseError("local foo: 1; true", "test:1:10: Expected operator = but got :");
        testParseError("local foo = bar(a b); true",
                       "test:1:19: Expected a comma before next function argument.");

        testParseError("local a = b ()", "test:1:15: Expected , or ; but got end of file");
        testParseError("local a = b; (a b)",
                       "test:1:17: Expected token \")\" but got (IDENTIFIER, \"b\")");
    }

    @Test
    public void testInvalidTuple() throws JsonnetStaticError {
        testParseError("{a b}",
                       "test:1:4: Expected token OPERATOR but got (IDENTIFIER, \"b\")");
        testParseError("{a = b}", "test:1:2: Expected one of :, ::, :::, +:, +::, +:::, got: =");
        testParseError("{a :::: b}", "test:1:2: Expected one of :, ::, :::, +:, +::, +:::, got: ::::");
    }

    @Test
    public void testInvalidComprehension() throws JsonnetStaticError {
        testParseError("{assert x for x in [1, 2, 3]}",
                       "test:1:11-13: Object comprehension cannot have asserts.");
        testParseError("{['foo' + x]: true, [x]: x for x in [1, 2, 3]}",
                       "test:1:28-30: Object comprehension can only have one field.");
        testParseError("{foo: x for x in [1, 2, 3]}",
                       "test:1:9-11: Object comprehensions can only have [e] fields.");
        testParseError("{[x]:: true for x in [1, 2, 3]}",
                       "test:1:13-15: Object comprehensions cannot have hidden fields.");
        testParseError("{[x]: true for 1 in [1, 2, 3]}",
                       "test:1:16: Expected token IDENTIFIER but got (NUMBER, \"1\")");
        testParseError("{[x]: true for x at [1, 2, 3]}",
                       "test:1:18-19: Expected token in but got (IDENTIFIER, \"at\")");
        testParseError("{[x]: true for x in [1, 2 3]}",
                       "test:1:27: Expected a comma before next array element.");
        testParseError("{[x]: true for x in [1, 2, 3] if (a b)}",
                       "test:1:37: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("{[x]: true for x in [1, 2, 3] if a b}",
                       "test:1:36: Expected for, if or \"}\" after for clause," +
                       " got: (IDENTIFIER, \"b\")");
    }

    @Test
    public void testInvalidNoComma() throws JsonnetStaticError {
        testParseError("{a: b c:d}", "test:1:7: Expected a comma before next field.");
    }

    @Test
    public void testInvalidArrayKey() throws JsonnetStaticError {
        testParseError("{[(x y)]: z}", "test:1:6: Expected token \")\" but got (IDENTIFIER, \"y\")");
        testParseError("{[x y]: z}", "test:1:5: Expected token \"]\" but got (IDENTIFIER, \"y\")");
    }

    @Test
    public void testInvalidFields() throws JsonnetStaticError {
        testParseError("{foo(x y): z}", "test:1:8: Expected a comma before next method parameter.");
        testParseError("{foo(x)+: z}", "test:1:2-4: Cannot use +: syntax sugar in a method: foo");
        testParseError("{foo: 1, foo: 2}", "test:1:10-12: Duplicate field: foo");
        testParseError("{foo: (1 2)}", "test:1:10: Expected token \")\" but got (NUMBER, \"2\")");
    }

    @Test
    public void testInvalidLocalInTuple() throws JsonnetStaticError {
        testParseError("{local 1 = 3, true}",
                       "test:1:8: Expected token IDENTIFIER but got (NUMBER, \"1\")");
        testParseError("{local foo = 1, local foo = 2, true}",
                       "test:1:23-25: Duplicate local var: foo");
        testParseError("{local foo(a b) = 1, a: true}",
                       "test:1:14: Expected a comma before next function parameter.");
        testParseError("{local foo(a): 1, a: true}", "test:1:14: Expected operator = but got :");
        testParseError("{local foo(a) = (a b), a: true}",
                       "test:1:20: Expected token \")\" but got (IDENTIFIER, \"b\")");
    }

    @Test
    public void testInvalidAssertInTuple() throws JsonnetStaticError {
        testParseError("{assert (a b), a: true}",
                       "test:1:12: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("{assert a: (a b), a: true}",
                       "test:1:15: Expected token \")\" but got (IDENTIFIER, \"b\")");
    }

    @Test
    public void testInvalidUnexpectedFunction() throws JsonnetStaticError {
        // TODO(jsonnet-team): The following error output differs from the Go
        // implementation, which is:
        // test:1:2-10 Unexpected: (function, "function") while parsing field
        // definition.
        testParseError("{function(a, b) a+b: true}",
                       "test:1:2-9: Unexpected: function while parsing field definition");
    }

    @Test
    public void testInvalidArray() throws JsonnetStaticError {
    testParseError("[(a b), 2, 3]",
                   "test:1:5: Expected token \")\" but got (IDENTIFIER, \"b\")");
    testParseError("[1, (a b), 2, 3]",
                   "test:1:8: Expected token \")\" but got (IDENTIFIER, \"b\")");
    testParseError("[a for b in [1 2 3]]",
                   "test:1:16: Expected a comma before next array element.");
    }

    @Test
    public void testInvalidExpression() throws JsonnetStaticError {
        // TODO(jsonnet-team): The error output of the following differs from the Go
        // implementation, which is:
        // test:1:1-4 Unexpected: (for, "for") while parsing terminal)
        testParseError("for", "test:1:1-3: Unexpected: for while parsing terminal");
        testParseError("", "test:1:1: Unexpected end of file.");
        testParseError("((a b))", "test:1:5: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("a.1", "test:1:3: Expected token IDENTIFIER but got (NUMBER, \"1\")");
        testParseError("super.1", "test:1:7: Expected token IDENTIFIER but got (NUMBER, \"1\")");
        testParseError("super[(a b)]", "test:1:10: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("super[a b]", "test:1:9: Expected token \"]\" but got (IDENTIFIER, \"b\")");
        testParseError("super", "test:1:1-5: Expected . or [ after super.");
    }

    @Test
    public void testInvalidAssert() throws JsonnetStaticError {
        testParseError("assert (a b); true",
                       "test:1:11: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("assert a: (a b); true",
                       "test:1:14: Expected token \")\" but got (IDENTIFIER, \"b\")");
        // TODO(jsonnet-team): The error output of this differs from the Go
        // implementation, which is:
        // test:1:16: Expected token ";" but got (",", ",")
        testParseError("assert a: 'foo', true",
                       "test:1:16: Expected token \";\" but got \",\"");
        testParseError("assert a: 'foo'; (a b)",
                       "test:1:21: Expected token \")\" but got (IDENTIFIER, \"b\")");
    }

    @Test
    public void testInvalidError() throws JsonnetStaticError {
        testParseError("error (a b)", "test:1:10: Expected token \")\" but got (IDENTIFIER, \"b\")");
    }

    @Test
    public void testInvalidIf() throws JsonnetStaticError {
        testParseError("if (a b) then c",
                       "test:1:7: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("if a b c",
                       "test:1:6: Expected token then but got (IDENTIFIER, \"b\")");
        testParseError("if a then (b c)",
                       "test:1:14: Expected token \")\" but got (IDENTIFIER, \"c\")");
        testParseError("if a then b else (c d)",
                       "test:1:21: Expected token \")\" but got (IDENTIFIER, \"d\")");
    }

    @Test
    public void testInvalidFunction() throws JsonnetStaticError {
        testParseError("function(a) (a b)",
                       "test:1:16: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("function a a", "test:1:10: Expected ( but got (IDENTIFIER, \"a\")");
    }

    @Test
    public void testInvalidImport() throws JsonnetStaticError {
        testParseError("import (a b)", "test:1:11: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("import (a+b)", "test:1:8-12: Computed imports are not allowed.");
        testParseError("importstr (a b)",
                       "test:1:14: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("importstr (a+b)", "test:1:11-15: Computed imports are not allowed.");
    }

    @Test
    public void testInvalidOperator() throws JsonnetStaticError {
        testParseError("1+ <<", "test:1:4-5: Not a unary operator: <<");
        testParseError("-(a b)", "test:1:5: Expected token \")\" but got (IDENTIFIER, \"b\")");
        testParseError("1~2", "test:1:2: Not a binary operator: ~");
    }

    @Test
    public void testInvalidArrayAccess() throws JsonnetStaticError {
        testParseError("a[(b c)]", "test:1:6: Expected token \")\" but got (IDENTIFIER, \"c\")");
        // TODO(jsonnet-team): The error output of this differs from the Go
        // implementation, which is:
        // test:1:5: Expected token "]" but got (IDENTIFIER, "c")
        testParseError("a[b c]", "test:1:5: Unexpected: IDENTIFIER while parsing slice");
    }

    @Test
    public void testInvalidOverride() throws JsonnetStaticError {
        testParseError("a{b c}", "test:1:5: Expected token OPERATOR but got (IDENTIFIER, \"c\")");
    }
}
