class EnforceCommentStyle : public FmtPass {
   public:
    bool firstFodder;
    EnforceCommentStyle(Allocator &alloc, const FmtOpts &opts)
        : FmtPass(alloc, opts), firstFodder(true)
    {
    }
    /** Change the comment to match the given style, but don't break she-bang.
     *
     * If preserve_hash is true, do not touch a comment that starts with #!.
     */
    void fixComment(std::string &s, bool preserve_hash)
    {
        if (opts.commentStyle == 'h' && s[0] == '/') {
            s = "#" + s.substr(2);
        }
        if (opts.commentStyle == 's' && s[0] == '#') {
            if (preserve_hash && s[1] == '!')
                return;
            s = "//" + s.substr(1);
        }
    }
    void fodder(Fodder &fodder)
    {
        for (auto &f : fodder) {
            switch (f.kind) {
                case FodderElement::LINE_END:
                case FodderElement::PARAGRAPH:
                    if (f.comment.size() == 1) {
                        fixComment(f.comment[0], firstFodder);
                    }
                    break;

                case FodderElement::INTERSTITIAL: break;
            }
            firstFodder = false;
        }
    }
};
