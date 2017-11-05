package org.jsonnet.unparse;

import java.io.OutputStream;

public class Unparser {
    // private final std::ostream &o;
    private final OutputStream o;
    private final FmtOpts opts;

    public Unparser(final OutputStream o, final FmtOpts opts) {
        this.o = o;
        this.opts = opts;
    }

    public void unparseSpecs(const std::vector<ComprehensionSpec> &specs) {
        for (const auto &spec : specs) {
            fill(spec.openFodder, true, true);
            switch (spec.kind) {
                case ComprehensionSpec::FOR:
                    o << "for";
                    fill(spec.varFodder, true, true);
                    o << unparse_id(spec.var);
                    fill(spec.inFodder, true, true);
                    o << "in";
                    unparse(spec.expr, true);
                    break;
                case ComprehensionSpec::IF:
                    o << "if";
                    unparse(spec.expr, true);
                    break;
            }
        }
    }

    public void fill(const Fodder &fodder, bool space_before, bool separate_token)
    {
        fodder_fill(o, fodder, space_before, separate_token);
    }

    public void unparseParams(const Fodder &fodder_l, const ArgParams &params, bool trailing_comma,
                       const Fodder &fodder_r)
    {
        fill(fodder_l, false, false);
        o << "(";
        bool first = true;
        for (const auto &param : params) {
            if (!first)
                o << ",";
            fill(param.idFodder, !first, true);
            o << unparse_id(param.id);
            if (param.expr != nullptr) {
                // default arg, no spacing: x=e
                fill(param.eqFodder, false, false);
                o << "=";
                unparse(param.expr, false);
            }
            fill(param.commaFodder, false, false);
            first = false;
        }
        if (trailing_comma)
            o << ",";
        fill(fodder_r, false, false);
        o << ")";
    }

    public void unparseFieldParams(const ObjectField &field)
    {
        if (field.methodSugar) {
            unparseParams(field.fodderL, field.params, field.trailingComma, field.fodderR);
        }
    }

    public void unparseFields(const ObjectFields &fields, bool space_before)
    {
        bool first = true;
        for (const auto &field : fields) {
            if (!first)
                o << ',';

            switch (field.kind) {
                case ObjectField::LOCAL: {
                    fill(field.fodder1, !first || space_before, true);
                    o << "local";
                    fill(field.fodder2, true, true);
                    o << unparse_id(field.id);
                    unparseFieldParams(field);
                    fill(field.opFodder, true, true);
                    o << "=";
                    unparse(field.expr2, true);
                } break;

                case ObjectField::FIELD_ID:
                case ObjectField::FIELD_STR:
                case ObjectField::FIELD_EXPR: {
                    if (field.kind == ObjectField::FIELD_ID) {
                        fill(field.fodder1, !first || space_before, true);
                        o << unparse_id(field.id);

                    } else if (field.kind == ObjectField::FIELD_STR) {
                        unparse(field.expr1, !first || space_before);

                    } else if (field.kind == ObjectField::FIELD_EXPR) {
                        fill(field.fodder1, !first || space_before, true);
                        o << "[";
                        unparse(field.expr1, false);
                        fill(field.fodder2, false, false);
                        o << "]";
                    }
                    unparseFieldParams(field);

                    fill(field.opFodder, false, false);

                    if (field.superSugar)
                        o << "+";
                    switch (field.hide) {
                        case ObjectField::INHERIT: o << ":"; break;
                        case ObjectField::HIDDEN: o << "::"; break;
                        case ObjectField::VISIBLE: o << ":::"; break;
                    }
                    unparse(field.expr2, true);

                } break;

                case ObjectField::ASSERT: {
                    fill(field.fodder1, !first || space_before, true);
                    o << "assert";
                    unparse(field.expr2, true);
                    if (field.expr3 != nullptr) {
                        fill(field.opFodder, true, true);
                        o << ":";
                        unparse(field.expr3, true);
                    }
                } break;
            }

            first = false;
            fill(field.commaFodder, false, false);
        }
    }

    /** Unparse the given AST.
     *
     * \param ast_ The AST to be unparsed.
     *
     * \param precedence The precedence of the enclosing AST.  If this is greater than the current
     * precedence, parens are not needed.
     */
    public void unparse(const AST *ast_, bool space_before)
    {
        bool separate_token = !left_recursive(ast_);

        fill(ast_->openFodder, space_before, separate_token);

        if (auto *ast = dynamic_cast<const Apply *>(ast_)) {
            unparse(ast->target, space_before);
            fill(ast->fodderL, false, false);
            o << "(";
            bool first = true;
            for (const auto &arg : ast->args) {
                if (!first)
                    o << ',';
                bool space = !first;
                if (arg.id != nullptr) {
                    fill(arg.idFodder, space, true);
                    o << unparse_id(arg.id);
                    space = false;
                    o << "=";
                }
                unparse(arg.expr, space);
                fill(arg.commaFodder, false, false);
                first = false;
            }
            if (ast->trailingComma)
                o << ",";
            fill(ast->fodderR, false, false);
            o << ")";
            if (ast->tailstrict) {
                fill(ast->tailstrictFodder, true, true);
                o << "tailstrict";
            }

        } else if (auto *ast = dynamic_cast<const ApplyBrace *>(ast_)) {
            unparse(ast->left, space_before);
            unparse(ast->right, true);

        } else if (auto *ast = dynamic_cast<const Array *>(ast_)) {
            o << "[";
            bool first = true;
            for (const auto &element : ast->elements) {
                if (!first)
                    o << ',';
                unparse(element.expr, !first || opts.padArrays);
                fill(element.commaFodder, false, false);
                first = false;
            }
            if (ast->trailingComma)
                o << ",";
            fill(ast->closeFodder, ast->elements.size() > 0, opts.padArrays);
            o << "]";

        } else if (auto *ast = dynamic_cast<const ArrayComprehension *>(ast_)) {
            o << "[";
            unparse(ast->body, opts.padArrays);
            fill(ast->commaFodder, false, false);
            if (ast->trailingComma)
                o << ",";
            unparseSpecs(ast->specs);
            fill(ast->closeFodder, true, opts.padArrays);
            o << "]";

        } else if (auto *ast = dynamic_cast<const Assert *>(ast_)) {
            o << "assert";
            unparse(ast->cond, true);
            if (ast->message != nullptr) {
                fill(ast->colonFodder, true, true);
                o << ":";
                unparse(ast->message, true);
            }
            fill(ast->semicolonFodder, false, false);
            o << ";";
            unparse(ast->rest, true);

        } else if (auto *ast = dynamic_cast<const Binary *>(ast_)) {
            unparse(ast->left, space_before);
            fill(ast->opFodder, true, true);
            o << bop_string(ast->op);
            // The - 1 is for left associativity.
            unparse(ast->right, true);

        } else if (auto *ast = dynamic_cast<const BuiltinFunction *>(ast_)) {
            o << "/* builtin " << ast->name << " */ null";

        } else if (auto *ast = dynamic_cast<const Conditional *>(ast_)) {
            o << "if";
            unparse(ast->cond, true);
            fill(ast->thenFodder, true, true);
            o << "then";
            if (ast->branchFalse != nullptr) {
                unparse(ast->branchTrue, true);
                fill(ast->elseFodder, true, true);
                o << "else";
                unparse(ast->branchFalse, true);
            } else {
                unparse(ast->branchTrue, true);
            }

        } else if (dynamic_cast<const Dollar *>(ast_)) {
            o << "$";

        } else if (auto *ast = dynamic_cast<const Error *>(ast_)) {
            o << "error";
            unparse(ast->expr, true);

        } else if (auto *ast = dynamic_cast<const Function *>(ast_)) {
            o << "function";
            unparseParams(
                ast->parenLeftFodder, ast->params, ast->trailingComma, ast->parenRightFodder);
            unparse(ast->body, true);

        } else if (auto *ast = dynamic_cast<const Import *>(ast_)) {
            o << "import";
            unparse(ast->file, true);

        } else if (auto *ast = dynamic_cast<const Importstr *>(ast_)) {
            o << "importstr";
            unparse(ast->file, true);

        } else if (auto *ast = dynamic_cast<const InSuper *>(ast_)) {
            unparse(ast->element, true);
            fill(ast->inFodder, true, true);
            o << "in";
            fill(ast->superFodder, true, true);
            o << "super";

        } else if (auto *ast = dynamic_cast<const Index *>(ast_)) {
            unparse(ast->target, space_before);
            fill(ast->dotFodder, false, false);
            if (ast->id != nullptr) {
                o << ".";
                fill(ast->idFodder, false, false);
                o << unparse_id(ast->id);
            } else {
                o << "[";
                if (ast->isSlice) {
                    if (ast->index != nullptr) {
                        unparse(ast->index, false);
                    }
                    fill(ast->endColonFodder, false, false);
                    o << ":";
                    if (ast->end != nullptr) {
                        unparse(ast->end, false);
                    }
                    if (ast->step != nullptr || ast->stepColonFodder.size() > 0) {
                        fill(ast->stepColonFodder, false, false);
                        o << ":";
                        if (ast->step != nullptr) {
                            unparse(ast->step, false);
                        }
                    }
                } else {
                    unparse(ast->index, false);
                }
                fill(ast->idFodder, false, false);
                o << "]";
            }

        } else if (auto *ast = dynamic_cast<const Local *>(ast_)) {
            o << "local";
            assert(ast->binds.size() > 0);
            bool first = true;
            for (const auto &bind : ast->binds) {
                if (!first)
                    o << ",";
                first = false;
                fill(bind.varFodder, true, true);
                o << unparse_id(bind.var);
                if (bind.functionSugar) {
                    unparseParams(bind.parenLeftFodder,
                                  bind.params,
                                  bind.trailingComma,
                                  bind.parenRightFodder);
                }
                fill(bind.opFodder, true, true);
                o << "=";
                unparse(bind.body, true);
                fill(bind.closeFodder, false, false);
            }
            o << ";";
            unparse(ast->body, true);

        } else if (auto *ast = dynamic_cast<const LiteralBoolean *>(ast_)) {
            o << (ast->value ? "true" : "false");

        } else if (auto *ast = dynamic_cast<const LiteralNumber *>(ast_)) {
            o << ast->originalString;

        } else if (auto *ast = dynamic_cast<const LiteralString *>(ast_)) {
            if (ast->tokenKind == LiteralString::DOUBLE) {
                o << "\"";
                o << encode_utf8(ast->value);
                o << "\"";
            } else if (ast->tokenKind == LiteralString::SINGLE) {
                o << "'";
                o << encode_utf8(ast->value);
                o << "'";
            } else if (ast->tokenKind == LiteralString::BLOCK) {
                o << "|||\n";
                if (ast->value.c_str()[0] != U'\n')
                    o << ast->blockIndent;
                for (const char32_t *cp = ast->value.c_str(); *cp != U'\0'; ++cp) {
                    std::string utf8;
                    encode_utf8(*cp, utf8);
                    o << utf8;
                    if (*cp == U'\n' && *(cp + 1) != U'\n' && *(cp + 1) != U'\0') {
                        o << ast->blockIndent;
                    }
                }
                o << ast->blockTermIndent << "|||";
            } else if (ast->tokenKind == LiteralString::VERBATIM_DOUBLE) {
                o << "@\"";
                for (const char32_t *cp = ast->value.c_str(); *cp != U'\0'; ++cp) {
                    if (*cp == U'"') {
                        o << "\"\"";
                    } else {
                        std::string utf8;
                        encode_utf8(*cp, utf8);
                        o << utf8;
                    }
                }
                o << "\"";
            } else if (ast->tokenKind == LiteralString::VERBATIM_SINGLE) {
                o << "@'";
                for (const char32_t *cp = ast->value.c_str(); *cp != U'\0'; ++cp) {
                    if (*cp == U'\'') {
                        o << "''";
                    } else {
                        std::string utf8;
                        encode_utf8(*cp, utf8);
                        o << utf8;
                    }
                }
                o << "'";
            }

        } else if (dynamic_cast<const LiteralNull *>(ast_)) {
            o << "null";

        } else if (auto *ast = dynamic_cast<const Object *>(ast_)) {
            o << "{";
            unparseFields(ast->fields, opts.padObjects);
            if (ast->trailingComma)
                o << ",";
            fill(ast->closeFodder, ast->fields.size() > 0, opts.padObjects);
            o << "}";

        } else if (auto *ast = dynamic_cast<const DesugaredObject *>(ast_)) {
            o << "{";
            for (AST *assert : ast->asserts) {
                o << "assert";
                unparse(assert, true);
                o << ",";
            }
            for (auto &field : ast->fields) {
                o << "[";
                unparse(field.name, false);
                o << "]";
                switch (field.hide) {
                    case ObjectField::INHERIT: o << ":"; break;
                    case ObjectField::HIDDEN: o << "::"; break;
                    case ObjectField::VISIBLE: o << ":::"; break;
                }
                unparse(field.body, true);
                o << ",";
            }
            o << "}";

        } else if (auto *ast = dynamic_cast<const ObjectComprehension *>(ast_)) {
            o << "{";
            unparseFields(ast->fields, opts.padObjects);
            if (ast->trailingComma)
                o << ",";
            unparseSpecs(ast->specs);
            fill(ast->closeFodder, true, opts.padObjects);
            o << "}";

        } else if (auto *ast = dynamic_cast<const ObjectComprehensionSimple *>(ast_)) {
            o << "{[";
            unparse(ast->field, false);
            o << "]:";
            unparse(ast->value, true);
            o << " for " << unparse_id(ast->id) << " in";
            unparse(ast->array, true);
            o << "}";

        } else if (auto *ast = dynamic_cast<const Parens *>(ast_)) {
            o << "(";
            unparse(ast->expr, false);
            fill(ast->closeFodder, false, false);
            o << ")";

        } else if (dynamic_cast<const Self *>(ast_)) {
            o << "self";

        } else if (auto *ast = dynamic_cast<const SuperIndex *>(ast_)) {
            o << "super";
            fill(ast->dotFodder, false, false);
            if (ast->id != nullptr) {
                o << ".";
                fill(ast->idFodder, false, false);
                o << unparse_id(ast->id);
            } else {
                o << "[";
                unparse(ast->index, false);
                fill(ast->idFodder, false, false);
                o << "]";
            }

        } else if (auto *ast = dynamic_cast<const Unary *>(ast_)) {
            o << uop_string(ast->op);
            if (dynamic_cast<const Dollar *>(left_recursive(ast->expr))) {
                unparse(ast->expr, true);
            } else {
                unparse(ast->expr, false);
            }

        } else if (auto *ast = dynamic_cast<const Var *>(ast_)) {
            o << encode_utf8(ast->id->name);

        } else {
            std::cerr << "INTERNAL ERROR: Unknown AST: " << ast_ << std::endl;
            std::abort();
        }
    }

    private static std::string unparse_id(const Identifier *id)
    {
        return encode_utf8(id->name);
    }
}

/** Pretty-print fodder.
 *
 * \param fodder The fodder to print
 * \param space_before Whether a space should be printed before any other output.
 * \param separate_token If the last fodder was an interstitial, whether a space should follow it.
 */
void fodder_fill(std::ostream &o, const Fodder &fodder, bool space_before, bool separate_token)
{
    unsigned last_indent = 0;
    for (const auto &fod : fodder) {
        switch (fod.kind) {
            case FodderElement::LINE_END:
                if (fod.comment.size() > 0)
                    o << "  " << fod.comment[0];
                o << '\n';
                o << std::string(fod.blanks, '\n');
                o << std::string(fod.indent, ' ');
                last_indent = fod.indent;
                space_before = false;
                break;

            case FodderElement::INTERSTITIAL:
                if (space_before)
                    o << ' ';
                o << fod.comment[0];
                space_before = true;
                break;

            case FodderElement::PARAGRAPH: {
                bool first = true;
                for (const std::string &l : fod.comment) {
                    // Do not indent empty lines (note: first line is never empty).
                    if (l.length() > 0) {
                        // First line is already indented by previous fod.
                        if (!first)
                            o << std::string(last_indent, ' ');
                        o << l;
                    }
                    o << '\n';
                    first = false;
                }
                o << std::string(fod.blanks, '\n');
                o << std::string(fod.indent, ' ');
                last_indent = fod.indent;
                space_before = false;
            } break;
        }
    }
    if (separate_token && space_before)
        o << ' ';
}



/********************************************************************************
 * The rest of this file contains transformations on the ASTs before unparsing. *
 ********************************************************************************/


/** Strip blank lines from the top of the file. */
void remove_initial_newlines(AST *ast)
{
    Fodder &f = open_fodder(ast);
    while (f.size() > 0 && f[0].kind == FodderElement::LINE_END)
        f.erase(f.begin());
}










std::string jsonnet_fmt(AST *ast, Fodder &final_fodder, const FmtOpts &opts)
{
    Allocator alloc;

    // Passes to enforce style on the AST.
    if (opts.sortImports)
        SortImports(alloc).file(ast);
    remove_initial_newlines(ast);
    if (opts.maxBlankLines > 0)
        EnforceMaximumBlankLines(alloc, opts).file(ast, final_fodder);
    FixNewlines(alloc, opts).file(ast, final_fodder);
    FixTrailingCommas(alloc, opts).file(ast, final_fodder);
    FixParens(alloc, opts).file(ast, final_fodder);
    FixPlusObject(alloc, opts).file(ast, final_fodder);
    NoRedundantSliceColon(alloc, opts).file(ast, final_fodder);
    if (opts.stripComments)
        StripComments(alloc, opts).file(ast, final_fodder);
    else if (opts.stripAllButComments)
        StripAllButComments(alloc, opts).file(ast, final_fodder);
    else if (opts.stripEverything)
        StripEverything(alloc, opts).file(ast, final_fodder);
    if (opts.prettyFieldNames)
        PrettyFieldNames(alloc, opts).file(ast, final_fodder);
    if (opts.stringStyle != 'l')
        EnforceStringStyle(alloc, opts).file(ast, final_fodder);
    if (opts.commentStyle != 'l')
        EnforceCommentStyle(alloc, opts).file(ast, final_fodder);
    if (opts.indent > 0)
        FixIndentation(opts).file(ast, final_fodder);

    std::stringstream ss;
    Unparser unparser(ss, opts);
    unparser.unparse(ast, false);
    unparser.fill(final_fodder, true, false);
    return ss.str();
}
