package org.jsonnet.lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Whitespace and comments.
 *
 * "Fodder" (as in cannon fodder) implies this data is expendable.
 */
public class FodderElement {
    public enum Kind {
        /** The next token, paragraph, or interstitial should be on a new line.
         *
         * A single comment string is allowed, which flows before the new line.
         *
         * The LINE_END fodder specifies the indentation level and vertical spacing before whatever
         * comes next.
         */
        LINE_END,

        /** A C-style comment that begins and ends on the same line.
         *
         * If it follows a token (i.e., it is the first fodder element) then it appears after the
         * token on the same line.  If it follows another interstitial, it will also flow after it
         * on the same line.  If it follows a new line or a paragraph, it is the first thing on the
         * following line, after the blank lines and indentation specified by the previous fodder.
         *
         * There is exactly one comment string.
         */
        INTERSTITIAL,

        /** A comment consisting of at least one line.
         *
         * // and # style commes have exactly one line.  C-style comments can have more than one
         * line.
         *
         * All lines of the comment are indented according to the indentation level of the previous
         * new line / paragraph fodder.
         *
         * The PARAGRAPH fodder specifies the indentation level and vertical spacing before whatever
         * comes next.
         */
        PARAGRAPH;
    }

    private final Kind kind;

    /** How many blank lines (vertical space) before the next fodder / token. */
    private final int blanks;

    /** How far the next fodder / token should be indented. */
    private final int indent;

    /**
     * Whatever comments are part of this fodder.
     *
     * Constraints apply.  See Kind, above.
     *
     * The strings include any delimiting characters, e.g. // # and C-style comment delimiters but
     * not newline characters or indentation.
     */
    private final List<String> comment;

    public FodderElement(
        final Kind kind,
        final int blanks,
        final int indent,
        final List<String> comment)
    {
        this.kind = kind;
        this.blanks = blanks;
        this.indent = indent;
        this.comment = Collections.unmodifiableList(comment);
        // assert(kind != LINE_END || comment.size() <= 1);
        // assert(kind != INTERSTITIAL || (blanks == 0 && indent == 0 && comment.size() == 1));
        // assert(kind != PARAGRAPH || comment.size() >= 1);
    }

    public Kind getKind() {
        return this.kind;
    }

    public int getBlanks() {
        return this.blanks;
    }

    public int getIndent() {
        return this.indent;
    }

    public List<String> getComment() {
        return this.comment;
    }

    public int countNewlines() {
        switch (this.kind) {
            case INTERSTITIAL:
                return 0;
            case LINE_END:
                return 1;
            case PARAGRAPH:
                return this.comment.size() + this.blanks;
        }
        throw new RuntimeException("Unknown FodderElement kind");
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        switch (this.kind) {
        case LINE_END:
            builder.append("END(").append(this.blanks).append(", ").append(this.indent);
            if (!this.comment.isEmpty()) {
                builder.append(", ").append(this.comment.get(0));
            }
            builder.append(")");
            break;
        case INTERSTITIAL:
            builder.append("INT(").append(this.blanks).append(", ").append(this.indent).append(", ")
                .append(this.comment.get(0)).append(")");
            break;
        case PARAGRAPH:
            builder.append("PAR(").append(this.blanks).append(", ").append(this.indent).append(", ")
                .append(this.comment.get(0)).append("...)");
            break;
        }
        return builder.toString();
    }
}
