/** Sort top-level imports.
 *
 * Top-level imports are `local x = import 'xxx.jsonnet` expressions
 * that go before anything else in the file (more precisely all such imports
 * that are either the root of AST or a direct child (body) of a top-level
 * import.
 *
 * Grouping of imports is preserved. Groups of imports are separated by blank
 * lines or lines containing comments.
 */
class SortImports {
    /// Internal representation of an import
    struct ImportElem {
        ImportElem(UString key, Fodder adjacentFodder, Local::Bind bind)
            : key(key), adjacentFodder(adjacentFodder), bind(bind)
        {
        }

        // A key by which the imports should be sorted.
        // It's a file path that is imported, represented as UTF-32 codepoints without case folding.
        // In particular "Z" < "a", because 'Z' == 90 and 'a' == 97.
        UString key;

        // Comments adjacent to the import that go after it and that should stay attached
        // when imports are reordered.
        Fodder adjacentFodder;

        // The bind that contains the import
        // Satisfies: bind.functionSugar == false && bind.body->type == AST_IMPORT
        Local::Bind bind;
        bool operator<(const ImportElem &elem) const
        {
            return key < elem.key;
        }
    };

    typedef std::vector<ImportElem> ImportElems;

    Allocator &alloc;

   public:
    SortImports(Allocator &alloc) : alloc(alloc) {}

    /// Get the value by which the imports should be sorted.
    UString sortingKey(Import *import)
    {
        return import->file->value;
    }

    /// Check if `local` expression is used for importing,
    bool isGoodLocal(Local *local)
    {
        for (const auto &bind : local->binds) {
            if (bind.body->type != AST_IMPORT || bind.functionSugar) {
                return false;
            }
        }
        return true;
    }

    Local *goodLocalOrNull(AST *expr)
    {
        if (auto *local = dynamic_cast<Local *>(expr)) {
            return isGoodLocal(local) ? local : nullptr;
        } else {
            return nullptr;
        }
    }

    /** Split fodder after the first new line / paragraph fodder,
     * leaving blank lines after the newline in the second half.
     *
     * The two returned fodders can be concatenated using concat_fodder to get the original fodder.
     *
     * It's a heuristic that given two consecutive tokens `prev_token`, `next_token`
     * with some fodder between them, decides which part of the fodder logically belongs
     * to `prev_token` and which part belongs to the `next_token`.
     *
     * Example:
     * prev_token // prev_token is awesome!
     *
     * // blah blah
     * next_token
     *
     * In such case "// prev_token is awesome!\n" part of the fodder belongs
     * to the `prev_token` and "\n//blah blah\n" to the `next_token`.
     */
    std::pair<Fodder, Fodder> splitFodder(const Fodder &fodder)
    {
        Fodder afterPrev, beforeNext;
        bool inSecondPart = false;
        for (const auto &fodderElem : fodder) {
            if (inSecondPart) {
                fodder_push_back(beforeNext, fodderElem);
            } else {
                afterPrev.push_back(fodderElem);
            }
            if (fodderElem.kind != FodderElement::Kind::INTERSTITIAL && !inSecondPart) {
                inSecondPart = true;
                if (fodderElem.blanks > 0) {
                    // If there are any blank lines at the end of afterPrev, move them
                    // to beforeNext.
                    afterPrev.back().blanks = 0;
                    assert(beforeNext.empty());
                    beforeNext.emplace_back(FodderElement::Kind::LINE_END,
                                            fodderElem.blanks,
                                            fodderElem.indent,
                                            std::vector<std::string>());
                }
            }
        }
        return {afterPrev, beforeNext};
    }

    void sortGroup(ImportElems &imports)
    {
        // We don't want to change behaviour in such case:
        // local foo = "b.jsonnet";
        // local foo = "a.jsonnet";
        // So we don't change the order when there are shadowed variables.
        if (!duplicatedVariables(imports)) {
            std::sort(imports.begin(), imports.end());
        }
    }

    ImportElems extractImportElems(const Local::Binds &binds, Fodder after)
    {
        ImportElems result;
        Fodder before = binds.front().varFodder;
        for (int i = 0; i < int(binds.size()); ++i) {
            const auto &bind = binds[i];
            bool last = i == int(binds.size() - 1);
            Fodder adjacent, beforeNext;
            if (!last) {
                auto &next = binds[i + 1];
                std::tie(adjacent, beforeNext) = splitFodder(next.varFodder);
            } else {
                adjacent = after;
            }
            ensureCleanNewline(adjacent);
            Local::Bind newBind = bind;
            newBind.varFodder = before;
            Import *import = dynamic_cast<Import *>(bind.body);
            assert(import != nullptr);
            result.emplace_back(sortingKey(import), adjacent, newBind);
            before = beforeNext;
        }
        return result;
    }

    AST *buildGroupAST(ImportElems &imports, AST *body, const Fodder &groupOpenFodder)
    {
        for (int i = imports.size() - 1; i >= 0; --i) {
            auto &import = imports[i];
            Fodder fodder;
            if (i == 0) {
                fodder = groupOpenFodder;
            } else {
                fodder = imports[i - 1].adjacentFodder;
            }
            auto *local =
                alloc.make<Local>(LocationRange(), fodder, Local::Binds({import.bind}), body);
            body = local;
        }

        return body;
    }

    bool duplicatedVariables(const ImportElems &elems)
    {
        std::set<UString> idents;
        for (const auto &elem : elems) {
            idents.insert(elem.bind.var->name);
        }
        return idents.size() < elems.size();
    }

    /// Check if the import group ends after this local
    bool groupEndsAfter(Local *local)
    {
        Local *next = goodLocalOrNull(local->body);
        if (!next) {
            return true;
        }

        bool newlineReached = false;
        for (const auto &fodderElem : open_fodder(next)) {
            if (newlineReached || fodderElem.blanks > 0) {
                return true;
            }
            if (fodderElem.kind != FodderElement::Kind::INTERSTITIAL) {
                newlineReached = true;
            }
        }
        return false;
    }

    AST *toplevelImport(Local *local, ImportElems &imports, const Fodder &groupOpenFodder)
    {
        assert(isGoodLocal(local));

        Fodder adjacentCommentFodder, beforeNextFodder;
        std::tie(adjacentCommentFodder, beforeNextFodder) = splitFodder(open_fodder(local->body));

        ensureCleanNewline(adjacentCommentFodder);

        ImportElems newImports = extractImportElems(local->binds, adjacentCommentFodder);
        imports.insert(imports.end(), newImports.begin(), newImports.end());

        if (groupEndsAfter(local)) {
            sortGroup(imports);

            Fodder afterGroup = imports.back().adjacentFodder;
            ensureCleanNewline(beforeNextFodder);
            auto nextOpenFodder = concat_fodder(afterGroup, beforeNextFodder);

            // Process the code after the current group:
            AST *bodyAfterGroup;
            Local *next = goodLocalOrNull(local->body);
            if (next) {
                // Another group of imports
                ImportElems nextImports;
                bodyAfterGroup = toplevelImport(next, nextImports, nextOpenFodder);
            } else {
                // Something else
                bodyAfterGroup = local->body;
                open_fodder(bodyAfterGroup) = nextOpenFodder;
            }

            return buildGroupAST(imports, bodyAfterGroup, groupOpenFodder);
        } else {
            assert(beforeNextFodder.empty());
            return toplevelImport(dynamic_cast<Local *>(local->body), imports, groupOpenFodder);
        }
    }

    void file(AST *&body)
    {
        ImportElems imports;
        Local *local = goodLocalOrNull(body);
        if (local) {
            body = toplevelImport(local, imports, open_fodder(local));
        }
    }
};
