package org.jsonnet.vm;

import java.util.TreeMap;
import org.jsonnet.parser.Identifier;

/**
 * Stores the values bound to variables.
 *
 * Each nested local statement, function call, and field access has its own binding frame to
 * give the values for the local variable, function parameters, or upValues.
 */
public class BindingFrame extends TreeMap<Identifier, HeapThunk> {  // std::map in C++
}
