class StripComments : public FmtPass {
   public:
    StripComments(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}
    void fodder(Fodder &fodder)
    {
        Fodder copy = fodder;
        fodder.clear();
        for (auto &f : copy) {
            if (f.kind == FodderElement::LINE_END)
                fodder.push_back(f);
        }
    }
};
