class StripAllButComments : public FmtPass {
   public:
    StripAllButComments(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}
    Fodder comments;
    void fodder(Fodder &fodder)
    {
        for (auto &f : fodder) {
            if (f.kind == FodderElement::PARAGRAPH) {
                comments.emplace_back(FodderElement::PARAGRAPH, 0, 0, f.comment);
            } else if (f.kind == FodderElement::INTERSTITIAL) {
                comments.push_back(f);
                comments.emplace_back(FodderElement::LINE_END, 0, 0, std::vector<std::string>{});
            }
        }
        fodder.clear();
    }
    virtual void file(AST *&body, Fodder &final_fodder)
    {
        expr(body);
        fodder(final_fodder);
        body = alloc.make<LiteralNull>(body->location, comments);
        final_fodder.clear();
    }
};
