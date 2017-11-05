class FixIndentation {
    FmtOpts opts;
    unsigned column;

   public:
    FixIndentation(const FmtOpts &opts) : opts(opts), column(0) {}

    /* Set the indentation on the fodder elements, adjust column counter as if it was printed.
     * \param fodder The fodder to pretend to print.
     * \param space_before Whether a space should be printed before any other output.
     * \param separate_token If the last fodder was an interstitial, whether a space should follow
     * it.
     * \param all_but_last_indent New indentation value for all but final fodder element.
     * \param last_indent New indentation value for the final fodder element.
     */
    void fill(Fodder &fodder, bool space_before, bool separate_token, unsigned all_but_last_indent,
              unsigned last_indent)
    {
        setIndents(fodder, all_but_last_indent, last_indent);
        fodder_count(column, fodder, space_before, separate_token);
    }

    void fill(Fodder &fodder, bool space_before, bool separate_token, unsigned indent)
    {
        fill(fodder, space_before, separate_token, indent, indent);
    }

    /* This struct is the representation of the indentation level.  The field lineUp is what is
     * generally used to indent after a new line.  The field base is used to help derive a new
     * Indent struct when the indentation level increases.  lineUp is generally > base.
     *
     * In the following case (where spaces are replaced with underscores):
     * ____foobar(1,
     * ___________2)
     *
     * At the AST representing the 2, the indent has base == 4 and lineUp == 11.
     */
    struct Indent {
        unsigned base;
        unsigned lineUp;
        Indent(unsigned base, unsigned line_up) : base(base), lineUp(line_up) {}
    };

    /** Calculate the indentation of sub-expressions.
     *
     * If the first sub-expression is on the same line as the current node, then subsequent
     * ones will be lined up, otherwise subsequent ones will be on the next line indented
     * by 'indent'.
     */
    Indent newIndent(const Fodder &first_fodder, const Indent &old, unsigned line_up)
    {
        if (first_fodder.size() == 0 || first_fodder[0].kind == FodderElement::INTERSTITIAL) {
            return Indent(old.base, line_up);
        } else {
            // Reset
            return Indent(old.base + opts.indent, old.base + opts.indent);
        }
    }

    /** Calculate the indentation of sub-expressions.
     *
     * If the first sub-expression is on the same line as the current node, then subsequent
     * ones will be lined up and further indentations in their subexpressions will be based from
     * this column.
     */
    Indent newIndentStrong(const Fodder &first_fodder, const Indent &old, unsigned line_up)
    {
        if (first_fodder.size() == 0 || first_fodder[0].kind == FodderElement::INTERSTITIAL) {
            return Indent(line_up, line_up);
        } else {
            // Reset
            return Indent(old.base + opts.indent, old.base + opts.indent);
        }
    }

    /** Calculate the indentation of sub-expressions.
     *
     * If the first sub-expression is on the same line as the current node, then subsequent
     * ones will be lined up, otherwise subseqeuent ones will be on the next line with no
     * additional indent.
     */
    Indent align(const Fodder &first_fodder, const Indent &old, unsigned line_up)
    {
        if (first_fodder.size() == 0 || first_fodder[0].kind == FodderElement::INTERSTITIAL) {
            return Indent(old.base, line_up);
        } else {
            // Reset
            return old;
        }
    }

    /** Calculate the indentation of sub-expressions.
     *
     * If the first sub-expression is on the same line as the current node, then subsequent
     * ones will be lined up and further indentations in their subexpresssions will be based from
     * this column.  Otherwise, subseqeuent ones will be on the next line with no
     * additional indent.
     */
    Indent alignStrong(const Fodder &first_fodder, const Indent &old, unsigned line_up)
    {
        if (first_fodder.size() == 0 || first_fodder[0].kind == FodderElement::INTERSTITIAL) {
            return Indent(line_up, line_up);
        } else {
            // Reset
            return old;
        }
    }

    /* Set indentation values within the fodder elements.
     *
     * The last one gets a special indentation value, all the others are set to the same thing.
     */
    void setIndents(Fodder &fodder, unsigned all_but_last_indent, unsigned last_indent)
    {
        // First count how many there are.
        unsigned count = 0;
        for (const auto &f : fodder) {
            if (f.kind != FodderElement::INTERSTITIAL)
                count++;
        }
        // Now set the indents.
        unsigned i = 0;
        for (auto &f : fodder) {
            if (f.kind != FodderElement::INTERSTITIAL) {
                if (i + 1 < count) {
                    f.indent = all_but_last_indent;
                } else {
                    assert(i == count - 1);
                    f.indent = last_indent;
                }
                i++;
            }
        }
    }

    /** Indent comprehension specs.
     * \param indent The indentation level.
     */
    void specs(std::vector<ComprehensionSpec> &specs, const Indent &indent)
    {
        for (auto &spec : specs) {
            fill(spec.openFodder, true, true, indent.lineUp);
            switch (spec.kind) {
                case ComprehensionSpec::FOR: {
                    column += 3;  // for
                    fill(spec.varFodder, true, true, indent.lineUp);
                    column += spec.var->name.length();
                    fill(spec.inFodder, true, true, indent.lineUp);
                    column += 2;  // in
                    Indent new_indent = newIndent(open_fodder(spec.expr), indent, column);
                    expr(spec.expr, new_indent, true);
                } break;

                case ComprehensionSpec::IF: {
                    column += 2;  // if
                    Indent new_indent = newIndent(open_fodder(spec.expr), indent, column);
                    expr(spec.expr, new_indent, true);
                } break;
            }
        }
    }

    void params(Fodder &fodder_l, ArgParams &params, bool trailing_comma, Fodder &fodder_r,
                const Indent &indent)
    {
        fill(fodder_l, false, false, indent.lineUp, indent.lineUp);
        column++;  // (
        const Fodder &first_inside = params.size() == 0 ? fodder_r : params[0].idFodder;

        Indent new_indent = newIndent(first_inside, indent, column);
        bool first = true;
        for (auto &param : params) {
            if (!first)
                column++;  // ','
            fill(param.idFodder, !first, true, new_indent.lineUp);
            column += param.id->name.length();
            if (param.expr != nullptr) {
                // default arg, no spacing: x=e
                fill(param.eqFodder, false, false, new_indent.lineUp);
                column++;
                expr(param.expr, new_indent, false);
            }
            fill(param.commaFodder, false, false, new_indent.lineUp);
            first = false;
        }
        if (trailing_comma)
            column++;
        fill(fodder_r, false, false, new_indent.lineUp, indent.lineUp);
        column++;  // )
    }

    void fieldParams(ObjectField &field, const Indent &indent)
    {
        if (field.methodSugar) {
            params(field.fodderL, field.params, field.trailingComma, field.fodderR, indent);
        }
    }

    /** Indent fields within an object.
     *
     * \params fields
     * \param indent Indent of the first field.
     * \param space_before
     */
    void fields(ObjectFields &fields, const Indent &indent, bool space_before)
    {
        unsigned new_indent = indent.lineUp;
        bool first = true;
        for (auto &field : fields) {
            if (!first)
                column++;  // ','

            switch (field.kind) {
                case ObjectField::LOCAL: {
                    fill(field.fodder1, !first || space_before, true, indent.lineUp);
                    column += 5;  // local
                    fill(field.fodder2, true, true, indent.lineUp);
                    column += field.id->name.length();
                    fieldParams(field, indent);
                    fill(field.opFodder, true, true, indent.lineUp);
                    column++;  // =
                    Indent new_indent2 = newIndent(open_fodder(field.expr2), indent, column);
                    expr(field.expr2, new_indent2, true);
                } break;

                case ObjectField::FIELD_ID:
                case ObjectField::FIELD_STR:
                case ObjectField::FIELD_EXPR: {
                    if (field.kind == ObjectField::FIELD_ID) {
                        fill(field.fodder1, !first || space_before, true, new_indent);
                        column += field.id->name.length();

                    } else if (field.kind == ObjectField::FIELD_STR) {
                        expr(field.expr1, indent, !first || space_before);

                    } else if (field.kind == ObjectField::FIELD_EXPR) {
                        fill(field.fodder1, !first || space_before, true, new_indent);
                        column++;  // [
                        expr(field.expr1, indent, false);
                        fill(field.fodder2, false, false, new_indent);
                        column++;  // ]
                    }

                    fieldParams(field, indent);

                    fill(field.opFodder, false, false, new_indent);

                    if (field.superSugar)
                        column++;
                    switch (field.hide) {
                        case ObjectField::INHERIT: column += 1; break;
                        case ObjectField::HIDDEN: column += 2; break;
                        case ObjectField::VISIBLE: column += 3; break;
                    }
                    Indent new_indent2 = newIndent(open_fodder(field.expr2), indent, column);
                    expr(field.expr2, new_indent2, true);

                } break;

                case ObjectField::ASSERT: {
                    fill(field.fodder1, !first || space_before, true, new_indent);
                    column += 6;  // assert
                    // + 1 for the space after the assert
                    Indent new_indent2 = newIndent(open_fodder(field.expr2), indent, column + 1);
                    expr(field.expr2, indent, true);
                    if (field.expr3 != nullptr) {
                        fill(field.opFodder, true, true, new_indent2.lineUp);
                        column++;  // ":"
                        expr(field.expr3, new_indent2, true);
                    }
                } break;
            }

            first = false;
            fill(field.commaFodder, false, false, new_indent);
        }
    }

    /** Does the given fodder contain at least one new line? */
    bool hasNewLines(const Fodder &fodder)
    {
        for (const auto &f : fodder) {
            if (f.kind != FodderElement::INTERSTITIAL)
                return true;
        }
        return false;
    }

    /** Get the first fodder from an ArgParam. */
    const Fodder &argParamFirstFodder(const ArgParam &ap)
    {
        if (ap.id != nullptr)
            return ap.idFodder;
        return open_fodder(ap.expr);
    }

    /** Reindent an expression.
     *
     * \param ast_ The ast to reindent.
     * \param indent Beginning of the line.
     * \param space_before As defined in the pretty-printer.
     */
    void expr(AST *ast_, const Indent &indent, bool space_before)
    {
        fill(ast_->openFodder, space_before, !left_recursive(ast_), indent.lineUp);

        if (auto *ast = dynamic_cast<Apply *>(ast_)) {
            const Fodder &init_fodder = open_fodder(ast->target);
            Indent new_indent = align(init_fodder, indent, column + (space_before ? 1 : 0));
            expr(ast->target, new_indent, space_before);
            fill(ast->fodderL, false, false, new_indent.lineUp);
            column++;  // (
            const Fodder &first_fodder =
                ast->args.size() == 0 ? ast->fodderR : argParamFirstFodder(ast->args[0]);
            bool strong_indent = false;
            // Need to use strong indent if any of the
            // arguments (except the first) are preceded by newlines.
            bool first = true;
            for (auto &arg : ast->args) {
                if (first) {
                    // Skip first element.
                    first = false;
                    continue;
                }
                if (hasNewLines(argParamFirstFodder(arg)))
                    strong_indent = true;
            }

            Indent arg_indent = strong_indent ? newIndentStrong(first_fodder, indent, column)
                                              : newIndent(first_fodder, indent, column);
            first = true;
            for (auto &arg : ast->args) {
                if (!first)
                    column++;  // ","

                bool space = !first;
                if (arg.id != nullptr) {
                    fill(arg.idFodder, space, false, arg_indent.lineUp);
                    column += arg.id->name.length();
                    space = false;
                    column++;  // "="
                }
                expr(arg.expr, arg_indent, space);
                fill(arg.commaFodder, false, false, arg_indent.lineUp);
                first = false;
            }
            if (ast->trailingComma)
                column++;  // ","
            fill(ast->fodderR, false, false, arg_indent.lineUp, indent.base);
            column++;  // )
            if (ast->tailstrict) {
                fill(ast->tailstrictFodder, true, true, indent.base);
                column += 10;  // tailstrict
            }

        } else if (auto *ast = dynamic_cast<ApplyBrace *>(ast_)) {
            const Fodder &init_fodder = open_fodder(ast->left);
            Indent new_indent = align(init_fodder, indent, column + (space_before ? 1 : 0));
            expr(ast->left, new_indent, space_before);
            expr(ast->right, new_indent, true);

        } else if (auto *ast = dynamic_cast<Array *>(ast_)) {
            column++;  // '['
            // First fodder element exists and is a newline
            const Fodder &first_fodder =
                ast->elements.size() > 0 ? open_fodder(ast->elements[0].expr) : ast->closeFodder;
            unsigned new_column = column + (opts.padArrays ? 1 : 0);
            bool strong_indent = false;
            // Need to use strong indent if there are not newlines before any of the sub-expressions
            bool first = true;
            for (auto &el : ast->elements) {
                if (first) {
                    first = false;
                    continue;
                }
                if (hasNewLines(open_fodder(el.expr)))
                    strong_indent = true;
            }

            Indent new_indent = strong_indent ? newIndentStrong(first_fodder, indent, new_column)
                                              : newIndent(first_fodder, indent, new_column);

            first = true;
            for (auto &element : ast->elements) {
                if (!first)
                    column++;
                expr(element.expr, new_indent, !first || opts.padArrays);
                fill(element.commaFodder, false, false, new_indent.lineUp, new_indent.lineUp);
                first = false;
            }
            if (ast->trailingComma)
                column++;

            // Handle penultimate newlines from expr.close_fodder if there are any.
            fill(ast->closeFodder,
                 ast->elements.size() > 0,
                 opts.padArrays,
                 new_indent.lineUp,
                 indent.base);
            column++;  // ']'

        } else if (auto *ast = dynamic_cast<ArrayComprehension *>(ast_)) {
            column++;  // [
            Indent new_indent =
                newIndent(open_fodder(ast->body), indent, column + (opts.padArrays ? 1 : 0));
            expr(ast->body, new_indent, opts.padArrays);
            fill(ast->commaFodder, false, false, new_indent.lineUp);
            if (ast->trailingComma)
                column++;  // ','
            specs(ast->specs, new_indent);
            fill(ast->closeFodder, true, opts.padArrays, new_indent.lineUp, indent.base);
            column++;  // ]

        } else if (auto *ast = dynamic_cast<Assert *>(ast_)) {
            column += 6;  // assert
            // + 1 for the space after the assert
            Indent new_indent = newIndent(open_fodder(ast->cond), indent, column + 1);
            expr(ast->cond, new_indent, true);
            if (ast->message != nullptr) {
                fill(ast->colonFodder, true, true, new_indent.lineUp);
                column++;  // ":"
                expr(ast->message, new_indent, true);
            }
            fill(ast->semicolonFodder, false, false, new_indent.lineUp);
            column++;  // ";"
            expr(ast->rest, indent, true);

        } else if (auto *ast = dynamic_cast<Binary *>(ast_)) {
            const Fodder &first_fodder = open_fodder(ast->left);

            // Need to use strong indent in the case of
            /*
            A
            + B
            or
            A +
            B
            */
            bool strong_indent = hasNewLines(ast->opFodder) || hasNewLines(open_fodder(ast->right));

            unsigned inner_column = column + (space_before ? 1 : 0);
            Indent new_indent = strong_indent ? alignStrong(first_fodder, indent, inner_column)
                                              : align(first_fodder, indent, inner_column);
            expr(ast->left, new_indent, space_before);
            fill(ast->opFodder, true, true, new_indent.lineUp);
            column += bop_string(ast->op).length();
            // Don't calculate a new indent for here, because we like being able to do:
            // true &&
            // true &&
            // true
            expr(ast->right, new_indent, true);

        } else if (auto *ast = dynamic_cast<BuiltinFunction *>(ast_)) {
            column += 11;  // "/* builtin "
            column += ast->name.length();
            column += 8;  // " */ null"

        } else if (auto *ast = dynamic_cast<Conditional *>(ast_)) {
            column += 2;  // if
            Indent cond_indent = newIndent(open_fodder(ast->cond), indent, column + 1);
            expr(ast->cond, cond_indent, true);
            fill(ast->thenFodder, true, true, indent.base);
            column += 4;  // then
            Indent true_indent = newIndent(open_fodder(ast->branchTrue), indent, column + 1);
            expr(ast->branchTrue, true_indent, true);
            if (ast->branchFalse != nullptr) {
                fill(ast->elseFodder, true, true, indent.base);
                column += 4;  // else
                Indent false_indent = newIndent(open_fodder(ast->branchFalse), indent, column + 1);
                expr(ast->branchFalse, false_indent, true);
            }

        } else if (dynamic_cast<Dollar *>(ast_)) {
            column++;  // $

        } else if (auto *ast = dynamic_cast<Error *>(ast_)) {
            column += 5;  // error
            Indent new_indent = newIndent(open_fodder(ast->expr), indent, column + 1);
            expr(ast->expr, new_indent, true);

        } else if (auto *ast = dynamic_cast<Function *>(ast_)) {
            column += 8;  // function
            params(ast->parenLeftFodder,
                   ast->params,
                   ast->trailingComma,
                   ast->parenRightFodder,
                   indent);
            Indent new_indent = newIndent(open_fodder(ast->body), indent, column + 1);
            expr(ast->body, new_indent, true);

        } else if (auto *ast = dynamic_cast<Import *>(ast_)) {
            column += 6;  // import
            Indent new_indent = newIndent(open_fodder(ast->file), indent, column + 1);
            expr(ast->file, new_indent, true);

        } else if (auto *ast = dynamic_cast<Importstr *>(ast_)) {
            column += 9;  // importstr
            Indent new_indent = newIndent(open_fodder(ast->file), indent, column + 1);
            expr(ast->file, new_indent, true);

        } else if (auto *ast = dynamic_cast<InSuper *>(ast_)) {
            expr(ast->element, indent, space_before);
            fill(ast->inFodder, true, true, indent.lineUp);
            column += 2;  // in
            fill(ast->superFodder, true, true, indent.lineUp);
            column += 5;  // super

        } else if (auto *ast = dynamic_cast<Index *>(ast_)) {
            expr(ast->target, indent, space_before);
            fill(ast->dotFodder, false, false, indent.lineUp);
            if (ast->id != nullptr) {
                Indent new_indent = newIndent(ast->idFodder, indent, column);
                column++;  // "."
                fill(ast->idFodder, false, false, new_indent.lineUp);
                column += ast->id->name.length();
            } else {
                column++;  // "["
                if (ast->isSlice) {
                    Indent new_indent(0, 0);
                    if (ast->index != nullptr) {
                        new_indent = newIndent(open_fodder(ast->index), indent, column);
                        expr(ast->index, new_indent, false);
                    }
                    if (ast->end != nullptr) {
                        new_indent = newIndent(ast->endColonFodder, indent, column);
                        fill(ast->endColonFodder, false, false, new_indent.lineUp);
                        column++;  // ":"
                        expr(ast->end, new_indent, false);
                    }
                    if (ast->step != nullptr) {
                        if (ast->end == nullptr) {
                            new_indent = newIndent(ast->endColonFodder, indent, column);
                            fill(ast->endColonFodder, false, false, new_indent.lineUp);
                            column++;  // ":"
                        }
                        fill(ast->stepColonFodder, false, false, new_indent.lineUp);
                        column++;  // ":"
                        expr(ast->step, new_indent, false);
                    }
                    if (ast->index == nullptr && ast->end == nullptr && ast->step == nullptr) {
                        new_indent = newIndent(ast->endColonFodder, indent, column);
                        fill(ast->endColonFodder, false, false, new_indent.lineUp);
                        column++;  // ":"
                    }
                } else {
                    Indent new_indent = newIndent(open_fodder(ast->index), indent, column);
                    expr(ast->index, new_indent, false);
                    fill(ast->idFodder, false, false, new_indent.lineUp, indent.base);
                }
                column++;  // "]"
            }

        } else if (auto *ast = dynamic_cast<Local *>(ast_)) {
            column += 5;  // local
            assert(ast->binds.size() > 0);
            bool first = true;
            Indent new_indent = newIndent(ast->binds[0].varFodder, indent, column + 1);
            for (auto &bind : ast->binds) {
                if (!first)
                    column++;  // ','
                first = false;
                fill(bind.varFodder, true, true, new_indent.lineUp);
                column += bind.var->name.length();
                if (bind.functionSugar) {
                    params(bind.parenLeftFodder,
                           bind.params,
                           bind.trailingComma,
                           bind.parenRightFodder,
                           new_indent);
                }
                fill(bind.opFodder, true, true, new_indent.lineUp);
                column++;  // '='
                Indent new_indent2 = newIndent(open_fodder(bind.body), new_indent, column + 1);
                expr(bind.body, new_indent2, true);
                fill(bind.closeFodder, false, false, new_indent2.lineUp, indent.base);
            }
            column++;  // ';'
            expr(ast->body, indent, true);

        } else if (auto *ast = dynamic_cast<LiteralBoolean *>(ast_)) {
            column += (ast->value ? 4 : 5);

        } else if (auto *ast = dynamic_cast<LiteralNumber *>(ast_)) {
            column += ast->originalString.length();

        } else if (auto *ast = dynamic_cast<LiteralString *>(ast_)) {
            if (ast->tokenKind == LiteralString::DOUBLE) {
                column += 2 + ast->value.length();  // Include quotes
            } else if (ast->tokenKind == LiteralString::SINGLE) {
                column += 2 + ast->value.length();  // Include quotes
            } else if (ast->tokenKind == LiteralString::BLOCK) {
                ast->blockIndent = std::string(indent.base + opts.indent, ' ');
                ast->blockTermIndent = std::string(indent.base, ' ');
                column = indent.base;  // blockTermIndent
                column += 3;           // "|||"
            } else if (ast->tokenKind == LiteralString::VERBATIM_SINGLE) {
                column += 3;  // Include @, start and end quotes
                for (const char32_t *cp = ast->value.c_str(); *cp != U'\0'; ++cp) {
                    if (*cp == U'\'') {
                        column += 2;
                    } else {
                        column += 1;
                    }
                }
            } else if (ast->tokenKind == LiteralString::VERBATIM_DOUBLE) {
                column += 3;  // Include @, start and end quotes
                for (const char32_t *cp = ast->value.c_str(); *cp != U'\0'; ++cp) {
                    if (*cp == U'"') {
                        column += 2;
                    } else {
                        column += 1;
                    }
                }
            }

        } else if (dynamic_cast<LiteralNull *>(ast_)) {
            column += 4;  // null

        } else if (auto *ast = dynamic_cast<Object *>(ast_)) {
            column++;  // '{'
            const Fodder &first_fodder = ast->fields.size() == 0
                                             ? ast->closeFodder
                                             : ast->fields[0].kind == ObjectField::FIELD_STR
                                                   ? open_fodder(ast->fields[0].expr1)
                                                   : ast->fields[0].fodder1;
            Indent new_indent = newIndent(first_fodder, indent, column + (opts.padObjects ? 1 : 0));

            fields(ast->fields, new_indent, opts.padObjects);
            if (ast->trailingComma)
                column++;
            fill(ast->closeFodder,
                 ast->fields.size() > 0,
                 opts.padObjects,
                 new_indent.lineUp,
                 indent.base);
            column++;  // '}'

        } else if (auto *ast = dynamic_cast<DesugaredObject *>(ast_)) {
            // No fodder but need to recurse and maintain column counter.
            column++;  // '{'
            for (AST *assert : ast->asserts) {
                column += 6;  // assert
                expr(assert, indent, true);
                column++;  // ','
            }
            for (auto &field : ast->fields) {
                column++;  // '['
                expr(field.name, indent, false);
                column++;  // ']'
                switch (field.hide) {
                    case ObjectField::INHERIT: column += 1; break;
                    case ObjectField::HIDDEN: column += 2; break;
                    case ObjectField::VISIBLE: column += 3; break;
                }
                expr(field.body, indent, true);
            }
            column++;  // '}'

        } else if (auto *ast = dynamic_cast<ObjectComprehension *>(ast_)) {
            column++;  // '{'
            unsigned start_column = column;
            const Fodder &first_fodder = ast->fields.size() == 0
                                             ? ast->closeFodder
                                             : ast->fields[0].kind == ObjectField::FIELD_STR
                                                   ? open_fodder(ast->fields[0].expr1)
                                                   : ast->fields[0].fodder1;
            Indent new_indent =
                newIndent(first_fodder, indent, start_column + (opts.padObjects ? 1 : 0));

            fields(ast->fields, new_indent, opts.padObjects);
            if (ast->trailingComma)
                column++;  // ','
            specs(ast->specs, new_indent);
            fill(ast->closeFodder, true, opts.padObjects, new_indent.lineUp, indent.base);
            column++;  // '}'

        } else if (auto *ast = dynamic_cast<ObjectComprehensionSimple *>(ast_)) {
            column++;  // '{'
            column++;  // '['
            expr(ast->field, indent, false);
            column++;  // ']'
            column++;  // ':'
            expr(ast->value, indent, true);
            column += 5;  // " for "
            column += ast->id->name.length();
            column += 3;  // " in"
            expr(ast->array, indent, true);
            column++;  // '}'

        } else if (auto *ast = dynamic_cast<Parens *>(ast_)) {
            column++;  // (
            Indent new_indent = newIndentStrong(open_fodder(ast->expr), indent, column);
            expr(ast->expr, new_indent, false);
            fill(ast->closeFodder, false, false, new_indent.lineUp, indent.base);
            column++;  // )

        } else if (dynamic_cast<const Self *>(ast_)) {
            column += 4;  // self

        } else if (auto *ast = dynamic_cast<SuperIndex *>(ast_)) {
            column += 5;  // super
            fill(ast->dotFodder, false, false, indent.lineUp);
            if (ast->id != nullptr) {
                column++;  // ".";
                Indent new_indent = newIndent(ast->idFodder, indent, column);
                fill(ast->idFodder, false, false, new_indent.lineUp);
                column += ast->id->name.length();
            } else {
                column++;  // "[";
                Indent new_indent = newIndent(open_fodder(ast->index), indent, column);
                expr(ast->index, new_indent, false);
                fill(ast->idFodder, false, false, new_indent.lineUp, indent.base);
                column++;  // "]";
            }

        } else if (auto *ast = dynamic_cast<Unary *>(ast_)) {
            column += uop_string(ast->op).length();
            Indent new_indent = newIndent(open_fodder(ast->expr), indent, column);
            expr(ast->expr, new_indent, false);

        } else if (auto *ast = dynamic_cast<Var *>(ast_)) {
            column += ast->id->name.length();

        } else {
            std::cerr << "INTERNAL ERROR: Unknown AST: " << ast_ << std::endl;
            std::abort();
        }
    }
    virtual void file(AST *body, Fodder &final_fodder)
    {
        expr(body, Indent(0, 0), false);
        setIndents(final_fodder, 0, 0);
    }
};
