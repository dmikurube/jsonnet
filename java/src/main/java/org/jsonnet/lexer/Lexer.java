package org.jsonnet.lexer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jsonnet.JsonnetStaticError;
import org.jsonnet.Location;
import org.jsonnet.LocationRange;

public class Lexer {
    private Lexer() {
        // Do not instantiate
    }

    /**
     * IF the given identifier is a keyword, return its kind, otherwise return IDENTIFIER.
     */
    public static Token.Kind lex_get_keyword_kind(final String identifier) {
        final Token.Kind it = keywords.get(identifier);
        if (it == null) {
            return Token.Kind.IDENTIFIER;
        }
        return it;
    }

    public static Tokens jsonnet_lex(final String filename, final byte[] input) throws JsonnetStaticError {
        final LexerContext c = new LexerContext(input);
        c.line_number = 1;
        c.line_start = 0;

        Tokens r = new Tokens();

        c.index = 0;

        Fodder fodder = new Fodder();
        boolean fresh_line = true;  // Are we tokenizing from the beginning of a new line?

        while (c.in()) {
            // Used to ensure we have actually advanced the pointer by the end of the iteration.
            final int original_index = c.index;

            Token.Kind kind;
            StringBuilder data = new StringBuilder();
            String string_block_indent = "";
            StringBuilder string_block_term_indent = new StringBuilder();

            c.indent = 0;
            c.new_lines = 0;
            lex_ws(c);

            // If it's the end of the file, discard final whitespace.
            if (c.out()) {
                break;
            }

            if (c.new_lines > 0) {
                // Otherwise store whitespace in fodder.
                int blanks = c.new_lines - 1;
                fodder.add(new FodderElement(FodderElement.Kind.LINE_END, blanks, c.indent, EMPTY));
                fresh_line = true;
            }

            final Location begin = new Location(c.line_number, c.index - c.line_start + 1);

            switch (c.cur()) {
                // The following operators should never be combined with subsequent symbols.
                case '{':
                    kind = Token.Kind.BRACE_L;
                    c.inc();
                    break;

                case '}':
                    kind = Token.Kind.BRACE_R;
                    c.inc();
                    break;

                case '[':
                    kind = Token.Kind.BRACKET_L;
                    c.inc();
                    break;

                case ']':
                    kind = Token.Kind.BRACKET_R;
                    c.inc();
                    break;

                case ',':
                    kind = Token.Kind.COMMA;
                    c.inc();
                    break;

                case '.':
                    kind = Token.Kind.DOT;
                    c.inc();
                    break;

                case '(':
                    kind = Token.Kind.PAREN_L;
                    c.inc();
                    break;

                case ')':
                    kind = Token.Kind.PAREN_R;
                    c.inc();
                    break;

                case ';':
                    kind = Token.Kind.SEMICOLON;
                    c.inc();
                    break;

                // Numeric literals.
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    kind = Token.Kind.NUMBER;
                    data = lex_number(c, filename, begin);
                    break;

                // UString literals.
                case '"': {
                    c.inc();
                    for (;; c.inc()) {
                        if (c.out()) {
                            throw new JsonnetStaticError(filename, begin, "Unterminated string");
                        }
                        if (c.cur() == '"') {
                            break;
                        }
                        if (c.cur() == '\\' && c.inNext()) {
                            data.append(c.cur());
                            c.inc();
                        }
                        if (c.cur() == '\n') {
                            // Maintain line/column counters.
                            c.line_number++;
                            c.line_start = c.index + 1;
                        }
                        data.append(c.cur());
                    }
                    c.inc();  // Advance beyond the ".
                    kind = Token.Kind.STRING_DOUBLE;
                } break;

                // UString literals.
                case '\'': {
                    c.inc();
                    for (;; c.inc()) {
                        if (c.out()) {
                            throw new JsonnetStaticError(filename, begin, "Unterminated string");
                        }
                        if (c.cur() == '\'') {
                            break;
                        }
                        if (c.cur() == '\\' && c.inNext()) {
                            data.append(c.cur());
                            c.inc();
                        }
                        if (c.cur() == '\n') {
                            // Maintain line/column counters.
                            c.line_number++;
                            c.line_start = c.index + 1;
                        }
                        data.append(c.cur());
                    }
                    c.inc();  // Advance beyond the '.
                    kind = Token.Kind.STRING_SINGLE;
                } break;

                // Verbatim string literals.
                // ' and " quoting is interpreted here, unlike non-verbatim strings
                // where it is done later by jsonnet_string_unescape.  This is OK
                // in this case because no information is lost by resoving the
                // repeated quote into a single quote, so we can go back to the
                // original form in the formatter.
                case '@': {
                    c.inc();
                    if (c.cur() != '"' && c.cur() != '\'') {
                        throw new JsonnetStaticError(filename, begin,
                                                     "Couldn't lex verbatim string, junk after '@': " + c.cur());
                    }
                    final char quot = c.cur();
                    c.inc();  // Advance beyond the opening quote.
                    for (;; c.inc()) {
                        if (c.out()) {
                            throw new JsonnetStaticError(filename, begin, "Unterminated verbatim string");
                        }
                        if (c.cur() == quot) {
                            if (c.next() == quot) {
                                c.inc();
                            } else {
                                break;
                            }
                        }
                        data.append(c.cur());
                    }
                    c.inc();  // Advance beyond the closing quote.
                    if (quot == '"') {
                        kind = Token.Kind.VERBATIM_STRING_DOUBLE;
                    } else {
                        kind = Token.Kind.VERBATIM_STRING_SINGLE;
                    }
                } break;

                // Keywords
                default:
                    if (is_identifier_first(c.cur())) {
                        final StringBuilder id = new StringBuilder();
                        for (; is_identifier(c.cur()); c.inc()) {
                            id.append(c.cur());
                        }
                        kind = lex_get_keyword_kind(id.toString());
                        data = id;

                    } else if (is_symbol(c.cur()) || c.cur() == '#') {
                        // Single line C++ and Python style comments.
                        if (c.cur() == '#' || (c.cur() == '/' && c.next() == '/')) {
                            final ArrayList<String> comment = new ArrayList<String>();
                            comment.add(lex_until_newline(c));
                            FodderElement.Kind fkind =
                                fresh_line ? FodderElement.Kind.PARAGRAPH : FodderElement.Kind.LINE_END;
                            fodder.add(new FodderElement(fkind, c.blanks, c.indent, comment));
                            fresh_line = true;
                            continue;  // We've not got a token, just fodder, so keep scanning.
                        }

                        // Multi-line C style comment.
                        if (c.cur() == '/' && c.next() == '*') {
                            int margin = c.index - c.line_start;

                            int initial_index = c.index;
                            c.inc(2);  // Avoid matching /*/: skip the /* before starting the search for
                                       // */.

                            while (!(c.cur() == '*' && c.next() == '/')) {
                                if (c.out()) {
                                    throw new JsonnetStaticError(filename, begin,
                                                                 "Multi-line comment has no terminating */.");
                                }
                                if (c.cur() == '\n') {
                                    // Just keep track of the line / column counters.
                                    c.line_number++;
                                    c.line_start = c.index + 1;
                                }
                                c.inc();
                            }
                            c.inc(2);  // Move the pointer to the char after the closing '/'.

                            final String comment = c.substring(initial_index,
                                                               c.index);  // Includes the "/*" and "*/".
                            // Lex whitespace after comment
                            final LexerContext new_c = c.clone();
                            lex_ws(new_c);
                            int new_lines_after = new_c.new_lines;
                            int indent_after = new_c.indent;
                            ArrayList<String> lines = new ArrayList<String>();
                            if (comment.indexOf('\n') < 0) {
                                // Comment looks like /* foo */
                                lines.add(comment);
                                fodder.add(new FodderElement(FodderElement.Kind.INTERSTITIAL, 0, 0, lines));
                                if (new_lines_after > 0) {
                                    fodder.add(new FodderElement(FodderElement.Kind.LINE_END,
                                                                 new_lines_after - 1,
                                                                 indent_after,
                                                                 EMPTY));
                                    fresh_line = true;
                                }
                            } else {
                                lines = line_split(comment, margin);
                                assert(lines.get(0).charAt(0) == '/');
                                // Little hack to support PARAGRAPHs with * down the LHS:
                                // Add a space to lines that start with a '*'
                                boolean all_star = true;
                                for (final String l : lines) {
                                    if (l.charAt(0) != '*') {
                                        all_star = false;
                                    }
                                }
                                if (all_star) {
                                    for (int i = 0; i < lines.size(); ++i) {
                                        if (lines.get(i).charAt(0) == '*') {
                                            lines.set(i, " " + lines.get(i));
                                        }
                                    }
                                }
                                if (new_lines_after == 0) {
                                    // Ensure a line end after the paragraph.
                                    new_lines_after = 1;
                                    indent_after = 0;
                                }
                                fodder.fodder_push_back(new FodderElement(FodderElement.Kind.PARAGRAPH,
                                                                          new_lines_after - 1,
                                                                          indent_after,
                                                                          lines));
                                fresh_line = true;
                            }
                            continue;  // We've not got a token, just fodder, so keep scanning.
                        }

                        // Text block
                        if (c.cur() == '|' && c.next() == '|' && c.next(2) == '|') {
                            if (c.next(3) != '\n') {
                                throw new JsonnetStaticError(filename, begin,
                                                             "Text block syntax requires new line after |||.");
                            }
                            final StringBuilder block = new StringBuilder();
                            c.inc(4);  // Skip the "|||\n"
                            c.line_number++;
                            // Skip any blank lines at the beginning of the block.
                            while (c.cur() == '\n') {
                                c.line_number++;
                                c.inc();
                                block.append('\n');
                            }
                            c.line_start = c.index;
                            int first_line = c.index;
                            int ws_chars = whitespace_check(c, first_line, c.index);
                            string_block_indent = c.substring(first_line, first_line + ws_chars);
                            if (ws_chars == 0) {
                                throw new JsonnetStaticError(filename, begin,
                                                             "Text block's first line must start with whitespace.");
                            }
                            while (true) {
                                assert(ws_chars > 0);
                                // Read up to the \n
                                for (c.index += ws_chars; c.cur() != '\n'; c.inc()) {
                                    if (c.out()) {
                                        throw new JsonnetStaticError(filename, begin, "Unexpected EOF");
                                    }
                                    block.append(c.cur());
                                }
                                // Add the \n
                                block.append('\n');
                                c.inc();
                                c.line_number++;
                                c.line_start = c.index;
                                // Skip any blank lines
                                while (c.cur() == '\n') {
                                    c.line_number++;
                                    c.inc();
                                    block.append('\n');
                                }
                                // Examine next line
                                ws_chars = whitespace_check(c, first_line, c.index);
                                if (ws_chars == 0) {
                                    // End of text block
                                    // Skip over any whitespace
                                    while (c.cur() == ' ' || c.cur() == '\t') {
                                        string_block_term_indent.append(c.cur());
                                        c.inc();
                                    }
                                    // Expect |||
                                    if (!(c.cur() == '|' && c.next() == '|' && c.next(2) == '|')) {
                                        throw new JsonnetStaticError(filename, begin,
                                                                     "Text block not terminated with |||");
                                    }
                                    c.inc(3);  // Leave after the last |
                                    data = block;
                                    kind = Token.Kind.STRING_BLOCK;
                                    break;  // Out of the while loop.
                                }
                            }

                            break;  // Out of the switch.
                        }

                        int operator_begin = c.index;
                        for (; is_symbol(c.cur()); c.inc()) {
                            // Not allowed // in operators
                            if (c.cur() == '/' && c.next() == '/') {
                                break;
                            }
                            // Not allowed /* in operators
                            if (c.cur() == '/' && c.next() == '*') {
                                break;
                            }
                            // Not allowed ||| in operators
                            if (c.cur() == '|' && c.next() == '|' && c.next(2) == '|') {
                                break;
                            }
                        }
                        // Not allowed to end with a + - ~ ! unless a single char.
                        // So, wind it back if we need to (but not too far).
                        while (c.index > operator_begin + 1 && (c.prev() == '+' || c.prev() == '-' ||
                                                                c.prev() == '~' || c.prev() == '!')) {
                            c.dec();
                        }
                        data.append(c.substring(operator_begin, c.index));
                        if (data.toString().equals("$")) {
                            kind = Token.Kind.DOLLAR;
                            data.delete(0, data.length());
                        } else {
                            kind = Token.Kind.OPERATOR;
                        }
                    } else {
                        final String rep;
                        if (c.cur() < 32) {
                            rep = "code " + ((int)c.cur());
                        }
                        else {
                            rep = "'" + c.cur() + "'";
                        }
                        throw new JsonnetStaticError(filename, begin, "Could not lex the character " + rep);
                    }
            }

            // Ensure that a bug in the above code does not cause an infinite memory consuming loop due
            // to pushing empty tokens.
            if (c.index == original_index) {
                throw new JsonnetStaticError(filename, begin, "Internal lexing error:  Pointer did not advance");
            }

            final Location end = new Location(c.line_number, c.index - c.line_start);
            r.add(new Token(kind,
                            fodder,
                            data.toString(),
                            string_block_indent,
                            string_block_term_indent.toString(),
                            new LocationRange(filename, begin, end)));
            fodder.clear();
            fresh_line = false;
        }

        final Location end = new Location(c.line_number, c.index - c.line_start + 1);
        r.add(new Token(Token.Kind.END_OF_FILE, fodder, "", "", "", new LocationRange(filename, end, end)));
        return r;
    }

    public String jsonnet_unlex(final Tokens tokens) {
        final StringBuilder ss = new StringBuilder();
        for (final Token t : tokens) {
            for (final FodderElement f : t.getFodder()) {
                switch (f.getKind()) {
                    case LINE_END: {
                        if (f.getComment().size() > 0) {
                            ss.append("LineEnd(").append(f.getBlanks()).append(", ").append(f.getIndent())
                                .append(", ").append(f.getComment().get(0)).append(")\n");
                        } else {
                            ss.append("LineEnd(").append(f.getBlanks()).append(", ").append(f.getIndent())
                                .append(")\n");
                        }
                    } break;
                    case INTERSTITIAL: {
                        ss.append("Interstitial(").append(f.getComment().get(0)).append(")\n");
                    } break;
                    case PARAGRAPH: {
                        ss.append("Paragraph(\n");
                        for (final String line : f.getComment()) {
                            ss.append("    ").append(line).append('\n');
                        }
                        ss.append(")\n");
                    } break;
                }
            }
            if (t.getKind() == Token.Kind.END_OF_FILE) {
                ss.append("EOF\n");
                break;
            }
            if (t.getKind() == Token.Kind.STRING_DOUBLE) {
                ss.append("\"").append(t.getData()).append("\"\n");
            } else if (t.getKind() == Token.Kind.STRING_SINGLE) {
                ss.append("'").append(t.getData()).append("'\n");
            } else if (t.getKind() == Token.Kind.STRING_BLOCK) {
                ss.append("|||\n");
                ss.append(t.getStringBlockIndent());
                final String data = t.getData();
                for (int i = 0; i < data.length(); ++i) {
                    ss.append(data.charAt(i));
                    if (data.charAt(i) == '\n' && i < data.length() && data.charAt(i + i) != '\n') {
                        ss.append(t.getStringBlockIndent());
                    }
                }
                ss.append(t.getStringBlockTermIndent()).append("|||\n");
            } else {
                ss.append(t.getData()).append("\n");
            }
        }
        return ss.toString();
    }

    private static final ArrayList<String> EMPTY = new ArrayList<String>();

    private static class LexerContext {
        public LexerContext(final byte[] input) {
            this.input = input;
        }

        public boolean in() {
            return this.index < this.input.length;
        }

        public boolean inNext() {
            return (this.index + 1) < this.input.length;
        }

        public boolean outAt(final int i) {
            return i >= this.input.length;
        }

        public boolean out() {
            return this.index >= this.input.length;
        }

        public boolean outNext() {
            return (this.index + 1) >= this.input.length;
        }

        public boolean outNext(final int step) {
            return (this.index + step) >= this.input.length;
        }

        public char at(final int i) {
            if (this.outAt(i)) {
                return '\0';
            }
            return (char)this.input[i];
        }

        public char cur() {
            if (this.out()) {
                return '\0';
            }
            return (char)this.input[this.index];
        }

        public char next() {
            if (this.outNext()) {
                return '\0';
            }
            return (char)this.input[this.index + 1];
        }

        public char next(final int step) {
            if (this.outNext(step)) {
                return '\0';
            }
            return (char)this.input[this.index + step];
        }

        public char prev() {
            return (char)this.input[this.index - 1];
        }

        public void inc() {
            ++this.index;
        }

        public void inc(final int step) {
            this.index += step;
        }

        public void dec() {
            --this.index;
        }

        public String substring(final int start, final int end) {
            return new String(Arrays.copyOfRange(this.input, start, end), StandardCharsets.UTF_8);
        }

        public LexerContext clone() {
            final LexerContext other = new LexerContext(input);
            other.index = this.index;
            other.line_number = this.line_number;
            other.line_start = this.line_start;
            other.new_lines = this.new_lines;
            other.indent = this.indent;
            other.blanks = this.blanks;
            return other;
        }

        public int index;
        public int line_number;
        public int line_start;
        public int new_lines;
        public int indent;
        public int blanks;

        private final byte[] input;
    }

    /**
     * Strip whitespace from both ends of a string, but only up to margin on the left hand side.
     */
    private static String strip_ws(final String s, final int margin) {
        if (s.length() == 0) {
            return s;  // Avoid underflow below.
        }
        int i = 0;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t' || s.charAt(i) == '\r') && i < margin) {
            i++;
        }
        int j = s.length();
        while (j > i && (s.charAt(j - 1) == ' ' || s.charAt(j - 1) == '\t' || s.charAt(j - 1) == '\r')) {
            j--;
        }
        return s.substring(i, j);
    }

    /**
     * Split a string by \n and also strip left (up to margin) & right whitespace from each line.
     */
    private static ArrayList<String> line_split(final String s, final int margin) {
        final ArrayList<String> ret = new ArrayList<String>();
        final StringBuilder ss = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == '\n') {
                ret.add(strip_ws(ss.toString(), margin));
                ss.delete(0, ss.length());
            } else {
                ss.append(s.charAt(i));
            }
        }
        ret.add(strip_ws(ss.toString(), margin));
        return ret;
    }

    /**
     * Consume whitespace.
     *
     * Return number of \n and number of spaces after last \n.  Convert \t to spaces.
     */
    private static void lex_ws(final LexerContext c) {
        c.indent = 0;
        c.new_lines = 0;
        for (; c.in() && (c.cur() == ' ' || c.cur() == '\n' || c.cur() == '\t' || c.cur() == '\r'); c.inc()) {
            switch (c.cur()) {
            case '\r':
                // Ignore.
                break;

            case '\n':
                c.indent = 0;
                c.new_lines++;
                c.line_number++;
                c.line_start = c.index + 1;
                break;

            case ' ':
                c.indent += 1;
                break;

            // This only works for \t at the beginning of lines, but we strip it everywhere else
            // anyway.  The only case where this will cause a problem is spaces followed by \t
            // at the beginning of a line.  However that is rare, ill-advised, and if re-indentation
            // is enabled it will be fixed later.
            case '\t': c.indent += 8; break;
            }
        }
    }

    /**
     * # Consume all text until the end of the line, return number of newlines after that and indent
     */
    private static String lex_until_newline(final LexerContext c) {
        final int original_index = c.index;
        int last_non_space = c.index;
        for (; c.cur() != '\0' && c.cur() != '\n'; c.inc()) {
            if (c.cur() != ' ' && c.cur() != '\t' && c.cur() != '\r') {
                last_non_space = c.index;
            }
        }
        final String text = c.substring(original_index, last_non_space);
        // Consume subsequent whitespace including the '\n'.
        lex_ws(c);
        c.blanks = c.new_lines == 0 ? 0 : c.new_lines - 1;
        return text;
    }

    private static boolean is_upper(final char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static boolean is_lower(final char c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean is_number(final char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean is_identifier_first(final char c) {
        return is_upper(c) || is_lower(c) || c == '_';
    }

    private static boolean is_identifier(final char c) {
        return is_identifier_first(c) || is_number(c);
    }

    private static boolean is_symbol(final char c) {
        switch (c) {
        case '!':
        case '$':
        case ':':
        case '~':
        case '+':
        case '-':
        case '&':
        case '|':
        case '^':
        case '=':
        case '<':
        case '>':
        case '*':
        case '/':
        case '%': return true;
        }
        return false;
    }

    private static final Map<String, Token.Kind> keywords;

    static {
        final HashMap<String, Token.Kind> keywordsBuilt = new HashMap<String, Token.Kind>();
        keywordsBuilt.put("assert", Token.Kind.ASSERT);
        keywordsBuilt.put("else", Token.Kind.ELSE);
        keywordsBuilt.put("error", Token.Kind.ERROR);
        keywordsBuilt.put("false", Token.Kind.FALSE);
        keywordsBuilt.put("for", Token.Kind.FOR);
        keywordsBuilt.put("function", Token.Kind.FUNCTION);
        keywordsBuilt.put("if", Token.Kind.IF);
        keywordsBuilt.put("import", Token.Kind.IMPORT);
        keywordsBuilt.put("importstr", Token.Kind.IMPORTSTR);
        keywordsBuilt.put("in", Token.Kind.IN);
        keywordsBuilt.put("local", Token.Kind.LOCAL);
        keywordsBuilt.put("null", Token.Kind.NULL_LIT);
        keywordsBuilt.put("self", Token.Kind.SELF);
        keywordsBuilt.put("super", Token.Kind.SUPER);
        keywordsBuilt.put("tailstrict", Token.Kind.TAILSTRICT);
        keywordsBuilt.put("then", Token.Kind.THEN);
        keywordsBuilt.put("true", Token.Kind.TRUE);
        keywords = Collections.unmodifiableMap(keywordsBuilt);
    }

    private enum NumberState {
        BEGIN,
        AFTER_ZERO,
        AFTER_ONE_TO_NINE,
        AFTER_DOT,
        AFTER_DIGIT,
        AFTER_E,
        AFTER_EXP_SIGN,
        AFTER_EXP_DIGIT
    }

    private static StringBuilder lex_number(final LexerContext c, final String filename, final Location begin)
            throws JsonnetStaticError {
        // This function should be understood with reference to the linked image:
        // http://www.json.org/number.gif

        // Note, we deviate from the json.org documentation as follows:
        // There is no reason to lex negative numbers as atomic tokens, it is better to parse them
        // as a unary operator combined with a numeric literal.  This avoids x-1 being tokenized as
        // <identifier> <number> instead of the intended <identifier> <binop> <number>.

        NumberState state;

        final StringBuilder r = new StringBuilder();

        state = NumberState.BEGIN;
        while (true) {
            switch (state) {
            case BEGIN:
                switch (c.cur()) {
                    case '0': state = NumberState.AFTER_ZERO; break;

                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': state = NumberState.AFTER_ONE_TO_NINE; break;

                    default: throw new JsonnetStaticError(filename, begin, "Couldn't lex number");
                }
                break;

            case AFTER_ZERO:
                switch (c.cur()) {
                    case '.': state = NumberState.AFTER_DOT; break;

                    case 'e':
                    case 'E': state = NumberState.AFTER_E; break;

                    default: return r;
                }
                break;

            case AFTER_ONE_TO_NINE:
                switch (c.cur()) {
                    case '.': state = NumberState.AFTER_DOT; break;

                    case 'e':
                    case 'E': state = NumberState.AFTER_E; break;

                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': state = NumberState.AFTER_ONE_TO_NINE; break;

                    default: return r;
                }
                break;

            case AFTER_DOT:
                switch (c.cur()) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': state = NumberState.AFTER_DIGIT; break;

                    default:
                        throw new JsonnetStaticError(filename, begin,
                                                     "Couldn't lex number, junk after decimal point: " + c.cur());
                }
                break;

            case AFTER_DIGIT:
                switch (c.cur()) {
                    case 'e':
                    case 'E': state = NumberState.AFTER_E; break;

                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': state = NumberState.AFTER_DIGIT; break;

                    default: return r;
                }
                break;

            case AFTER_E:
                switch (c.cur()) {
                    case '+':
                    case '-': state = NumberState.AFTER_EXP_SIGN; break;

                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': state = NumberState.AFTER_EXP_DIGIT; break;

                    default:
                        throw new JsonnetStaticError(filename, begin,
                                                     "Couldn't lex number, junk after 'E': " + c.cur());
                }
                break;

            case AFTER_EXP_SIGN:
                switch (c.cur()) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': state = NumberState.AFTER_EXP_DIGIT; break;

                    default:
                        throw new JsonnetStaticError(filename, begin,
                                                     "Couldn't lex number, junk after exponent sign: " + c.cur());
                }
                break;

            case AFTER_EXP_DIGIT:
                switch (c.cur()) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': state = NumberState.AFTER_EXP_DIGIT; break;

                    default: return r;
                }
                break;
            }
            r.append(c.cur());
            c.inc();
        }
    }

    // Check that b has at least the same whitespace prefix as a and returns the amount of this
    // whitespace, otherwise returns 0.  If a has no whitespace prefix than return 0.
    private static int whitespace_check(final LexerContext c, final int a, final int b) {
        int i = 0;
        while (c.at(a + i) == ' ' || c.at(a + i) == '\t') {
            if (c.at(b + i) != c.at(a + i)) {
                return 0;
            }
            i++;
        }
        return i;
    }

    /* Commented out originally in C++
    static void add_whitespace(Fodder &fodder, const char *s, size_t n) {
        std::string ws(s, n);
        if (fodder.size() == 0 || fodder.back().kind != FodderElement::WHITESPACE) {
            fodder.emplace_back(FodderElement::WHITESPACE, ws);
        } else {
            fodder.back().data += ws;
        }
    }
    */
}
