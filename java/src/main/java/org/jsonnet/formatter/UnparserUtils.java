/** These cases are infix so we descend on the left to find the fodder. */
static Fodder &open_fodder(AST *ast_)
{
    AST *left = left_recursive(ast_);
    return left != nullptr ? open_fodder(left) : ast_->openFodder;
}

/** If left recursive, return the left hand side, else return nullptr. */
static AST *left_recursive(AST *ast_)
{
    if (auto *ast = dynamic_cast<Apply *>(ast_))
        return ast->target;
    if (auto *ast = dynamic_cast<ApplyBrace *>(ast_))
        return ast->left;
    if (auto *ast = dynamic_cast<Binary *>(ast_))
        return ast->left;
    if (auto *ast = dynamic_cast<Index *>(ast_))
        return ast->target;
    if (auto *ast = dynamic_cast<InSuper *>(ast_))
        return ast->element;
    return nullptr;
}
static const AST *left_recursive(const AST *ast_)
{
    return left_recursive(const_cast<AST *>(ast_));
}

/** A model of fodder_fill that just keeps track of the column counter. */
static void fodder_count(unsigned &column, const Fodder &fodder, bool space_before,
                         bool separate_token)
{
    for (const auto &fod : fodder) {
        switch (fod.kind) {
            case FodderElement::PARAGRAPH:
            case FodderElement::LINE_END:
                column = fod.indent;
                space_before = false;
                break;

            case FodderElement::INTERSTITIAL:
                if (space_before)
                    column++;
                column += fod.comment[0].length();
                space_before = true;
                break;
        }
    }
    if (separate_token && space_before)
        column++;
}
