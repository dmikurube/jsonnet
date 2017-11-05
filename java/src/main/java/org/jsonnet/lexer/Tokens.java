package org.jsonnet.lexer;

import java.util.LinkedList;

/**
 * The result of lexing.
 *
 * Because of the EOF token, this will always contain at least one token.  So element 0 can be used
 * to get the filename.
 */
public class Tokens extends LinkedList<Token> {  // typedef std::list<Token> Tokens; May be better with LinkedList.
}
