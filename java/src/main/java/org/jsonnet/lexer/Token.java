package org.jsonnet.lexer;

import org.jsonnet.LocationRange;

public class Token {
    public enum Kind {
        // Symbols
        BRACE_L("\"{\""),
        BRACE_R("\"}\""),
        BRACKET_L("\"[\""),
        BRACKET_R("\"]\""),
        COMMA("\",\""),
        DOLLAR("\"$\""),
        DOT("\".\""),

        PAREN_L("\"(\""),
        PAREN_R("\")\""),
        SEMICOLON("\";\""),

        // Arbitrary length lexemes
        IDENTIFIER("IDENTIFIER"),
        NUMBER("NUMBER"),
        OPERATOR("OPERATOR"),
        STRING_DOUBLE("STRING_DOUBLE"),
        STRING_SINGLE("STRING_SINGLE"),
        STRING_BLOCK("STRING_BLOCK"),
        VERBATIM_STRING_SINGLE("VERBATIM_STRING_SINGLE"),
        VERBATIM_STRING_DOUBLE("VERBATIM_STRING_DOUBLE"),

        // Keywords
        ASSERT("assert"),
        ELSE("else"),
        ERROR("error"),
        FALSE("false"),
        FOR("for"),
        FUNCTION("function"),
        IF("if"),
        IMPORT("import"),
        IMPORTSTR("importstr"),
        IN("in"),
        LOCAL("local"),
        NULL_LIT("null"),
        TAILSTRICT("tailstrict"),
        THEN("then"),
        SELF("self"),
        SUPER("super"),
        TRUE("true"),

        // A special token that holds line/column information about the end of the file.
        END_OF_FILE("end of file");

        private Kind(final String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return this.string;
        }

        private final String string;
    }

    private final Kind kind;

    /**
     * Fodder before this token.
     */
    private final Fodder fodder;

    /**
     * Content of the token if it wasn't a keyword.
     */
    private final String data;

    /**
     * If kind == STRING_BLOCK then stores the sequence of whitespace that indented the block.
     */
    private final String stringBlockIndent;

    /**
     * If kind == STRING_BLOCK then stores the sequence of whitespace that indented the end of
     * the block.
     *
     * This is always fewer whitespace characters than in stringBlockIndent.
     */
    private final String stringBlockTermIndent;

    /*
    UString data32(void) {
        return decode_utf8(data);
    }
    */

    private final LocationRange location;

    public Token(final Kind kind,
                 final Fodder fodder,
                 final String data,
                 final String string_block_indent,
                 final String string_block_term_indent,
                 final LocationRange location) {
        this.kind = kind;
        this.fodder = fodder;
        this.data = data;
        this.stringBlockIndent = string_block_indent;
        this.stringBlockTermIndent = string_block_term_indent;
        this.location = location;
    }

    public Token(final Kind kind, final String data) {
        this(kind, null, data, null, null, null);
    }

    public Token(final Kind kind) {
        this(kind, null, "", null, null, null);
    }

    public Kind getKind() {
        return this.kind;
    }

    public Fodder getFodder() {
        return this.fodder;
    }

    public String getData() {
        return this.data;
    }

    public String getStringBlockIndent() {
        return this.stringBlockIndent;
    }

    public String getStringBlockTermIndent() {
        return this.stringBlockTermIndent;
    }

    public LocationRange getLocation() {
        return this.location;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (this.data.equals("")) {
            builder.append(this.kind.toString());
        } else if (this.kind == Kind.OPERATOR) {
            builder.append("\"").append(this.data).append("\"");
        } else {
            builder.append("(").append(this.kind.toString()).append(", \"").append(this.data).append("\")");
        }
        return builder.toString();
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof Token)) {
            return false;
        }
        final Token other = (Token) otherObject;
        if (this.kind != other.kind) {
            return false;
        }
        if (this.data == null) {
            if (other.data == null) {
                return true;
            } else {
                return false;
            }
        }
        if (!this.data.equals(other.data)) {
            return false;
        }
        return true;
    }
}
