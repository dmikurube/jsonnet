class StripEverything : public FmtPass {
   public:
    StripEverything(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}
    void fodder(Fodder &fodder)
    {
        fodder.clear();
    }
};
