package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents JSON strings.
 */
public class LiteralString extends AST {
    private final String value;
    public enum TokenKind { SINGLE, DOUBLE, BLOCK, VERBATIM_SINGLE, VERBATIM_DOUBLE }
    private final TokenKind tokenKind;
    private final String blockIndent;      // Only contains ' ' and '\t'.
    private final String blockTermIndent;  // Only contains ' ' and '\t'.

    public LiteralString(final LocationRange lr,
                         final Fodder open_fodder,
                         final String value,
                         final TokenKind token_kind,
                         final String block_indent,
                         final String block_term_indent) {
        super(lr, ASTType.AST_LITERAL_STRING, open_fodder);
        this.value = value;
        this.tokenKind = token_kind;
        this.blockIndent = block_indent;
        this.blockTermIndent = block_term_indent;
    }

    public String getValue() {
        return this.value;
    }

    public TokenKind getTokenKind() {
        return this.tokenKind;
    }
}
