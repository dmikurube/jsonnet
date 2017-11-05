package org.jsonnet.parser;

import java.util.ArrayList;
import org.jsonnet.LocationRange;
import org.jsonnet.lexer.Fodder;

/**
 * Represents local x = e; e.  After desugaring, functionSugar is false.
 */
public class Local extends AST {
    public static class Bind {
        private final Fodder varFodder;
        private final Identifier var;
        private final Fodder opFodder;
        private final AST body;
        private final boolean functionSugar;
        private final Fodder parenLeftFodder;
        private final ArgParams params;  // If functionSugar == true
        private final boolean trailingComma;
        private final Fodder parenRightFodder;
        private final Fodder closeFodder;

        public Bind(final Fodder var_fodder,
                    final Identifier var,
                    final Fodder op_fodder,
                    final AST body,
                    final boolean function_sugar,
                    final Fodder paren_left_fodder,
                    final ArgParams params,
                    final boolean trailing_comma,
                    final Fodder paren_right_fodder,
                    final Fodder close_fodder) {
            this.varFodder = var_fodder;
            this.var = var;
            this.opFodder = op_fodder;
            this.body = body;
            this.functionSugar = function_sugar;
            this.parenLeftFodder = paren_left_fodder;
            this.params = params;
            this.trailingComma = trailing_comma;
            this.parenRightFodder = paren_right_fodder;
            this.closeFodder = close_fodder;
        }

        public Identifier getVar() {
            return this.var;
        }
    }

    public static class Binds extends ArrayList<Bind> {
    }

    private final Binds binds;
    private final AST body;

    public Local(final LocationRange lr,
                 final Fodder open_fodder,
                 final Binds binds,
                 final AST body) {
        super(lr, ASTType.AST_LOCAL, open_fodder);
        this.binds = binds;
        this.body = body;
    }
}
