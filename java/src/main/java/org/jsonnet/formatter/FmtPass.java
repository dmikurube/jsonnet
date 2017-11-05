/** A generic Pass that does nothing but can be extended to easily define real passes.
 */
class FmtPass : public CompilerPass {
   protected:
    FmtOpts opts;

   public:
    FmtPass(Allocator &alloc, const FmtOpts &opts) : CompilerPass(alloc), opts(opts) {}
}
