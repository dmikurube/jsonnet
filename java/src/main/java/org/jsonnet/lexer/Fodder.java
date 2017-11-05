package org.jsonnet.lexer;

import java.util.ArrayList;

public class Fodder extends ArrayList<FodderElement> {
    public Fodder() {
        super();
    }

    public Fodder(final Fodder other) {
        super(other);
    }

    public boolean fodder_has_clean_endline() {
        return !this.isEmpty() && this.get(this.size() - 1).getKind() != FodderElement.Kind.INTERSTITIAL;
    }

    /**
     * As a.push_back(elem) but preserves constraints.
     *
     * See concat_fodder below.
     */
    public void fodder_push_back(final FodderElement elem) {
        if (this.fodder_has_clean_endline() && elem.getKind() == FodderElement.Kind.LINE_END) {
            if (elem.getComment().size() > 0) {
                // The line end had a comment, so create a single line paragraph for it.
                this.add(new FodderElement(
                             FodderElement.Kind.PARAGRAPH, elem.getBlanks(), elem.getIndent(), elem.getComment()));
            } else {
                // Merge it into the previous line end.
                final FodderElement back = this.remove(this.size() - 1);
                this.add(new FodderElement(
                             back.getKind(), back.getBlanks() + elem.getBlanks(), elem.getIndent(), back.getComment()));
            }
        } else {
            if (!this.fodder_has_clean_endline() && elem.getKind() == FodderElement.Kind.PARAGRAPH) {
                this.add(new FodderElement(
                             FodderElement.Kind.LINE_END, 0, elem.getIndent(), new ArrayList<String>()));
            }
            this.add(elem);
        }
    }

    /**
     * As a + b but preserves constraints.
     *
     * Namely, a LINE_END is not allowed to follow a PARAGRAPH or a LINE_END.
     */
    public static Fodder concat_fodder(final Fodder a, final Fodder b) {
        if (a.size() == 0) {
            return new Fodder(b);
        }
        if (b.size() == 0) {
            return new Fodder(a);
        }
        final Fodder r = new Fodder(a);
        // Carefully add the first element of b.
        r.fodder_push_back(b.get(0));
        // Add the rest of b.
        for (int i = 1; i < b.size(); ++i) {
            r.add(b.get(i));
        }
        return r;
    }

    /**
     * Move b to the front of this.
     */
    public void fodder_move_front(final Fodder b) {
        if (this.size() == 0) {
            this.addAll(b);
        }
        if (b.size() == 0) {
            return;
        }
        // Carefully add the first element of b.
        this.fodder_push_back(b.get(0));
        // Add the rest of b.
        for (int i = 1; i < b.size(); ++i) {
            this.add(b.get(i));
        }
    }

    public static Fodder make_fodder(final FodderElement elem) {
        final Fodder fodder = new Fodder();
        fodder.fodder_push_back(elem);
        return fodder;
    }

    public static void ensureCleanNewline(final Fodder fodder) {
        if (!fodder.fodder_has_clean_endline()) {
            fodder.fodder_push_back(new FodderElement(FodderElement.Kind.LINE_END, 0, 0, new ArrayList<String>()));
        }
    }

    public int countNewlines() {
        int sum = 0;
        for (final FodderElement elem : this) {
            sum += elem.countNewlines();
        }
        return sum;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (final FodderElement f : this) {
            builder.append(first ? "[" : ", ");
            first = false;
            builder.append(f.toString());
        }
        builder.append(first ? "[]" : "]");
        return builder.toString();
    }
}
