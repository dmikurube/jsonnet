class EnforceStringStyle : public FmtPass {
    using FmtPass::visit;

   public:
    EnforceStringStyle(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}
    void visit(LiteralString *lit)
    {
        if (lit->tokenKind == LiteralString::BLOCK)
            return;
        if (lit->tokenKind == LiteralString::VERBATIM_DOUBLE)
            return;
        if (lit->tokenKind == LiteralString::VERBATIM_SINGLE)
            return;
        UString canonical = jsonnet_string_unescape(lit->location, lit->value);
        unsigned num_single = 0, num_double = 0;
        for (char32_t c : canonical) {
            if (c == '\'')
                num_single++;
            if (c == '"')
                num_double++;
        }
        if (num_single > 0 && num_double > 0)
            return;  // Don't change it.
        bool use_single = opts.stringStyle == 's';
        if (num_single > 0)
            use_single = false;
        if (num_double > 0)
            use_single = true;

        // Change it.
        lit->value = jsonnet_string_escape(canonical, use_single);
        lit->tokenKind = use_single ? LiteralString::SINGLE : LiteralString::DOUBLE;
    }
}
