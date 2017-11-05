package org.jsonnet.parser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.jsonnet.JsonnetStaticError;
import org.jsonnet.Location;
import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;
import org.jsonnet.lexer.FodderElement;
import org.jsonnet.lexer.Token;
import org.jsonnet.lexer.Tokens;

/**
 * Holds state while parsing a given token list.
 */
public class Parser {
    /**
     * Parse a given JSON++ string.
     *
     * @param alloc Used to allocate the AST nodes.  The Allocator must outlive the AST pointer returned.
     * @param tokens The list of tokens (all tokens are popped except EOF).
     * @return The parsed abstract syntax tree.
     */
    public static AST jsonnet_parse(final Allocator alloc, final Tokens tokens) throws JsonnetStaticError {
        final Parser parser = new Parser(tokens, alloc);
        final AST expr = parser.parse(PrecedenceMap.MAX_PRECEDENCE);
        final Token remainingFirstToken = parser.getRemainingFirstToken();
        if (remainingFirstToken.getKind() != Token.Kind.END_OF_FILE) {
            throw new JsonnetStaticError(remainingFirstToken.getLocation(),
                                         "Did not expect: " + remainingFirstToken.toString());
        }
        return expr;
    }

    /**
     * Outputs a number, trying to preserve precision as well as possible.
     */
    public static String jsonnet_unparse_number(final double v) {
        if (v == Math.floor(v)) {
            return INTEGRAL_FORMAT.format(v);
        } else {
            // See "What Every Computer Scientist Should Know About Floating-Point Arithmetic"
            // Theorem 15
            // http://docs.oracle.com/cd/E19957-01/806-3568/ncg_goldberg.html
            return FLOATING_FORMAT.format(v);
        }
    }

    /**
     * The inverse of jsonnet_parse.
     */
    public static String jsonnet_unparse_jsonnet(final AST ast,
                                                 final Fodder final_fodder,
                                                 final int indent,
                                                 final boolean pad_arrays,
                                                 final boolean pad_objects,
                                                 final char comment_style) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    private static LocationRange span(final Token begin) {
        return new LocationRange(begin.getLocation().getFile(),
                                 begin.getLocation().getBegin(),
                                 begin.getLocation().getEnd());
    }

    private static LocationRange span(final Token begin, final Token end) {
        return new LocationRange(begin.getLocation().getFile(),
                                 begin.getLocation().getBegin(),
                                 end.getLocation().getEnd());
    }

    private static LocationRange span(final Token begin, final AST end) {
        return new LocationRange(begin.getLocation().getFile(),
                                 begin.getLocation().getBegin(),
                                 end.getLocation().getEnd());
    }

    // The private member functions are utilities for dealing with the token stream.

    /*
    private static JsonnetStaticError unexpected(const Token &tok, const std::string &while_) {
        std::stringstream ss;
        ss << "Unexpected: " << tok.kind << " while " << while_;
        return StaticError(tok.location, ss.str());
    }
    */

    private Token pop() {
        final Token tok = this.tokens.pollFirst();
        return tok;
    }

    private void push(final Token tok) {
        this.tokens.push(tok);
    }

    private Token peek() {
        final Token tok = this.tokens.peekFirst();
        return tok;
    }

    /**
     * Only call this is peek() is not an EOF token.
     */
    private Token doublePeek() {
        return this.tokens.get(1);
    }

    private Token popExpect(final Token.Kind k) throws JsonnetStaticError {
        return this.popExpect(k, null);
    }

    private Token popExpect(final Token.Kind k, final String data) throws JsonnetStaticError {
        final Token tok = this.pop();
        if (tok.getKind() != k) {
            throw new JsonnetStaticError(tok.getLocation(),
                                         "Expected token " + k.toString() + " but got " + tok.toString());
        }
        if (data != null && !tok.getData().equals(data)) {
            throw new JsonnetStaticError(tok.getLocation(),
                                         "Expected operator " + data + " but got " + tok.getData());
        }
        return tok;
    }

    private final LinkedList<Token> tokens;
    private final Allocator alloc;

    public Parser(final Tokens tokens, final Allocator alloc) {
        this.tokens = new LinkedList<Token>(tokens);
        this.alloc = alloc;
    }

    private static class ParseArgsContext {
        public boolean got_comma;
    }

    /**
     * Parse a comma-separated list of expressions.
     *
     * Allows an optional ending comma.
     *
     * @param exprs Expressions added here.
     * @param end The token that ends the list (e.g. ] or )).
     * @param element_kind Used in error messages when a comma was not found.
     * @return The last token (the one that matched parameter end).
     */
    private Token parseArgs(final ArgParams args,
                            final Token.Kind end,
                            final String element_kind,
                            final ParseArgsContext context) throws JsonnetStaticError {
        context.got_comma = false;
        boolean first = true;
        do {
            final Token next = peek();
            if (next.getKind() == end) {
                // context.got_comma can be true or false here.
                return this.pop();
            }
            if (!first && !context.got_comma) {
                throw new JsonnetStaticError(next.getLocation(), "Expected a comma before next " + element_kind + ".");
            }
            // Either id=expr or id or expr, but note that expr could be id==1 so this needs
            // look-ahead.
            Fodder id_fodder = new Fodder();
            Identifier id = null;
            Fodder eq_fodder = new Fodder();
            if (this.peek().getKind() == Token.Kind.IDENTIFIER) {
                final Token maybe_eq = this.doublePeek();
                if (maybe_eq.getKind() == Token.Kind.OPERATOR && maybe_eq.getData().equals("=")) {
                    id_fodder = this.peek().getFodder();
                    id = this.alloc.makeIdentifier(this.peek().getData());
                    eq_fodder = maybe_eq.getFodder();
                    this.pop();  // id
                    this.pop();  // eq
                }
            }
            final AST expr = this.parse(PrecedenceMap.MAX_PRECEDENCE);
            context.got_comma = false;
            first = false;
            Fodder comma_fodder = new Fodder();
            if (this.peek().getKind() == Token.Kind.COMMA) {
                final Token comma = this.pop();
                comma_fodder = comma.getFodder();
                context.got_comma = true;
            }
            args.add(new ArgParam(id_fodder, id, eq_fodder, expr, comma_fodder));
        } while (true);
    }

    private static class ParseParamsContext {
        public Fodder close_fodder;
        public boolean got_comma;
    }

    private ArgParams parseParams(final String element_kind, final ParseParamsContext context)
            throws JsonnetStaticError {
        final ArgParams params = new ArgParams();
        final ParseArgsContext parseArgsContext = new ParseArgsContext();
        final Token paren_r = parseArgs(params, Token.Kind.PAREN_R, element_kind, parseArgsContext);
        context.got_comma = parseArgsContext.got_comma;

        // Check they're all identifiers
        // parseArgs returns f(x) with x as an expression.  Convert it here.
        for (final ArgParam p : params) {
            if (p.getId() == null) {
                final Var pv;
                try {
                    pv = (Var)p.getExpr();
                }
                catch (ClassCastException ex) {
                    throw new JsonnetStaticError(p.getExpr().getLocation(), "Could not parse parameter here.");
                }
                if (pv == null) {
                    throw new JsonnetStaticError(p.getExpr().getLocation(), "Could not parse parameter here.");
                }
                p.setId(pv.getId());
                p.setIdFodder(pv.getOpenFodder());
                p.setExpr(null);
            }
        }

        context.close_fodder = paren_r.getFodder();

        return params;
    }

    private Token parseBind(final Local.Binds binds) throws JsonnetStaticError {
        final Token var_id = this.popExpect(Token.Kind.IDENTIFIER);
        final Identifier id = this.alloc.makeIdentifier(var_id.getData());
        for (final Local.Bind bind : binds) {
            if (bind.getVar().equals(id)) {
                throw new JsonnetStaticError(var_id.getLocation(), "Duplicate local var: " + var_id.getData());
            }
        }
        boolean is_function = false;
        ArgParams params = new ArgParams();
        boolean trailing_comma = false;
        Fodder fodder_l = new Fodder();
        Fodder fodder_r = new Fodder();
        if (this.peek().getKind() == Token.Kind.PAREN_L) {
            final Token paren_l = this.pop();
            fodder_l = paren_l.getFodder();
            final ParseParamsContext parseParamsContext = new ParseParamsContext();
            params = parseParams("function parameter", parseParamsContext);
            trailing_comma = parseParamsContext.got_comma;
            fodder_r = parseParamsContext.close_fodder;
            is_function = true;
        }
        final Token eq = this.popExpect(Token.Kind.OPERATOR, "=");
        final AST body = this.parse(PrecedenceMap.MAX_PRECEDENCE);
        final Token delim = this.pop();
        binds.add(new Local.Bind(var_id.getFodder(),
                                 id,
                                 eq.getFodder(),
                                 body,
                                 is_function,
                                 fodder_l,
                                 params,
                                 trailing_comma,
                                 fodder_r,
                                 delim.getFodder()));
        return delim;
    }

    private class ParseObjectRemainderContext {
        public AST obj;
    }

    private Token parseObjectRemainder(final ParseObjectRemainderContext context, final Token tok)
            throws JsonnetStaticError {
        final ObjectFields fields = new ObjectFields();
        final Set<String> literal_fields = new HashSet<String>();   // For duplicate fields detection.
        final Set<Identifier> binds = new HashSet<Identifier>();    // For duplicate locals detection.

        boolean got_comma = false;
        boolean first = true;
        Token next = this.pop();

        do {
            if (next.getKind() == Token.Kind.BRACE_R) {
                context.obj = new org.jsonnet.parser.Object(
                    span(tok, next), tok.getFodder(), fields, got_comma, next.getFodder());
                return next;

            } else if (next.getKind() == Token.Kind.FOR) {
                // It's a comprehension
                int num_fields = 0;
                int num_asserts = 0;
                ObjectField field_ptr = null;
                for (final ObjectField field : fields) {
                    if (field.getKind() == ObjectField.Kind.LOCAL) {
                        continue;
                    }
                    if (field.getKind() == ObjectField.Kind.ASSERT) {
                        num_asserts++;
                        continue;
                    }
                    field_ptr = field;
                    num_fields++;
                }
                if (num_asserts > 0) {
                    throw new JsonnetStaticError(next.getLocation(), "Object comprehension cannot have asserts.");
                }
                if (num_fields != 1) {
                    throw new JsonnetStaticError(next.getLocation(), "Object comprehension can only have one field.");
                }
                final ObjectField field = field_ptr;

                if (field.getHide() != ObjectField.Hide.INHERIT) {
                    throw new JsonnetStaticError(next.getLocation(),
                                                 "Object comprehensions cannot have hidden fields.");
                }

                if (field.getKind() != ObjectField.Kind.FIELD_EXPR) {
                    throw new JsonnetStaticError(next.getLocation(), "Object comprehensions can only have [e] fields.");
                }

                final ArrayList<ComprehensionSpec> specs = new ArrayList<ComprehensionSpec>();
                final Token last = this.parseComprehensionSpecs(Token.Kind.BRACE_R, next.getFodder(), specs);
                context.obj = new ObjectComprehension(
                    span(tok, last), tok.getFodder(), fields, got_comma, specs, last.getFodder());

                return last;
            }

            if (!got_comma && !first) {
                throw new JsonnetStaticError(next.getLocation(), "Expected a comma before next field.");
            }

            first = false;
            got_comma = false;

            switch (next.getKind()) {
                case BRACKET_L:
                case IDENTIFIER:
                case STRING_DOUBLE:
                case STRING_SINGLE:
                case STRING_BLOCK:
                case VERBATIM_STRING_DOUBLE:
                case VERBATIM_STRING_SINGLE: {
                    final ObjectField.Kind kind;
                    final AST expr1;
                    final Identifier id;
                    final Fodder fodder1;
                    final Fodder fodder2;
                    if (next.getKind() == Token.Kind.IDENTIFIER) {
                        fodder1 = next.getFodder();
                        kind = ObjectField.Kind.FIELD_ID;
                        id = this.alloc.makeIdentifier(next.getData());
                        expr1 = null;
                        fodder2 = new Fodder();
                    } else if (next.getKind() == Token.Kind.STRING_DOUBLE) {
                        kind = ObjectField.Kind.FIELD_STR;
                        expr1 = new LiteralString(next.getLocation(),
                                                  next.getFodder(),
                                                  next.getData(),
                                                  LiteralString.TokenKind.DOUBLE,
                                                  "",
                                                  "");
                        id = null;
                        fodder1 = new Fodder();
                        fodder2 = new Fodder();
                    } else if (next.getKind() == Token.Kind.STRING_SINGLE) {
                        kind = ObjectField.Kind.FIELD_STR;
                        expr1 = new LiteralString(next.getLocation(),
                                                  next.getFodder(),
                                                  next.getData(),
                                                  LiteralString.TokenKind.SINGLE,
                                                  "",
                                                  "");
                        id = null;
                        fodder1 = new Fodder();
                        fodder2 = new Fodder();
                    } else if (next.getKind() == Token.Kind.STRING_BLOCK) {
                        kind = ObjectField.Kind.FIELD_STR;
                        expr1 = new LiteralString(next.getLocation(),
                                                  next.getFodder(),
                                                  next.getData(),
                                                  LiteralString.TokenKind.BLOCK,
                                                  next.getStringBlockIndent(),
                                                  next.getStringBlockTermIndent());
                        id = null;
                        fodder1 = new Fodder();
                        fodder2 = new Fodder();
                    } else if (next.getKind() == Token.Kind.VERBATIM_STRING_SINGLE) {
                        kind = ObjectField.Kind.FIELD_STR;
                        expr1 = new LiteralString(next.getLocation(),
                                                  next.getFodder(),
                                                  next.getData(),
                                                  LiteralString.TokenKind.VERBATIM_SINGLE,
                                                  "",
                                                  "");
                        id = null;
                        fodder1 = new Fodder();
                        fodder2 = new Fodder();
                    } else if (next.getKind() == Token.Kind.VERBATIM_STRING_DOUBLE) {
                        kind = ObjectField.Kind.FIELD_STR;
                        expr1 = new LiteralString(next.getLocation(),
                                                  next.getFodder(),
                                                  next.getData(),
                                                  LiteralString.TokenKind.VERBATIM_DOUBLE,
                                                  "",
                                                  "");
                        id = null;
                        fodder1 = new Fodder();
                        fodder2 = new Fodder();
                    } else {
                        kind = ObjectField.Kind.FIELD_EXPR;
                        fodder1 = next.getFodder();
                        expr1 = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                        Token bracket_r = this.popExpect(Token.Kind.BRACKET_R);
                        fodder2 = bracket_r.getFodder();
                        id = null;
                    }

                    final boolean is_method;
                    final boolean meth_comma;
                    final ArgParams params;
                    final Fodder fodder_l;
                    final Fodder fodder_r;
                    if (this.peek().getKind() == Token.Kind.PAREN_L) {
                        final Token paren_l = this.pop();
                        fodder_l = paren_l.getFodder();
                        final ParseParamsContext parseParamsContext = new ParseParamsContext();
                        params = this.parseParams("method parameter", parseParamsContext);
                        meth_comma = parseParamsContext.got_comma;
                        fodder_r = parseParamsContext.close_fodder;
                        is_method = true;
                    } else {
                        is_method = false;
                        meth_comma = false;
                        params = new ArgParams();
                        fodder_l = new Fodder();
                        fodder_r = new Fodder();
                    }

                    final boolean plus_sugar;

                    final Token op = this.popExpect(Token.Kind.OPERATOR);
                    final String od = op.getData();
                    if (od.charAt(0) == '+') {
                        plus_sugar = true;
                    } else {
                        plus_sugar = false;
                    }
                    int colons = 0;
                    for (int i = (plus_sugar ? 1 : 0); i < od.length(); ++i) {
                        if (od.charAt(i) != ':') {
                            throw new JsonnetStaticError(
                                next.getLocation(),
                                "Expected one of :, ::, :::, +:, +::, +:::, got: " + op.getData());
                        }
                        ++colons;
                    }
                    final ObjectField.Hide field_hide;
                    switch (colons) {
                        case 1: field_hide = ObjectField.Hide.INHERIT; break;

                        case 2: field_hide = ObjectField.Hide.HIDDEN; break;

                        case 3: field_hide = ObjectField.Hide.VISIBLE; break;

                        default:
                            throw new JsonnetStaticError(
                                next.getLocation(),
                                "Expected one of :, ::, :::, +:, +::, +:::, got: " + op.getData());
                    }

                    // Basic checks for invalid Jsonnet code.
                    if (is_method && plus_sugar) {
                        throw new JsonnetStaticError(next.getLocation(),
                                                     "Cannot use +: syntax sugar in a method: " + next.getData());
                    }
                    if (kind != ObjectField.Kind.FIELD_EXPR) {
                        if (!literal_fields.add(next.getData())) {
                            throw new JsonnetStaticError(next.getLocation(), "Duplicate field: " + next.getData());
                        }
                    }

                    final AST body = this.parse(PrecedenceMap.MAX_PRECEDENCE);

                    final Fodder comma_fodder;
                    next = this.pop();
                    if (next.getKind() == Token.Kind.COMMA) {
                        comma_fodder = next.getFodder();
                        next = this.pop();
                        got_comma = true;
                    } else {
                        comma_fodder = new Fodder();
                    }
                    fields.add(new ObjectField(kind,
                                               fodder1,
                                               fodder2,
                                               fodder_l,
                                               fodder_r,
                                               field_hide,
                                               plus_sugar,
                                               is_method,
                                               expr1,
                                               id,
                                               params,
                                               meth_comma,
                                               op.getFodder(),
                                               body,
                                               null,
                                               comma_fodder));
                } break;

                case LOCAL: {
                    final Fodder local_fodder = next.getFodder();
                    final Token var_id = this.popExpect(Token.Kind.IDENTIFIER);
                    final Identifier id = this.alloc.makeIdentifier(var_id.getData());

                    if (binds.contains(id)) {
                        throw new JsonnetStaticError(var_id.getLocation(), "Duplicate local var: " + var_id.getData());
                    }
                    final boolean is_method;
                    final boolean func_comma;
                    final ArgParams params;
                    final Fodder paren_l_fodder;
                    final Fodder paren_r_fodder;
                    if (this.peek().getKind() == Token.Kind.PAREN_L) {
                        final Token paren_l = this.pop();
                        paren_l_fodder = paren_l.getFodder();
                        is_method = true;
                        final ParseParamsContext parseParamsContext = new ParseParamsContext();
                        params = parseParams("function parameter", parseParamsContext);
                        func_comma = parseParamsContext.got_comma;
                        paren_r_fodder = parseParamsContext.close_fodder;
                    } else {
                        is_method = false;
                        func_comma = false;
                        params = new ArgParams();
                        paren_l_fodder = new Fodder();
                        paren_r_fodder = new Fodder();
                    }
                    final Token eq = this.popExpect(Token.Kind.OPERATOR, "=");
                    final AST body = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                    binds.add(id);

                    final Fodder comma_fodder;
                    next = pop();
                    if (next.getKind() == Token.Kind.COMMA) {
                        comma_fodder = next.getFodder();
                        next = this.pop();
                        got_comma = true;
                    } else {
                        comma_fodder = new Fodder();
                    }
                    fields.add(ObjectField.Local(local_fodder,
                                                 var_id.getFodder(),
                                                 paren_l_fodder,
                                                 paren_r_fodder,
                                                 is_method,
                                                 id,
                                                 params,
                                                 func_comma,
                                                 eq.getFodder(),
                                                 body,
                                                 comma_fodder));

                } break;

                case ASSERT: {
                    final Fodder assert_fodder = next.getFodder();
                    final AST cond = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                    final AST msg;
                    final Fodder colon_fodder;
                    if (this.peek().getKind() == Token.Kind.OPERATOR && this.peek().getData().equals(":")) {
                        final Token colon = this.pop();
                        colon_fodder = colon.getFodder();
                        msg = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                    } else {
                        colon_fodder = new Fodder();
                        msg = null;
                    }

                    final Fodder comma_fodder;
                    next = this.pop();
                    if (next.getKind() == Token.Kind.COMMA) {
                        comma_fodder = next.getFodder();
                        next = this.pop();
                        got_comma = true;
                    } else {
                        comma_fodder = new Fodder();
                    }
                    fields.add(
                        ObjectField.Assert(assert_fodder, cond, colon_fodder, msg, comma_fodder));
                } break;

                default: throw new JsonnetStaticError(next.getLocation(),
                                                      "Unexpected: " + next.getKind() +
                                                      " while " + "parsing field definition");
            }

        } while (true);
    }

    /**
     * parses for x in expr for y in expr if expr for z in expr ...
     */
    private Token parseComprehensionSpecs(final Token.Kind end, Fodder for_fodder,  // for_fodder is not output (?)
                                          final ArrayList<ComprehensionSpec> specs) throws JsonnetStaticError
    {
        while (true) {
            LocationRange l;
            final Token id_token = this.popExpect(Token.Kind.IDENTIFIER);
            final Identifier id = this.alloc.makeIdentifier(id_token.getData());
            final Token in_token = this.popExpect(Token.Kind.IN);
            final AST arr = this.parse(PrecedenceMap.MAX_PRECEDENCE);
            specs.add(new ComprehensionSpec(
                          ComprehensionSpec.Kind.FOR, for_fodder, id_token.getFodder(), id, in_token.getFodder(), arr));

            Token maybe_if = this.pop();
            for (; maybe_if.getKind() == Token.Kind.IF; maybe_if = this.pop()) {
                final AST cond = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                specs.add(new ComprehensionSpec(
                              ComprehensionSpec.Kind.IF, maybe_if.getFodder(), new Fodder(), null, new Fodder(), cond));
            }
            if (maybe_if.getKind() == end) {
                return maybe_if;
            }
            if (maybe_if.getKind() != Token.Kind.FOR) {
                throw new JsonnetStaticError(maybe_if.getLocation(),
                                             "Expected for, if or " + end + " after for clause, got: " + maybe_if);
            }
            for_fodder = maybe_if.getFodder();
        }
    }

    private AST parseTerminal() throws JsonnetStaticError {
        final Token tok = this.pop();
        switch (tok.getKind()) {
            case ASSERT:
            case BRACE_R:
            case BRACKET_R:
            case COMMA:
            case DOT:
            case ELSE:
            case ERROR:
            case FOR:
            case FUNCTION:
            case IF:
            case IN:
            case IMPORT:
            case IMPORTSTR:
            case LOCAL:
            case OPERATOR:
            case PAREN_R:
            case SEMICOLON:
            case TAILSTRICT:
            case THEN:
                throw new JsonnetStaticError(tok.getLocation(),
                                             "Unexpected: " + tok.getKind() + " while " + "parsing terminal");

            case END_OF_FILE: throw new JsonnetStaticError(tok.getLocation(), "Unexpected end of file.");

            case BRACE_L: {
                final ParseObjectRemainderContext parseObjectRemainderContext = new ParseObjectRemainderContext();
                this.parseObjectRemainder(parseObjectRemainderContext, tok);
                return parseObjectRemainderContext.obj;
            }

            case BRACKET_L: {
                Token next = this.peek();
                if (next.getKind() == Token.Kind.BRACKET_R) {
                    final Token bracket_r = this.pop();
                    return new org.jsonnet.parser.Array(
                        span(tok, next), tok.getFodder(), new Array.Elements(), false, bracket_r.getFodder());
                }
                final AST first = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                boolean got_comma = false;
                Fodder comma_fodder;
                next = this.peek();
                if (!got_comma && next.getKind() == Token.Kind.COMMA) {
                    final Token comma = this.pop();
                    comma_fodder = comma.getFodder();
                    next = peek();
                    got_comma = true;
                } else {
                    comma_fodder = new Fodder();
                }

                if (next.getKind() == Token.Kind.FOR) {
                    // It's a comprehension
                    final Token for_token = this.pop();
                    final ArrayList<ComprehensionSpec> specs = new ArrayList<ComprehensionSpec>();
                    final Token last = this.parseComprehensionSpecs(Token.Kind.BRACKET_R, for_token.getFodder(), specs);
                    return new ArrayComprehension(span(tok, last),
                                                  tok.getFodder(),
                                                  first,
                                                  comma_fodder,
                                                  got_comma,
                                                  specs,
                                                  last.getFodder());
                }

                // Not a comprehension: It can have more elements.
                final Array.Elements elements = new Array.Elements();
                elements.add(new Array.Element(first, comma_fodder));
                do {
                    if (next.getKind() == Token.Kind.BRACKET_R) {
                        final Token bracket_r = this.pop();
                        return new Array(
                            span(tok, next), tok.getFodder(), elements, got_comma, bracket_r.getFodder());
                    }
                    if (!got_comma) {
                        throw new JsonnetStaticError(next.getLocation(), "Expected a comma before next array element.");
                    }
                    final AST expr = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                    // TODO (Java): Confirm that it is clearing |comma_fodder| already contained in |elements|.
                    comma_fodder.clear();
                    got_comma = false;
                    next = this.peek();
                    if (next.getKind() == Token.Kind.COMMA) {
                        final Token comma = this.pop();
                        comma_fodder = comma.getFodder();
                        next = this.peek();
                        got_comma = true;
                    }
                    elements.add(new Array.Element(expr, comma_fodder));
                } while (true);
            }

            case PAREN_L: {
                final AST inner = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                final Token close = this.popExpect(Token.Kind.PAREN_R);
                return new Parens(span(tok, close), tok.getFodder(), inner, close.getFodder());
            }

            // Literals
            case NUMBER: return new LiteralNumber(span(tok), tok.getFodder(), tok.getData());

            case STRING_SINGLE:
                return new LiteralString(
                    span(tok), tok.getFodder(), tok.getData(), LiteralString.TokenKind.SINGLE, "", "");
            case STRING_DOUBLE:
                return new LiteralString(
                    span(tok), tok.getFodder(), tok.getData(), LiteralString.TokenKind.DOUBLE, "", "");
            case STRING_BLOCK:
                return new LiteralString(span(tok),
                                         tok.getFodder(),
                                         tok.getData(),
                                         LiteralString.TokenKind.BLOCK,
                                         tok.getStringBlockIndent(),
                                         tok.getStringBlockTermIndent());
            case VERBATIM_STRING_SINGLE:
                return new LiteralString(
                    span(tok), tok.getFodder(), tok.getData(), LiteralString.TokenKind.VERBATIM_SINGLE, "", "");
            case VERBATIM_STRING_DOUBLE:
                return new LiteralString(
                    span(tok), tok.getFodder(), tok.getData(), LiteralString.TokenKind.VERBATIM_DOUBLE, "", "");

            case FALSE: return new LiteralBoolean(span(tok), tok.getFodder(), false);

            case TRUE: return new LiteralBoolean(span(tok), tok.getFodder(), true);

            case NULL_LIT:
                return new LiteralNull(span(tok), tok.getFodder());

            // Variables
            case DOLLAR: return new Dollar(span(tok), tok.getFodder());

            case IDENTIFIER:
                return new Var(span(tok), tok.getFodder(), this.alloc.makeIdentifier(tok.getData()));

            case SELF: return new Self(span(tok), tok.getFodder());

            case SUPER: {
                final Token next = this.pop();
                final AST index;
                final Identifier id;
                final Fodder id_fodder;
                switch (next.getKind()) {
                    case DOT: {
                        final Token field_id = this.popExpect(Token.Kind.IDENTIFIER);
                        id_fodder = field_id.getFodder();
                        id = this.alloc.makeIdentifier(field_id.getData());
                        index = null;
                    } break;
                    case BRACKET_L: {
                        index = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                        final Token bracket_r = this.popExpect(Token.Kind.BRACKET_R);
                        id_fodder = bracket_r.getFodder();  // Not id_fodder, but use the same var.
                        id = null;
                    } break;
                    default: throw new JsonnetStaticError(tok.getLocation(), "Expected . or [ after super.");
                }
                return new SuperIndex(
                    span(tok), tok.getFodder(), next.getFodder(), index, id_fodder, id);
            }
        }

        throw new JsonnetStaticError("INTERNAL ERROR: Unknown tok kind: " + tok.getKind().toString());
    }

    public AST parse(final int precedence) throws JsonnetStaticError {
        final Token begin = this.peek();

        switch (begin.getKind()) {
            // These cases have effectively PrecedenceMap.MAX_PRECEDENCE as the first
            // call to parse will parse them.
            case ASSERT: {
                this.pop();
                final AST cond = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                final Fodder colonFodder;
                final AST msg;
                if (peek().getKind() == Token.Kind.OPERATOR && peek().getData().equals(":")) {
                    final Token colon = this.pop();
                    colonFodder = colon.getFodder();
                    msg = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                } else {
                    colonFodder = null;
                    msg = null;
                }
                final Token semicolon = this.popExpect(Token.Kind.SEMICOLON);
                AST rest = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                return new Assert(span(begin, rest),
                                  begin.getFodder(),
                                  cond,
                                  colonFodder,
                                  msg,
                                  semicolon.getFodder(),
                                  rest);
            }

            case ERROR: {
                this.pop();
                final AST expr = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                return new org.jsonnet.parser.Error(span(begin, expr), begin.getFodder(), expr);
            }

            case IF: {
                this.pop();
                final AST cond = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                final Token then = this.popExpect(Token.Kind.THEN);
                final AST branch_true = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                if (this.peek().getKind() == Token.Kind.ELSE) {
                    final Token else_ = this.pop();
                    final AST branch_false = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                    return new org.jsonnet.parser.Conditional(span(begin, branch_false),
                                                           begin.getFodder(),
                                                           cond,
                                                           then.getFodder(),
                                                           branch_true,
                                                           else_.getFodder(),
                                                           branch_false);
                }
                return new org.jsonnet.parser.Conditional(span(begin, branch_true),
                                                       begin.getFodder(),
                                                       cond,
                                                       then.getFodder(),
                                                       branch_true,
                                                       new Fodder(),
                                                       null);
            }

            case FUNCTION: {
                this.pop();  // Still available in 'begin'.
                final Token paren_l = this.pop();
                if (paren_l.getKind() == Token.Kind.PAREN_L) {
                    final ParseParamsContext parseParamsContext = new ParseParamsContext();
                    final ArgParams params = this.parseParams("function parameter", parseParamsContext);
                    final AST body = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                    return new org.jsonnet.parser.Function(span(begin, body),
                                                        begin.getFodder(),
                                                        paren_l.getFodder(),
                                                        params,
                                                        parseParamsContext.got_comma,
                                                        parseParamsContext.close_fodder,  // paren_r_fodder
                                                        body);
                } else {
                    throw new JsonnetStaticError(paren_l.getLocation(), "Expected ( but got " + paren_l.toString());
                }
            }

            case IMPORT: {
                this.pop();
                final AST body = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                if (body instanceof LiteralString) {
                    final LiteralString lit = (LiteralString)body;
                    if (lit.getTokenKind() == LiteralString.TokenKind.BLOCK) {
                        throw new JsonnetStaticError(lit.getLocation(),
                                                     "Cannot use text blocks in import statements.");
                    }
                    return new org.jsonnet.parser.Import(span(begin, body), begin.getFodder(), lit);
                } else {
                    throw new JsonnetStaticError(body.getLocation(), "Computed imports are not allowed.");
                }
            }

            case IMPORTSTR: {
                this.pop();
                final AST body = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                if (body instanceof LiteralString) {
                    final LiteralString lit = (LiteralString)body;
                    if (lit.getTokenKind() == LiteralString.TokenKind.BLOCK) {
                        throw new JsonnetStaticError(lit.getLocation(),
                                                     "Cannot use text blocks in import statements.");
                    }
                    return new org.jsonnet.parser.Importstr(span(begin, body), begin.getFodder(), lit);
                } else {
                    throw new JsonnetStaticError(body.getLocation(), "Computed imports are not allowed.");
                }
            }

            case LOCAL: {
                this.pop();
                Local.Binds binds = new Local.Binds();
                do {
                    final Token delim = this.parseBind(binds);
                    if (delim.getKind() != Token.Kind.SEMICOLON && delim.getKind() != Token.Kind.COMMA) {
                        throw new JsonnetStaticError(delim.getLocation(),
                                                     "Expected , or ; but got " + delim.toString());
                    }
                    if (delim.getKind() == Token.Kind.SEMICOLON) {
                        break;
                    }
                } while (true);
                final AST body = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                return new org.jsonnet.parser.Local(span(begin, body), begin.getFodder(), binds, body);
            }

            default:

                // Unary operator.
                if (begin.getKind() == Token.Kind.OPERATOR) {
                    final org.jsonnet.parser.UnaryOp uop = PrecedenceMap.UNARY_MAP.get(begin.getData());
                    if (uop == null) {
                        throw new JsonnetStaticError(begin.getLocation(), "Not a unary operator: " + begin.getData());
                    }
                    if (PrecedenceMap.UNARY_PRECEDENCE == precedence) {
                        final Token op = this.pop();
                        final AST expr = this.parse(precedence);
                        return new org.jsonnet.parser.Unary(span(op, expr), op.getFodder(), uop, expr);
                    }
                }

                // Base case
                if (precedence == 0) {
                    return this.parseTerminal();
                }

                AST lhs = this.parse(precedence - 1);

                Fodder begin_fodder = new Fodder();

                while (true) {
                    // Then next token must be a binary operator.

                    // The compiler can't figure out that this is never used uninitialized.
                    BinaryOp bop = BinaryOp.BOP_PLUS;

                    // Check precedence is correct for this level.  If we're
                    // parsing operators with higher precedence, then return
                    // lhs and let lower levels deal with the operator.
                    switch (this.peek().getKind()) {
                        // Logical / arithmetic binary operator.
                        case IN:
                        case OPERATOR:
                            if (this.peek().getData().equals(":")) {
                                // Special case for the colons in assert.
                                // Since COLON is no-longer a special token, we have to make sure it
                                // does not trip the op_is_binary test below.  It should
                                // terminate parsing of the expression here, returning control
                                // to the parsing of the actual assert AST.
                                return lhs;
                            }
                            if (this.peek().getData().equals("::")) {
                                // Special case for [e::]
                                // We need to stop parsing e when we see the :: and
                                // avoid tripping the op_is_binary test below.
                                return lhs;
                            }
                            bop = PrecedenceMap.BINARY_MAP.get(this.peek().getData());
                            if (bop == null) {
                                throw new JsonnetStaticError(this.peek().getLocation(),
                                                             "Not a binary operator: " + this.peek().getData());
                            }
                            if (PrecedenceMap.PRECEDENCE_MAP.get(bop) != precedence) {
                                return lhs;
                            }
                            break;

                        // Index, Apply
                        case DOT:
                        case BRACKET_L:
                        case PAREN_L:
                        case BRACE_L:
                            if (PrecedenceMap.APPLY_PRECEDENCE != precedence) {
                                return lhs;
                            }
                            break;

                        default: return lhs;
                    }

                    final Token op = this.pop();
                    if (op.getKind() == Token.Kind.BRACKET_L) {
                        final boolean is_slice;
                        final AST first;
                        final Fodder second_fodder;
                        final AST second;
                        final Fodder third_fodder;
                        final AST third;

                        if (this.peek().getKind() == Token.Kind.BRACKET_R) {
                            final Token tok = this.pop();
                            throw new JsonnetStaticError(tok.getLocation(),
                                                         "Unexpected: " + tok.getKind() +
                                                         " while " + "parsing index");
                        }

                        if (!this.peek().getData().equals(":") && !this.peek().getData().equals("::")) {
                            first = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                        } else {
                            first = null;
                        }

                        if (this.peek().getKind() == Token.Kind.OPERATOR && this.peek().getData().equals("::")) {
                            // Handle ::
                            is_slice = true;
                            final Token joined = this.pop();
                            second_fodder = joined.getFodder();
                            second = null;

                            third_fodder = new Fodder();
                            if (this.peek().getKind() != Token.Kind.BRACKET_R) {
                                third = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                            } else {
                                third = null;
                            }

                        } else if (this.peek().getKind() != Token.Kind.BRACKET_R) {
                            is_slice = true;
                            final Token delim = this.pop();
                            if (!delim.getData().equals(":")) {
                                throw new JsonnetStaticError(delim.getLocation(),
                                                             "Unexpected: " + delim.getKind() +
                                                             " while " + "parsing slice");
                            }

                            second_fodder = delim.getFodder();

                            if (!this.peek().getData().equals(":") && this.peek().getKind() != Token.Kind.BRACKET_R) {
                                second = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                            } else {
                                second = null;
                            }

                            if (this.peek().getKind() != Token.Kind.BRACKET_R) {
                                final Token delim2 = this.pop();
                                if (!delim2.getData().equals(":")) {
                                    throw new JsonnetStaticError(delim2.getLocation(),
                                                                 "Unexpected: " + delim2.getKind() +
                                                                 " while " + "parsing slice");
                                }

                                third_fodder = delim2.getFodder();

                                if (this.peek().getKind() != Token.Kind.BRACKET_R) {
                                    third = this.parse(PrecedenceMap.MAX_PRECEDENCE);
                                } else {
                                    third = null;
                                }
                            } else {
                                third_fodder = new Fodder();
                                third = null;
                            }
                        } else {
                            is_slice = false;
                            second_fodder = new Fodder();
                            second = null;
                            third_fodder = new Fodder();
                            third = null;
                        }
                        final Token end = this.popExpect(Token.Kind.BRACKET_R);
                        lhs = new Index(span(begin, end),
                                        begin_fodder,
                                        lhs,
                                        op.getFodder(),
                                        is_slice,
                                        first,
                                        second_fodder,
                                        second,
                                        third_fodder,
                                        third,
                                        end.getFodder());

                    } else if (op.getKind() == Token.Kind.DOT) {
                        final Token field_id = this.popExpect(Token.Kind.IDENTIFIER);
                        final Identifier id = this.alloc.makeIdentifier(field_id.getData());
                        lhs = new Index(span(begin, field_id),
                                        begin_fodder,
                                        lhs,
                                        op.getFodder(),
                                        field_id.getFodder(),
                                        id);

                    } else if (op.getKind() == Token.Kind.PAREN_L) {
                        final ArgParams args = new ArgParams();
                        final ParseArgsContext parseArgsContext = new ParseArgsContext();
                        final Token end = parseArgs(args, Token.Kind.PAREN_R, "function argument", parseArgsContext);
                        final boolean got_comma = parseArgsContext.got_comma;
                        final boolean tailstrict;
                        final Fodder tailstrict_fodder;
                        if (this.peek().getKind() == Token.Kind.TAILSTRICT) {
                            final Token tailstrict_token = this.pop();
                            tailstrict_fodder = tailstrict_token.getFodder();
                            tailstrict = true;
                        } else {
                            tailstrict = false;
                            tailstrict_fodder = new Fodder();
                        }
                        lhs = new Apply(span(begin, end),
                                        begin_fodder,
                                        lhs,
                                        op.getFodder(),
                                        args,
                                        got_comma,
                                        end.getFodder(),
                                        tailstrict_fodder,
                                        tailstrict);

                    } else if (op.getKind() == Token.Kind.BRACE_L) {
                        final ParseObjectRemainderContext parseObjectRemainderContext = new ParseObjectRemainderContext();
                        final Token end = parseObjectRemainder(parseObjectRemainderContext, op);
                        final AST obj = parseObjectRemainderContext.obj;
                        lhs = new ApplyBrace(span(begin, end), begin_fodder, lhs, obj);

                    } else if (op.getKind() == Token.Kind.IN) {
                        if (this.peek().getKind() == Token.Kind.SUPER) {
                            final Token super_ = this.pop();
                            lhs = new InSuper(
                                span(begin, super_), begin_fodder, lhs, op.getFodder(), super_.getFodder());
                        } else {
                            final AST rhs = this.parse(precedence - 1);
                            lhs = new Binary(
                                span(begin, rhs), begin_fodder, lhs, op.getFodder(), bop, rhs);
                        }

                    } else {
                        // Logical / arithmetic binary operator.
                        assert(op.getKind() == Token.Kind.OPERATOR);
                        final AST rhs = this.parse(precedence - 1);
                        lhs = new Binary(
                            span(begin, rhs), begin_fodder, lhs, op.getFodder(), bop, rhs);
                    }

                    begin_fodder.clear();
                }
        }
    }

    public Token getRemainingFirstToken() {
        if (this.tokens.isEmpty()) {
            return null;
        }
        return this.tokens.get(0);
    }

    private static final DecimalFormat INTEGRAL_FORMAT = new DecimalFormat("#0");
    private static final DecimalFormat FLOATING_FORMAT = new DecimalFormat("#0.################");
}
