package org.jsonnet.unparse;

public class FmtOpts {
    char stringStyle;
    char commentStyle;
    unsigned indent;
    unsigned maxBlankLines;
    bool padArrays;
    bool padObjects;
    bool stripComments;
    bool stripAllButComments;
    bool stripEverything;
    bool prettyFieldNames;
    bool sortImports;

    public FmtOpts() {
        : stringStyle('l'),
          commentStyle('l'),
          indent(0),
          maxBlankLines(2),
          padArrays(false),
          padObjects(true),
          stripComments(false),
          stripAllButComments(false),
          stripEverything(false),
          prettyFieldNames(true),
          sortImports(false)
    }
}
