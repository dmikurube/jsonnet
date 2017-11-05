class EnforceMaximumBlankLines : public FmtPass {
   public:
    EnforceMaximumBlankLines(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}
    void fodderElement(FodderElement &f)
    {
        if (f.kind != FodderElement::INTERSTITIAL)
            if (f.blanks > 2)
                f.blanks = 2;
    }
};
