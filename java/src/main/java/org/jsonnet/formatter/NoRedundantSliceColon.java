/* Remove final colon in slices. */
class NoRedundantSliceColon : public FmtPass {
    using FmtPass::visit;

   public:
    NoRedundantSliceColon(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}

    void visit(Index *expr)
    {
        if (expr->isSlice) {
            if (expr->step == nullptr) {
                if (expr->stepColonFodder.size() > 0) {
                    fodder_move_front(expr->idFodder, expr->stepColonFodder);
                }
            }
        }
        FmtPass::visit(expr);
    }
};
