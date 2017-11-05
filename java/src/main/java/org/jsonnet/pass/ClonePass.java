package org.jsonnet;

/**
 * A pass that clones the AST it is given.
 */
class ClonePass extends CompilerPass {
    public ClonePass(Allocator &alloc) {
        super(alloc);
    }

    public void expr(AST *&ast) {
    if (auto *ast = dynamic_cast<Apply *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<ApplyBrace *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Array *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<ArrayComprehension *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Assert *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Binary *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<BuiltinFunction *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Conditional *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Dollar *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Error *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Function *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Import *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Importstr *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<InSuper *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Index *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Local *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<LiteralBoolean *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<LiteralNumber *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<LiteralString *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<LiteralNull *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Object *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<DesugaredObject *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<ObjectComprehension *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<ObjectComprehensionSimple *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Parens *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Self *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<SuperIndex *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Unary *>(ast_)) {
        ast_ = alloc.clone(ast);
    } else if (auto *ast = dynamic_cast<Var *>(ast_)) {
        ast_ = alloc.clone(ast);

    } else {
        std::cerr << "INTERNAL ERROR: Unknown AST: " << ast_ << std::endl;
        std::abort();
    }

    CompilerPass::expr(ast_);
    }
}
