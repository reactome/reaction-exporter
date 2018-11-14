package org.reactome.server.tools.reaction.exporter.layout.algorithm.gridbreathe;

import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Dedup;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GridBreathe {

    private static final Comparator<Boolean> FALSE_FIRST = Comparator.nullsFirst((o1, o2) -> o1.equals(o2) ? 0 : o1 ? 1 : -1);
    private static final Comparator<Boolean> TRUE_FIRST = Comparator.nullsLast((o1, o2) -> o1.equals(o2) ? 0 : o1 ? -1 : 1);
    private static final Comparator<CompartmentGlyph> OUTER_FIRST = (o1, o2) -> {
        if (isAncestor(o1, o2)) return -1;
        else if (isAncestor(o2, o1)) return 1;
        return 0;
    };


    private final Layout layout;
    private final LayoutIndex index;
    private final InfiniteCanvas canvas = new InfiniteCanvas();
    private final ReactionGlyph reaction;

    public GridBreathe(Layout layout) {
        this.layout = layout;
        reaction = layout.getReaction();
        Dedup.addDuplicates(layout);
        index = new LayoutIndex(layout);
    }

    private static boolean isAncestor(CompartmentGlyph ancestor, CompartmentGlyph compartment) {
        CompartmentGlyph parent = compartment.getParent();
        while (parent != null) {
            if (parent == ancestor) return true;
            parent = parent.getParent();
        }
        return false;
    }

    public void compute() {
        canvas.set(0, 0, new Tile(reaction.getCompartment(), Collections.singletonList(reaction), EntityRole.NEGATIVE_REGULATOR));
        layoutInputs();
    }

    private void layoutInputs() {
        final List<CompartmentGlyph> comps = sortForInputs();
        ArrayList<EntityGlyph> inputs = new ArrayList<>(index.getInputs());
        inputs.sort(Comparator.comparingInt((EntityGlyph e1) -> comps.indexOf(e1.getCompartment())));
        vertical(inputs, layout.getCompartmentRoot(), 0, 0);
    }

    private void vertical(ArrayList<EntityGlyph> entityGlyphs, CompartmentGlyph compartment, int row, int col) {
        for (final CompartmentGlyph child : compartment.getChildren()) {

        }

    }

    private List<CompartmentGlyph> sortForInputs() {
        return layout.getCompartments().stream()
                .filter(compartmentGlyph -> containsRole(compartmentGlyph, EntityRole.INPUT))
                .sorted((c1, c2) -> Comparator
                        .comparing((CompartmentGlyph cg) -> containsRole(cg, EntityRole.CATALYST), TRUE_FIRST)
                        .thenComparing(cg -> containsRole(cg, EntityRole.NEGATIVE_REGULATOR), FALSE_FIRST)
                        .thenComparing(cg -> containsRole(cg, EntityRole.POSITIVE_REGULATOR), FALSE_FIRST)
                        .thenComparing(OUTER_FIRST)
                        .compare(c1, c2))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean containsRole(CompartmentGlyph compartment, EntityRole role) {
        for (Glyph glyph : compartment.getContainedGlyphs()) {
            if (glyph instanceof EntityGlyph && hasRole((EntityGlyph) glyph, role)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRole(EntityGlyph glyph, EntityRole role) {
        return glyph.getRoles().stream().anyMatch(r -> r.getType() == role);
    }


}
