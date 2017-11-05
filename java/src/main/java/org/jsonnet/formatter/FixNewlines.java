/// Add newlines inside complex structures (arrays, objects etc.).
///
/// The main principle is that a structure can either be:
/// * expanded and contain newlines in all the designated places
/// * unexpanded and contain newlines in none of the designated places
///
/// It only looks shallowly at the AST nodes, so there may be some newlines deeper that
/// don't affect expanding. For example:
/// [{
///     'a': 'b',
///     'c': 'd',
/// }]
/// The outer array can stay unexpanded, because there are no newlines between
/// the square brackets and the braces.
class FixNewlines : public FmtPass {
    using FmtPass::visit;

    bool shouldExpand(const Array *array)
    {
        for (const auto &elem : array->elements) {
            if (countNewlines(open_fodder(elem.expr)) > 0) {
                return true;
            }
        }
        if (countNewlines(array->closeFodder) > 0) {
            return true;
        }
        return false;
    }

    void expand(Array *array)
    {
        for (auto &elem : array->elements) {
            ensureCleanNewline(open_fodder(elem.expr));
        }
        ensureCleanNewline(array->closeFodder);
    }

    Fodder &objectFieldOpenFodder(ObjectField &field)
    {
        if (field.kind == ObjectField::Kind::FIELD_STR) {
            return field.expr1->openFodder;
        }
        return field.fodder1;
    }

    bool shouldExpand(Object *object)
    {
        for (auto &field : object->fields) {
            if (countNewlines(objectFieldOpenFodder(field)) > 0) {
                return true;
            }
        }
        if (countNewlines(object->closeFodder) > 0) {
            return true;
        }
        return false;
    }

    void expand(Object *object)
    {
        for (auto &field : object->fields) {
            ensureCleanNewline(objectFieldOpenFodder(field));
        }
        ensureCleanNewline(object->closeFodder);
    }

    bool shouldExpand(Local *local)
    {
        for (auto &bind : local->binds) {
            if (countNewlines(bind.varFodder) > 0) {
                return true;
            }
        }
        return false;
    }

    void expand(Local *local)
    {
        bool first = true;
        for (auto &bind : local->binds) {
            if (!first) {
                ensureCleanNewline(bind.varFodder);
            }
            first = false;
        }
    }

    bool shouldExpand(ArrayComprehension *comp)
    {
        if (countNewlines(open_fodder(comp->body)) > 0) {
            return true;
        }
        for (auto &spec : comp->specs) {
            if (countNewlines(spec.openFodder) > 0) {
                return true;
            }
        }
        if (countNewlines(comp->closeFodder) > 0) {
            return true;
        }
        return false;
    }

    void expand(ArrayComprehension *comp)
    {
        ensureCleanNewline(open_fodder(comp->body));
        for (auto &spec : comp->specs) {
            ensureCleanNewline(spec.openFodder);
        }
        ensureCleanNewline(comp->closeFodder);
    }

    bool shouldExpand(ObjectComprehension *comp)
    {
        for (auto &field : comp->fields) {
            if (countNewlines(objectFieldOpenFodder(field)) > 0) {
                return true;
            }
        }
        for (auto &spec : comp->specs) {
            if (countNewlines(spec.openFodder) > 0) {
                return true;
            }
        }
        if (countNewlines(comp->closeFodder) > 0) {
            return true;
        }
        return false;
    }

    void expand(ObjectComprehension *comp)
    {
        for (auto &field : comp->fields) {
            ensureCleanNewline(objectFieldOpenFodder(field));
        }
        for (auto &spec : comp->specs) {
            ensureCleanNewline(spec.openFodder);
        }
        ensureCleanNewline(comp->closeFodder);
    }

    bool shouldExpand(Parens *parens)
    {
        return countNewlines(open_fodder(parens->expr)) > 0 ||
               countNewlines(parens->closeFodder) > 0;
    }

    void expand(Parens *parens)
    {
        ensureCleanNewline(open_fodder(parens->expr));
        ensureCleanNewline(parens->closeFodder);
    }

    Fodder &argParamOpenFodder(ArgParam &param)
    {
        if (param.id != nullptr) {
            return param.idFodder;
        } else if (param.expr != nullptr) {
            return open_fodder(param.expr);
        } else {
            std::cerr << "Invalid ArgParam" << std::endl;
            abort();
        }
    }

    // Example:
    // f(1, 2,
    //   3)
    // Should be expanded to:
    // f(1,
    //   2,
    //   3)
    bool shouldExpandBetween(ArgParams &params)
    {
        bool first = true;
        for (auto &param : params) {
            if (!first && countNewlines(argParamOpenFodder(param)) > 0) {
                return true;
            }
            first = false;
        }
        return false;
    }

    void expandBetween(ArgParams &params)
    {
        bool first = true;
        for (auto &param : params) {
            if (!first) {
                ensureCleanNewline(argParamOpenFodder(param));
            }
            first = false;
        }
    }

    // Example:
    // foo(
    //     1, 2, 3)
    // Should be expanded to:
    // foo(
    //     1, 2, 3
    // )
    bool shouldExpandNearParens(ArgParams &params, Fodder &fodder_r)
    {
        if (params.empty()) {
            return false;
        }
        auto &argFodder = argParamOpenFodder(params.front());
        return countNewlines(fodder_r) > 0 || countNewlines(argFodder) > 0;
    }

    void expandNearParens(ArgParams &params, Fodder &fodder_r)
    {
        if (!params.empty()) {
            ensureCleanNewline(argParamOpenFodder(params.front()));
        }
        ensureCleanNewline(fodder_r);
    }

   public:
    FixNewlines(Allocator &alloc, const FmtOpts &opts) : FmtPass(alloc, opts) {}

    template <class T>
    void simpleExpandingVisit(T *expr)
    {
        if (shouldExpand(expr)) {
            expand(expr);
        }
        FmtPass::visit(expr);
    }

    void visit(Array *array)
    {
        simpleExpandingVisit(array);
    }

    void visit(Object *object)
    {
        simpleExpandingVisit(object);
    }

    void visit(Local *local)
    {
        simpleExpandingVisit(local);
    }

    void visit(ArrayComprehension *comp)
    {
        simpleExpandingVisit(comp);
    }

    void visit(ObjectComprehension *comp)
    {
        simpleExpandingVisit(comp);
    }

    void visit(Parens *parens)
    {
        simpleExpandingVisit(parens);
    }

    void params(Fodder &fodder_l, ArgParams &params, Fodder &fodder_r)
    {
        if (shouldExpandBetween(params)) {
            expandBetween(params);
        }

        if (shouldExpandNearParens(params, fodder_r)) {
            expandNearParens(params, fodder_r);
        }

        FmtPass::params(fodder_l, params, fodder_r);
    }
};
