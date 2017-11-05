package org.jsonnet;

/**
 * A generic Pass that does nothing but can be extended to easily define real passes.
 */
public class CompilerPass {
    public CompilerPass(Allocator alloc) {
        this.alloc = alloc;
    }

    public void fodderElement(FodderElement &) {
    }

    public void fodder(Fodder &fodder) {
        for (auto &f : fodder) {
            fodderElement(f);
        }
    }

    public void specs(std::vector<ComprehensionSpec> &specs) {
        for (auto &spec : specs) {
            fodder(spec.openFodder);
            switch (spec.kind) {
                case ComprehensionSpec::FOR:
                    fodder(spec.varFodder);
                    fodder(spec.inFodder);
                    expr(spec.expr);
                    break;
                case ComprehensionSpec::IF: expr(spec.expr); break;
            }
        }
    }

    public void params(Fodder &fodder_l, ArgParams &params, Fodder &fodder_r) {
        fodder(fodder_l);
        for (auto &param : params) {
            fodder(param.idFodder);
            if (param.expr) {
                fodder(param.eqFodder);
                expr(param.expr);
            }
            fodder(param.commaFodder);
        }
        fodder(fodder_r);
    }

    public void fieldParams(ObjectField &field) {
        if (field.methodSugar) {
            params(field.fodderL, field.params, field.fodderR);
        }
    }

    public void fields(ObjectFields &fields) {
    for (auto &field : fields) {
        switch (field.kind) {
            case ObjectField::LOCAL: {
                fodder(field.fodder1);
                fodder(field.fodder2);
                fieldParams(field);
                fodder(field.opFodder);
                expr(field.expr2);
            } break;

            case ObjectField::FIELD_ID:
            case ObjectField::FIELD_STR:
            case ObjectField::FIELD_EXPR: {
                if (field.kind == ObjectField::FIELD_ID) {
                    fodder(field.fodder1);

                } else if (field.kind == ObjectField::FIELD_STR) {
                    expr(field.expr1);

                } else if (field.kind == ObjectField::FIELD_EXPR) {
                    fodder(field.fodder1);
                    expr(field.expr1);
                    fodder(field.fodder2);
                }
                fieldParams(field);
                fodder(field.opFodder);
                expr(field.expr2);

            } break;

            case ObjectField::ASSERT: {
                fodder(field.fodder1);
                expr(field.expr2);
                if (field.expr3 != nullptr) {
                    fodder(field.opFodder);
                    expr(field.expr3);
                }
            } break;
        }

        fodder(field.commaFodder);
    }
    }

    public void expr(AST *&ast_) {
    fodder(ast_->openFodder);
    visitExpr(ast_);
    }

    public void visit(Apply *ast) {
    expr(ast->target);
    params(ast->fodderL, ast->args, ast->fodderR);
    if (ast->tailstrict) {
        fodder(ast->tailstrictFodder);
    }
    }

    public void visit(ApplyBrace *ast) {
    expr(ast->left);
    expr(ast->right);
    }

    public void visit(Array *ast) {
    for (auto &element : ast->elements) {
        expr(element.expr);
        fodder(element.commaFodder);
    }
    fodder(ast->closeFodder);
    }

    public void visit(ArrayComprehension *ast);{
    expr(ast->body);
    fodder(ast->commaFodder);
    specs(ast->specs);
    fodder(ast->closeFodder);
    }

    public void visit(Assert *ast);{
    expr(ast->cond);
    if (ast->message != nullptr) {
        fodder(ast->colonFodder);
        expr(ast->message);
    }
    fodder(ast->semicolonFodder);
    expr(ast->rest);
    }

    public void visit(Binary *ast);{
    expr(ast->left);
    fodder(ast->opFodder);
    expr(ast->right);
    }

    public void visit(BuiltinFunction *) {}

    public void visit(Conditional *ast) {
    expr(ast->cond);
    fodder(ast->thenFodder);
    if (ast->branchFalse != nullptr) {
        expr(ast->branchTrue);
        fodder(ast->elseFodder);
        expr(ast->branchFalse);
    } else {
        expr(ast->branchTrue);
    }
    }

    public void visit(Dollar *) {}

    public void visit(Error *ast) {
    expr(ast->expr);
    }

    public void visit(Function *ast) {
    params(ast->parenLeftFodder, ast->params, ast->parenRightFodder);
    expr(ast->body);
    }

    public void visit(Import *ast) {
    visit(ast->file);
    }

    public void visit(Importstr *ast) {
    visit(ast->file);
    }

    public void visit(InSuper *ast) {
        expr(ast->element);
    }

    public void visit(Index *ast) {
    expr(ast->target);
    if (ast->id != nullptr) {
    } else {
        if (ast->isSlice) {
            if (ast->index != nullptr)
                expr(ast->index);
            if (ast->end != nullptr)
                expr(ast->end);
            if (ast->step != nullptr)
                expr(ast->step);
        } else {
            expr(ast->index);
        }
    }
    }

    public void visit(Local *ast) {
    assert(ast->binds.size() > 0);
    for (auto &bind : ast->binds) {
        fodder(bind.varFodder);
        if (bind.functionSugar) {
            params(bind.parenLeftFodder, bind.params, bind.parenRightFodder);
        }
        fodder(bind.opFodder);
        expr(bind.body);
        fodder(bind.closeFodder);
    }
    expr(ast->body);
    }

    public void visit(LiteralBoolean *) {}

    public void visit(LiteralNumber *) {}

    public void visit(LiteralString *) {}

    public void visit(LiteralNull *) {}

    public void visit(org.jsonnet.Object *ast) {
    fields(ast->fields);
    fodder(ast->closeFodder);
    }

    public void visit(DesugaredObject *ast) {
    for (AST *assert : ast->asserts) {
        expr(assert);
    }
    for (auto &field : ast->fields) {
        expr(field.name);
        expr(field.body);
    }
    }

    public void visit(ObjectComprehension *ast) {
        fields(ast->fields);
        specs(ast->specs);
        fodder(ast->closeFodder);
    }

    public void visit(ObjectComprehensionSimple *ast) {
        expr(ast->field);
        expr(ast->value);
        expr(ast->array);
    }

    public void visit(Parens *ast) {
        expr(ast->expr);
        fodder(ast->closeFodder);
    }

    public void visit(Self *) {}

    public void visit(SuperIndex *ast) {
        if (ast->id != nullptr) {
        } else {
            expr(ast->index);
        }
    }

    public void visit(Unary *ast) {
        expr(ast->expr);
    }

    public void visit(Var *) {}

    public void visitExpr(AST *&ast_) {
    if (auto *ast = dynamic_cast<Apply *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<ApplyBrace *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Array *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<ArrayComprehension *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Assert *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Binary *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<BuiltinFunction *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Conditional *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Dollar *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Error *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Function *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Import *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Importstr *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<InSuper *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Index *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Local *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<LiteralBoolean *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<LiteralNumber *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<LiteralString *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<LiteralNull *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Object *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<DesugaredObject *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<ObjectComprehension *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<ObjectComprehensionSimple *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Parens *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Self *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<SuperIndex *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Unary *>(ast_)) {
        visit(ast);
    } else if (auto *ast = dynamic_cast<Var *>(ast_)) {
        visit(ast);

    } else {
        std::cerr << "INTERNAL ERROR: Unknown AST: " << ast_ << std::endl;
        std::abort();
    }
    }

    public void file(AST *&body, Fodder &final_fodder) {
        expr(body);
        fodder(final_fodder);
    }

   protected Allocator alloc;
}

/**
 * Return an equivalent AST that can be modified without affecting the original.
 *
 * This is a deep copy.
 */
AST *clone_ast(Allocator &alloc, AST *ast)
{
    AST *r = ast;
    ClonePass(alloc).expr(r);
    return r;
}
