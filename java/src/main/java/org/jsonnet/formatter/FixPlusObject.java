/* Ensure ApplyBrace syntax sugar is used in the case of A + { }. */
class FixPlusObject : public FmtPass {
    using FmtPass::visit;

   public:
    FixPlusObject(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}
    void visitExpr(AST *&expr)
    {
        if (auto *bin_op = dynamic_cast<Binary *>(expr)) {
            // Could relax this to allow more ASTs on the LHS but this seems OK for now.
            if (dynamic_cast<Var *>(bin_op->left) || dynamic_cast<Index *>(bin_op->left)) {
                if (AST *rhs = dynamic_cast<Object *>(bin_op->right)) {
                    if (bin_op->op == BOP_PLUS) {
                        fodder_move_front(rhs->openFodder, bin_op->opFodder);
                        expr = alloc.make<ApplyBrace>(
                            bin_op->location, bin_op->openFodder, bin_op->left, rhs);
                    }
                }
            }
        }
        FmtPass::visitExpr(expr);
    }
};
