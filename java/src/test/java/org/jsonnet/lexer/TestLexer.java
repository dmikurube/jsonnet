package org.jsonnet.lexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jsonnet.JsonnetStaticError;
import org.jsonnet.LocationRange;
import org.junit.Test;

public class TestLexer {
    private void testLex(final String name,
                         final String input,
                         final List<Token> tokens,
                         final String error) {
        final ArrayList<Token> test_tokens = new ArrayList<Token>(tokens);
        test_tokens.add(new Token(Token.Kind.END_OF_FILE, ""));

        try {
            final List<Token> lexed_tokens = Lexer.jsonnet_lex(name, input.getBytes());
            assertEquals(lexed_tokens, test_tokens);
        } catch (JsonnetStaticError ex) {
            assertEquals(ex.toString(), error);
        }
    }

    private void testLex(final String name,
                         final String input,
                         final String error) {
        testLex(name, input, new ArrayList<Token>(), error);
    }

    private void testLex(final String name,
                         final String input,
                         final Token token,
                         final String error) {
        final ArrayList<Token> tokens = new ArrayList<Token>();
        tokens.add(token);
        testLex(name, input, tokens, error);
    }

    private void testLex(final String name,
                         final String input,
                         final Token token1,
                         final Token token2,
                         final String error) {
        final ArrayList<Token> tokens = new ArrayList<Token>();
        tokens.add(token1);
        tokens.add(token2);
        testLex(name, input, tokens, error);
    }

    private void testLex(final String name,
                         final String input,
                         final Token token1,
                         final Token token2,
                         final Token token3,
                         final String error) {
        final ArrayList<Token> tokens = new ArrayList<Token>();
        tokens.add(token1);
        tokens.add(token2);
        tokens.add(token3);
        testLex(name, input, tokens, error);
    }

    @Test
    public void testWhitespace() {
        testLex("empty", "", "");
        testLex("whitespace", "  \t\n\r\r\n", "");
    }

    @Test
    public void testOperators() {
        testLex("brace L", "{", new Token(Token.Kind.BRACE_L), "");
        testLex("brace R", "}", new Token(Token.Kind.BRACE_R), "");
        testLex("bracket L", "[", new Token(Token.Kind.BRACKET_L), "");
        testLex("bracket R", "]", new Token(Token.Kind.BRACKET_R), "");
        testLex("colon ", ":", new Token(Token.Kind.OPERATOR, ":"), "");
        testLex("colon 2", "::", new Token(Token.Kind.OPERATOR, "::"), "");
        testLex("colon 2", ":::", new Token(Token.Kind.OPERATOR, ":::"), "");
        testLex("arrow right", "->", new Token(Token.Kind.OPERATOR, "->"), "");
        testLex("less than minus",
                "<-",
                new Token(Token.Kind.OPERATOR, "<"),
                new Token(Token.Kind.OPERATOR, "-"),
                "");
        testLex("comma", ",", new Token(Token.Kind.COMMA), "");
        testLex("dollar", "$", new Token(Token.Kind.DOLLAR), "");
        testLex("dot", ".", new Token(Token.Kind.DOT), "");
        testLex("paren L", "(", new Token(Token.Kind.PAREN_L), "");
        testLex("paren R", ")", new Token(Token.Kind.PAREN_R), "");
        testLex("semicolon", ";", new Token(Token.Kind.SEMICOLON), "");

        testLex("not 1", "!", new Token(Token.Kind.OPERATOR, "!"), "");
        testLex("not 2", "! ", new Token(Token.Kind.OPERATOR, "!"), "");
        testLex("not equal", "!=", new Token(Token.Kind.OPERATOR, "!="), "");
        testLex("tilde", "~", new Token(Token.Kind.OPERATOR, "~"), "");
        testLex("plus", "+", new Token(Token.Kind.OPERATOR, "+"), "");
        testLex("minus", "-", new Token(Token.Kind.OPERATOR, "-"), "");
    }

    @Test
    public void testMiscOperators() {
        testLex("op *", "*", new Token(Token.Kind.OPERATOR, "*"), "");
        testLex("op /", "/", new Token(Token.Kind.OPERATOR, "/"), "");
        testLex("op %", "%", new Token(Token.Kind.OPERATOR, "%"), "");
        testLex("op &", "&", new Token(Token.Kind.OPERATOR, "&"), "");
        testLex("op |", "|", new Token(Token.Kind.OPERATOR, "|"), "");
        testLex("op ^", "^", new Token(Token.Kind.OPERATOR, "^"), "");
        testLex("op =", "=", new Token(Token.Kind.OPERATOR, "="), "");
        testLex("op <", "<", new Token(Token.Kind.OPERATOR, "<"), "");
        testLex("op >", ">", new Token(Token.Kind.OPERATOR, ">"), "");
        testLex("op >==|", ">==|", new Token(Token.Kind.OPERATOR, ">==|"), "");
    }

    @Test
    public void testNumbers() {
        testLex("number 0", "0", new Token(Token.Kind.NUMBER, "0"), "");
        testLex("number 1", "1", new Token(Token.Kind.NUMBER, "1"), "");
        testLex("number 1.0", "1.0", new Token(Token.Kind.NUMBER, "1.0"), "");
        testLex("number 0.10", "0.10", new Token(Token.Kind.NUMBER, "0.10"), "");
        testLex("number 0e100", "0e100", new Token(Token.Kind.NUMBER, "0e100"), "");
        testLex("number 1e100", "1e100", new Token(Token.Kind.NUMBER, "1e100"), "");
        testLex("number 1.1e100", "1.1e100", new Token(Token.Kind.NUMBER, "1.1e100"), "");
        testLex("number 1.1e-100", "1.1e-100", new Token(Token.Kind.NUMBER, "1.1e-100"), "");
        testLex("number 1.1e+100", "1.1e+100", new Token(Token.Kind.NUMBER, "1.1e+100"), "");
        testLex("number 0100",
                "0100",
                new Token(Token.Kind.NUMBER, "0"),
                new Token(Token.Kind.NUMBER, "100"),
                "");
        testLex("number 10+10",
                "10+10",
                new Token(Token.Kind.NUMBER, "10"),
                new Token(Token.Kind.OPERATOR, "+"),
                new Token(Token.Kind.NUMBER, "10"),
                "");
        testLex("number 1.+3",
                "1.+3",
                "number 1.+3:1:1: Couldn't lex number, junk after decimal point: +");
        testLex("number 1e!", "1e!", "number 1e!:1:1: Couldn't lex number, junk after 'E': !");
        testLex("number 1e+!",
                "1e+!",
                "number 1e+!:1:1: Couldn't lex number, junk after exponent sign: !");
    }

    @Test
    public void testDoubleStrings() {
        testLex("double string \"hi\"", "\"hi\"", new Token(Token.Kind.STRING_DOUBLE, "hi"), "");
        testLex("double string \"hi nl\"", "\"hi\n\"", new Token(Token.Kind.STRING_DOUBLE, "hi\n"), "");
        testLex("double string \"hi\\\"\"",
                "\"hi\\\"\"",
                new Token(Token.Kind.STRING_DOUBLE, "hi\\\""),
                "");
        testLex("double string \"hi\\nl\"",
                "\"hi\\\n\"",
                new Token(Token.Kind.STRING_DOUBLE, "hi\\\n"),
                "");
        testLex("double string \"hi", "\"hi", "double string \"hi:1:1: Unterminated string");
    }

    @Test
    public void testSingleStrings() {
        testLex("single string 'hi'", "'hi'", new Token(Token.Kind.STRING_SINGLE, "hi"), "");
        testLex("single string 'hi nl'", "'hi\n'", new Token(Token.Kind.STRING_SINGLE, "hi\n"), "");
        testLex("single string 'hi\\''", "'hi\\''", new Token(Token.Kind.STRING_SINGLE, "hi\\'"), "");
        testLex(
            "single string 'hi\\nl'", "'hi\\\n'", new Token(Token.Kind.STRING_SINGLE, "hi\\\n"), "");
        testLex("single string 'hi", "'hi", "single string 'hi:1:1: Unterminated string");
    }

    @Test
    public void testVerbatimDoubleStrings() {
        testLex("verbatim double string @\"hi\"",
                "@\"hi\"",
                new Token(Token.Kind.VERBATIM_STRING_DOUBLE, "hi"),
                "");
        testLex("verbatim double string @\"hi nl\"",
                "@\"hi\n\"",
                new Token(Token.Kind.VERBATIM_STRING_DOUBLE, "hi\n"),
                "");
        testLex("verbatim double string @\"hi\\\"",
                "@\"hi\\\"",
                new Token(Token.Kind.VERBATIM_STRING_DOUBLE, "hi\\"),
                "");
        testLex("verbatim double string @\"hi\\\\\"",
                "@\"hi\\\\\"",
                new Token(Token.Kind.VERBATIM_STRING_DOUBLE, "hi\\\\"),
                "");
        testLex("verbatim double string @\"hi\"\"\"",
                "@\"hi\"\"\"",
                new Token(Token.Kind.VERBATIM_STRING_DOUBLE, "hi\""),
                "");
        testLex("verbatim double string @\"\"\"hi\"",
                "@\"\"\"hi\"",
                new Token(Token.Kind.VERBATIM_STRING_DOUBLE, "\"hi"),
                "");
    }

    @Test
    public void testVerbatimSingleStrings() {
        testLex("verbatim single string @'hi'",
                "@'hi'",
                new Token(Token.Kind.VERBATIM_STRING_SINGLE, "hi"),
                "");
        testLex("verbatim single string @'hi nl'",
                "@'hi\n'",
                new Token(Token.Kind.VERBATIM_STRING_SINGLE, "hi\n"),
                "");
        testLex("verbatim single string @'hi\\'",
                "@'hi\\'",
                new Token(Token.Kind.VERBATIM_STRING_SINGLE, "hi\\"),
                "");
        testLex("verbatim single string @'hi\\\\'",
                "@'hi\\\\'",
                new Token(Token.Kind.VERBATIM_STRING_SINGLE, "hi\\\\"),
                "");
        testLex("verbatim single string @'hi'''",
                "@'hi'''",
                new Token(Token.Kind.VERBATIM_STRING_SINGLE, "hi'"),
                "");
        testLex("verbatim single string @'''hi'",
                "@'''hi'",
                new Token(Token.Kind.VERBATIM_STRING_SINGLE, "'hi"),
                "");
    }

    @Test
    public void testBlockStringSpaces() {
        final String str =
            "|||\n" +
            "  test\n" +
            "    more\n" +
            "  |||\n" +
            "    foo\n" +
            "|||";
        final Token token = new Token(
            Token.Kind.STRING_BLOCK, new Fodder(), "test\n  more\n|||\n  foo\n", "  ", "", new LocationRange());
        testLex("block string spaces", str, token, "");
    }

    @Test
    public void testBlockStringTabs() {
        final String str =
            "|||\n" +
            "\ttest\n" +
            "\t  more\n" +
            "\t|||\n" +
            "\t  foo\n" +
            "|||";
        final Token token = new Token(
            Token.Kind.STRING_BLOCK, new Fodder(), "test\n  more\n|||\n  foo\n", "\t", "", new LocationRange());
        testLex("block string tabs", str, token, "");
    }

    @Test
    public void testBlockStringsMixed() {
        final String str =
            "|||\n" +
            "\t  \ttest\n" +
            "\t  \t  more\n" +
            "\t  \t|||\n" +
            "\t  \t  foo\n" +
            "|||";
        final Token token = new Token(
            Token.Kind.STRING_BLOCK, new Fodder(), "test\n  more\n|||\n  foo\n", "\t  \t", "", new LocationRange());
        testLex("block string mixed", str, token, "");
    }

    @Test
    public void testBlockStringBlanks() {
        final String str =
            "|||\n\n" +
            "  test\n\n\n" +
            "    more\n" +
            "  |||\n" +
            "    foo\n" +
            "|||";
        final Token token = new Token(
            Token.Kind.STRING_BLOCK, new Fodder(), "\ntest\n\n\n  more\n|||\n  foo\n", "  ", "", new LocationRange());
        testLex("block string blanks", str, token, "");
    }

    @Test
    public void testBlockStringBadIndent() {
        final String str =
            "|||\n" +
            "  test\n" +
            " foo\n" +
            "|||";
        testLex("block string bad indent",
                str,
                "block string bad indent:1:1: Text block not terminated with |||");
    }

    @Test
    public void testBlockStringEof() {
        final String str =
            "|||\n" +
            "  test";
        testLex("block string eof", str, "block string eof:1:1: Unexpected EOF");
    }

    @Test
    public void testBlockStringNotTerm() {
        final String str =
            "|||\n" +
            "  test\n";
        testLex("block string not term",
                str,
                "block string not term:1:1: Text block not terminated with |||");
    }

    @Test
    public void testBlockStringNoWs() {
        final String str =
            "|||\n" +
            "test\n" +
            "|||";
        testLex("block string no ws",
                str,
                "block string no ws:1:1: Text block's first line must start with" +
                " whitespace.");
    }

    @Test
    public void testKeywords() {
        testLex("assert", "assert", new Token(Token.Kind.ASSERT, "assert"), "");
        testLex("else", "else", new Token(Token.Kind.ELSE, "else"), "");
        testLex("error", "error", new Token(Token.Kind.ERROR, "error"), "");
        testLex("false", "false", new Token(Token.Kind.FALSE, "false"), "");
        testLex("for", "for", new Token(Token.Kind.FOR, "for"), "");
        testLex("function", "function", new Token(Token.Kind.FUNCTION, "function"), "");
        testLex("if", "if", new Token(Token.Kind.IF, "if"), "");
        testLex("import", "import", new Token(Token.Kind.IMPORT, "import"), "");
        testLex("importstr", "importstr", new Token(Token.Kind.IMPORTSTR, "importstr"), "");
        testLex("in", "in", new Token(Token.Kind.IN, "in"), "");
        testLex("local", "local", new Token(Token.Kind.LOCAL, "local"), "");
        testLex("null", "null", new Token(Token.Kind.NULL_LIT, "null"), "");
        testLex("self", "self", new Token(Token.Kind.SELF, "self"), "");
        testLex("super", "super", new Token(Token.Kind.SUPER, "super"), "");
        testLex("tailstrict", "tailstrict", new Token(Token.Kind.TAILSTRICT, "tailstrict"), "");
        testLex("then", "then", new Token(Token.Kind.THEN, "then"), "");
        testLex("true", "true", new Token(Token.Kind.TRUE, "true"), "");
    }

    @Test
    public void testIdentifier() {
        testLex("identifier", "foobar123", new Token(Token.Kind.IDENTIFIER, "foobar123"), "");
        testLex("identifier",
                "foo bar123",
                new Token(Token.Kind.IDENTIFIER, "foo"),
                new Token(Token.Kind.IDENTIFIER, "bar123"),
                "");
    }

    @Test
    public void testComments() {
        // TODO(dzc): Test does not look at fodder yet.
        testLex("c++ comment", "// hi", "");
        testLex("hash comment", "# hi", "");
        testLex("c comment", "/* hi */", "");
        testLex("c comment no term",
                "/* hi",
                "c comment no term:1:1: Multi-line comment has no terminating */.");
    }
}
