package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents JSON numbers.
 */
public class LiteralNumber extends AST {
    private final double value;
    private final String originalString;

    public LiteralNumber(final LocationRange lr,
                         final Fodder open_fodder,
                         final String str)
    {
        super(lr, ASTType.AST_LITERAL_NUMBER, open_fodder);
        // TODO: Skip trailing non-numeric characters.
        // Original: value(strtod(str.c_str(), nullptr)),
        this.value = Double.valueOf(str);
        this.originalString = str;
    }
}
