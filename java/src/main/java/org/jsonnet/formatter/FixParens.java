/* Remove nested parens. */
class FixParens : public FmtPass {
    using FmtPass::visit;

   public:
    FixParens(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}
    void visit(Parens *expr)
    {
        if (auto *body = dynamic_cast<Parens *>(expr->expr)) {
            // Deal with fodder.
            expr->expr = body->expr;
            fodder_move_front(open_fodder(body->expr), body->openFodder);
            fodder_move_front(expr->closeFodder, body->closeFodder);
        }
        FmtPass::visit(expr);
    }
};
