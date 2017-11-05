/* Ensure syntax sugar is used where possible. */
class PrettyFieldNames : public FmtPass {
    using FmtPass::visit;

   public:
    PrettyFieldNames(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}

    bool isIdentifier(const UString &str)
    {
        bool first = true;
        for (char32_t c : str) {
            if (!first && c >= '0' && c <= '9')
                continue;
            first = false;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c == '_'))
                continue;
            return false;
        }
        // Filter out keywords.
        if (lex_get_keyword_kind(encode_utf8(str)) != Token::IDENTIFIER)
            return false;
        return true;
    }

    void visit(Index *expr)
    {
        if (!expr->isSlice && expr->index != nullptr) {
            // Maybe we can use an id instead.
            if (auto *lit = dynamic_cast<LiteralString *>(expr->index)) {
                if (isIdentifier(lit->value)) {
                    expr->id = alloc.makeIdentifier(lit->value);
                    expr->idFodder = lit->openFodder;
                    expr->index = nullptr;
                }
            }
        }
        FmtPass::visit(expr);
    }

    void visit(Object *expr)
    {
        for (auto &field : expr->fields) {
            // First try ["foo"] -> "foo".
            if (field.kind == ObjectField::FIELD_EXPR) {
                if (auto *field_expr = dynamic_cast<LiteralString *>(field.expr1)) {
                    field.kind = ObjectField::FIELD_STR;
                    fodder_move_front(field_expr->openFodder, field.fodder1);
                    if (field.methodSugar) {
                        fodder_move_front(field.fodderL, field.fodder2);
                    } else {
                        fodder_move_front(field.opFodder, field.fodder2);
                    }
                }
            }
            // Then try "foo" -> foo.
            if (field.kind == ObjectField::FIELD_STR) {
                if (auto *lit = dynamic_cast<LiteralString *>(field.expr1)) {
                    if (isIdentifier(lit->value)) {
                        field.kind = ObjectField::FIELD_ID;
                        field.id = alloc.makeIdentifier(lit->value);
                        field.fodder1 = lit->openFodder;
                        field.expr1 = nullptr;
                    }
                }
            }
        }
        FmtPass::visit(expr);
    }
};
