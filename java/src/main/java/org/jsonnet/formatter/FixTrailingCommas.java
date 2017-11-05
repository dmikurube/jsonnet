/* Commas should appear at the end of an object/array only if the closing token is on a new line. */
class FixTrailingCommas : public FmtPass {
    using FmtPass::visit;

   public:
    FixTrailingCommas(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}
    Fodder comments;

    // Generalized fix that works across a range of ASTs.
    void fix_comma(Fodder &last_comma_fodder, bool &trailing_comma, Fodder &close_fodder)
    {
        bool need_comma = contains_newline(close_fodder) || contains_newline(last_comma_fodder);
        if (trailing_comma) {
            if (!need_comma) {
                // Remove it but keep fodder.
                trailing_comma = false;
                fodder_move_front(close_fodder, last_comma_fodder);
            } else if (contains_newline(last_comma_fodder)) {
                // The comma is needed but currently is separated by a newline.
                fodder_move_front(close_fodder, last_comma_fodder);
            }
        } else {
            if (need_comma) {
                // There was no comma, but there was a newline before the ] so add a comma.
                trailing_comma = true;
            }
        }
    }

    void remove_comma(Fodder &last_comma_fodder, bool &trailing_comma, Fodder &close_fodder)
    {
        if (trailing_comma) {
            // Remove it but keep fodder.
            trailing_comma = false;
            close_fodder = concat_fodder(last_comma_fodder, close_fodder);
            last_comma_fodder.clear();
        }
    }

    void visit(Array *expr)
    {
        if (expr->elements.size() == 0) {
            // No comma present and none can be added.
            return;
        }

        fix_comma(expr->elements.back().commaFodder, expr->trailingComma, expr->closeFodder);
        FmtPass::visit(expr);
    }

    void visit(ArrayComprehension *expr)
    {
        remove_comma(expr->commaFodder, expr->trailingComma, expr->specs[0].openFodder);
        FmtPass::visit(expr);
    }

    void visit(Object *expr)
    {
        if (expr->fields.size() == 0) {
            // No comma present and none can be added.
            return;
        }

        fix_comma(expr->fields.back().commaFodder, expr->trailingComma, expr->closeFodder);
        FmtPass::visit(expr);
    }

    void visit(ObjectComprehension *expr)
    {
        remove_comma(expr->fields.back().commaFodder, expr->trailingComma, expr->closeFodder);
        FmtPass::visit(expr);
    }

static bool contains_newline(const Fodder &fodder)
{
    for (const auto &f : fodder) {
        if (f.kind != FodderElement::INTERSTITIAL)
            return true;
    }
    return false;
}
};
