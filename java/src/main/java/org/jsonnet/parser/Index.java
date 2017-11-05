package org.jsonnet.parser;

import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents both e[e] and the syntax sugar e.f.
 *
 * One of index and id will be nullptr before desugaring.  After desugaring id will be nullptr.
 */
public class Index extends AST {
    private final AST target;
    private final Fodder dotFodder;  // When index is being used, this is the fodder before the [.
    private final boolean isSlice;
    private final AST index;
    private final Fodder endColonFodder;  // When end is being used, this is the fodder before the :.
    private final AST end;
    private final Fodder stepColonFodder;  // When step is being used, this is the fodder before the :.
    private final AST step;
    private final Fodder idFodder;  // When index is being used, this is the fodder before the ].
    private final Identifier id;

    // Use this finalructor for e.f
    public Index(final LocationRange lr,
                 final Fodder open_fodder,
                 final AST target,
                 final Fodder dot_fodder,
                 final Fodder id_fodder,
                 final Identifier id) {
        super(lr, ASTType.AST_INDEX, open_fodder);
        this.target = target;
        this.dotFodder = dot_fodder;
        this.isSlice = false;
        this.index = null;
        this.endColonFodder = null;  // Newly added
        this.end = null;
        this.stepColonFodder = null;  // Newly added
        this.step = null;
        this.idFodder = id_fodder;
        this.id = id;
    }

    // Use this finalructor for e[x:y:z] with nullptr for index, end or step if not present.
    public Index(final LocationRange lr,
                 final Fodder open_fodder,
                 final AST target,
                 final Fodder dot_fodder,
                 final boolean is_slice,
                 final AST index,
                 final Fodder end_colon_fodder,
                 final AST end,
                 final Fodder step_colon_fodder,
                 final AST step,
                 final Fodder id_fodder) {
        super(lr, ASTType.AST_INDEX, open_fodder);
        this.target = target;
        this.dotFodder = dot_fodder;
        this.isSlice = is_slice;
        this.index = index;
        this.endColonFodder = end_colon_fodder;
        this.end = end;
        this.stepColonFodder = step_colon_fodder;
        this.step = step;
        this.idFodder = id_fodder;
        this.id = null;
    }
}
