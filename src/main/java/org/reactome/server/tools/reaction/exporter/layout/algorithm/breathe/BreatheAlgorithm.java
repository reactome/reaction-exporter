package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.Stoichiometry;
import org.reactome.server.tools.diagram.data.layout.impl.*;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.LayoutAlgorithm;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.FontProperties;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Dedup.addDuplicates;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.CATALYST;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.INPUT;

/**
 * Standard layout algorithm. It places inputs on the left, outputs on the right, catalysts on top and regulators on the
 * bottom. Every group of participants is placed in a line perpendicular to the edge joining the center of the group to
 * the reaction, i.e. inputs and outputs vertically, catalysts and regulators horizontally. When a group of participants
 * lays into more than one compartment, the are placed into parallel lines, one behind the other, so compartments have
 * space to be drawn. At last, reaction is placed in the centre of the diagram, unless it falls into a compartment it
 * does not belong to. In that case, it is moved to a safer position. In order of priority, it's moved towards inputs,
 * if not possible towards outputs, then catalysts and regulators.
 */
@SuppressWarnings("Duplicates")
public class BreatheAlgorithm implements LayoutAlgorithm {

    private static final int COLUMN_PADDING = 20;
    /**
     * Length of the backbone of the reaction
     */
    private static final double BACKBONE_LENGTH = 20;
    /**
     * Size of the box surrounding regulator and catalysts shapes
     */
    private static final int REGULATOR_SIZE = 6;
    /**
     * Minimum length of segments departing participants.
     */
    private static final int MIN_SEGMENT = 35;
    /**
     * Minimum distance between the compartment border and any of ints contained glyphs.
     */
    private static final double COMPARTMENT_PADDING = 20;
    /**
     * Minimum allocated height for any glyph. Even if glyphs have a lower height, they are placed in the middle of this
     * minimum distance.
     */
    private static final double MIN_GLYPH_HEIGHT = 25;
    /**
     * Minimum allocated width for any glyph. Even if glyphs have a lower width, they are placed in the middle of this
     * minimum distance.
     */
    private static final double MIN_GLYPH_WIDTH = 60;
    /**
     * Vertical (y-axis) distance between two glyphs.
     */
    private static final double VERTICAL_PADDING = 4;
    /**
     * Horizontal (x-axis) distance between two glyphs.
     */
    private static final double HORIZONTAL_PADDING = 12;
    /**
     * Minimum vertical distance between any glyph and the reaction glyph
     */
    private static final double REACTION_MIN_V_DISTANCE = 60;
    /**
     * Minimum horizontal distance between any glyph and the reaction glyph
     */
    private static final double REACTION_MIN_H_DISTANCE = 200;
    private static final double ARROW_SIZE = 8;
    private static final double GENE_SEGMENT_LENGTH = 20;
    private final Comparator<Boolean> TRUE_FIRST = (o1, o2) -> o1 == o2 ? 0 : o1 ? -1 : 1;
    private LayoutIndex index;
    private boolean compact = false;

    @Override
    public void compute(Layout layout) {
        addDuplicates(layout);
        index = new LayoutIndex(layout);
        layoutParticipants(layout);
        layoutCompartments(layout);
        layoutReaction(layout);
        // we need to recalculate compartment positions if reaction has been moved
        layoutCompartments(layout);
        layoutConnectors(layout);
        removeExtracellular(layout);
        computeDimension(layout);
        failedReactions(layout);
        moveToOrigin(layout);
    }

    public void compute(Layout layout, boolean compact) {
        this.compact = compact;
        compute(layout);
    }

    private void layoutParticipants(Layout layout) {
        for (EntityGlyph entity : layout.getEntities()) setSize(entity);
        layoutInputs(layout);
        layoutOutputs(layout);
        layoutCatalysts(layout);
        layoutRegulators(layout);
    }

    private void layoutInputs(Layout layout) {
        // TODO: 02/11/18 use a global variable
        final List<CompartmentGlyph> sortedCompartments = flattenCompartments(layout.getCompartmentRoot());
        sortedCompartments.sort(Comparator.comparing(compartmentGlyph -> hasRole(compartmentGlyph, CATALYST), TRUE_FIRST));
        final List<EntityGlyph> inputs = new ArrayList<>(index.getInputs());
        inputs.sort(Comparator.comparingInt((EntityGlyph e) -> sortedCompartments.indexOf(e.getCompartment())));
        // To do the vertical layout, we need the max height
        double heightPerGlyph = MIN_GLYPH_HEIGHT;
        for (EntityGlyph input : index.getInputs()) {
            final Position bounds = Transformer.getBounds(input);
            if (bounds.getHeight() > heightPerGlyph) heightPerGlyph = bounds.getHeight();
        }
        heightPerGlyph += VERTICAL_PADDING;
        layoutVerticalEntities(layout.getCompartmentRoot(), inputs, heightPerGlyph, (glyph, coord) ->
                // Inputs have negative x-coord
                Transformer.center(glyph, new CoordinateImpl(-coord.getX(), coord.getY())));
        centerInputs(layout, inputs);

    }

    private boolean hasRole(CompartmentGlyph compartment, EntityRole role) {
        return compartment.getContainedGlyphs().stream()
                .filter(EntityGlyph.class::isInstance)
                .flatMap(glyph -> ((EntityGlyph) glyph).getRoles().stream())
                .map(Role::getType)
                .anyMatch(role::equals);
    }

    private boolean hasRole(EntityGlyph entity, EntityRole role) {
        return entity.getRoles().stream()
                .map(Role::getType)
                .anyMatch(role::equals);
    }


    private void layoutOutputs(Layout layout) {
        final List<CompartmentGlyph> sortedCompartments = flattenCompartments(layout.getCompartmentRoot());
        sortedCompartments.sort(Comparator.comparing(compartmentGlyph -> hasRole(compartmentGlyph, CATALYST), TRUE_FIRST));
        final List<EntityGlyph> outputs = new ArrayList<>(index.getOutputs());
        outputs.sort(Comparator.comparingInt((EntityGlyph e) -> sortedCompartments.indexOf(e.getCompartment())));
        // Compute max height and call vertical layout
        double heightPerGlyph = MIN_GLYPH_HEIGHT;
        for (EntityGlyph output : outputs) {
            final Position bounds = Transformer.getBounds(output);
            if (bounds.getHeight() > heightPerGlyph) heightPerGlyph = bounds.getHeight();
        }
        heightPerGlyph += VERTICAL_PADDING;
        layoutVerticalEntities(layout.getCompartmentRoot(), outputs, heightPerGlyph, Transformer::center);
        centerOutputs(layout, index.getOutputs());
    }

    private void centerInputs(Layout layout, List<EntityGlyph> entities) {
        // We now center vertically and add horizontal spacing until the distance to the reaction is the same as half
        // of the height, forming a 45 degrees angle.
        double maxy = 0;
        double maxx = Integer.MIN_VALUE;
        for (final EntityGlyph input : entities) {
            final Position bounds = getBounds(input);
            maxy = Math.max(maxy, bounds.getCenterY());
            maxx = Math.max(maxx, bounds.getMaxX());
        }
        final double dx = layout.getReaction().getPosition().getCenterX() - maxx;
        final double dy = maxy * 0.5;
        double cx = 0;
        if (dx < dy) cx = -dx;
        for (final EntityGlyph input : entities) Transformer.move(input, cx, -dy);
    }

    private void centerOutputs(Layout layout, List<EntityGlyph> entities) {
        // Center vertically and add horizontal space if needed
        double maxy = 0;
        double minx = Integer.MAX_VALUE;
        for (final EntityGlyph output : entities) {
            final Position bounds = getBounds(output);
            maxy = Math.max(maxy, bounds.getCenterY());
            minx = Math.min(minx, bounds.getX());
        }
        final double dx = minx - layout.getReaction().getPosition().getCenterX();
        final double cy = maxy * 0.5;
        double cx = 0;
        if (dx < cy) cx = cy - dx;
        for (final EntityGlyph entity : entities) Transformer.move(entity, cx, -cy);
    }

    private void layoutCatalysts(Layout layout) {
        // Remove catalysts that are inputs
        final List<EntityGlyph> catalysts = new ArrayList<>(index.getCatalysts());
        catalysts.removeIf(entityGlyph -> entityGlyph.getRoles().stream().anyMatch(role -> role.getType() == INPUT));
        if (catalysts.isEmpty()) return;
        final List<CompartmentGlyph> sortedCompartments = flattenCompartments(layout.getCompartmentRoot());
        catalysts.sort(Comparator.comparingInt((EntityGlyph e) -> sortedCompartments.indexOf(e.getCompartment())));
        double widthPerGlyph = MIN_GLYPH_WIDTH;
        for (EntityGlyph catalyst : catalysts) {
            final Position bounds = Transformer.getBounds(catalyst);
            if (bounds.getWidth() > widthPerGlyph) widthPerGlyph = bounds.getWidth();
        }
        widthPerGlyph += HORIZONTAL_PADDING;

        final double totalWidth = widthPerGlyph * catalysts.size();
        final double xOffset = 0.5 * (totalWidth - widthPerGlyph);
        layoutHorizontalEntities(layout.getCompartmentRoot(), catalysts, xOffset, widthPerGlyph, (glyph, coord) ->
                Transformer.center(glyph, new CoordinateImpl(coord.getY(), coord.getX())));

    }

    private void layoutRegulators(Layout layout) {
        final List<EntityGlyph> regulators = new ArrayList<>(index.getRegulators());
        if (regulators.isEmpty()) return;
        final List<CompartmentGlyph> compartmentGlyphs = flattenCompartments(layout.getCompartmentRoot());
        regulators.sort(Comparator.comparingInt((EntityGlyph e) -> compartmentGlyphs.indexOf(e.getCompartment())));

        double widthPerGlyph = MIN_GLYPH_WIDTH;
        for (EntityGlyph regulator : regulators) {
            final Position bounds = Transformer.getBounds(regulator);
            if (bounds.getWidth() > widthPerGlyph) widthPerGlyph = bounds.getWidth();
        }
        widthPerGlyph += HORIZONTAL_PADDING;

        final double totalWidth = widthPerGlyph * regulators.size();
        final double xOffset = 0.5 * (totalWidth - widthPerGlyph);
        layoutHorizontalEntities(layout.getCompartmentRoot(), regulators, xOffset, widthPerGlyph, (glyph, coord) ->
                Transformer.center(glyph, new CoordinateImpl(coord.getY(), -coord.getX())));

        double y = Integer.MAX_VALUE;
        double maxX = 0;
        for (final EntityGlyph regulator : regulators) {
            final Position bounds = getBounds(regulator);
            y = Math.min(y, bounds.getY());
            maxX = Math.max(maxX, bounds.getCenterX());
        }
        final double dx = maxX - layout.getReaction().getPosition().getCenterX();
        final double dy = y - layout.getReaction().getPosition().getMaxY();
        double cy = 0;
        if (dy < dx) cy = dx - dy;
        for (final EntityGlyph entity : regulators) Transformer.move(entity, 0, cy);
    }

    /**
     * This is a deep first recursive method, so the entities of the inner compartments are rendered first. Entities are
     * placed top to bottom in the order of appearance in the list.
     *
     * @param compartment    current compartment
     * @param entities       list of sorted entities
     * @param heightPerGlyph height that each glyph occupies, including padding
     * @param apply          operation to apply to entities once its position has been calculated, use this method to
     *                       invert axis or rotate the position.
     */
    private Coordinate layoutVerticalEntities(CompartmentGlyph compartment, List<EntityGlyph> entities, double heightPerGlyph, BiConsumer<EntityGlyph, Coordinate> apply) {
        // Sub compartments are layout first
        double startX = 0;
        double startY = 0;
        for (CompartmentGlyph child : compartment.getChildren()) {
            final Coordinate coordinate = layoutVerticalEntities(child, entities, heightPerGlyph, apply);
            startX = Math.max(startX, coordinate.getX());
            startY = Math.max(startY, coordinate.getY());
        }
        final List<EntityGlyph> glyphs = entities.stream()
                .filter(entityGlyph -> compartment.getContainedGlyphs().contains(entityGlyph))
                .collect(Collectors.toList());
        if (glyphs.isEmpty()) return new CoordinateImpl(COMPARTMENT_PADDING + startX, COMPARTMENT_PADDING + startY);
        if (compact && glyphs.size() > 6) {
            return compact(heightPerGlyph, apply, startX, startY, glyphs);
        } else {
            return oneRow(entities, heightPerGlyph, apply, startX, glyphs);
        }
    }

    /**
     * Lays out nodes in one vertical row, no matter how many nodes
     *
     * @param entities       list of all entities in the axis, helps selecting the y
     * @param heightPerGlyph vertical space for nodes
     * @param apply          function to transform the coordinates at the end
     * @param startX         x coordinate to be the minimum x for the compartment
     * @param glyphs         list of glyphs that fit in this compartment
     * @return a coordinate with the max x and y after laying out all glyphs
     */
    private Coordinate oneRow(List<EntityGlyph> entities, double heightPerGlyph, BiConsumer<EntityGlyph, Coordinate> apply, double startX, List<EntityGlyph> glyphs) {
        double width = MIN_GLYPH_WIDTH;
        for (EntityGlyph entity : entities) {
            final Position bounds = getBounds(entity);
            if (bounds.getWidth() > width) width = bounds.getWidth();
        }
        width += HORIZONTAL_PADDING;
        // Center of all nodes
        final double x = REACTION_MIN_H_DISTANCE + startX + 0.5 * width;
        for (EntityGlyph glyph : glyphs) {
            final int i = entities.indexOf(glyph);
            final double y = i * heightPerGlyph;
            apply.accept(glyph, new CoordinateImpl(x, y));
        }
        return new CoordinateImpl(COMPARTMENT_PADDING + startX + width, COMPARTMENT_PADDING + glyphs.size() * heightPerGlyph);
    }

    /**
     * Lays out glyphs in a grid, avoiding nodes to overlap segments. Max column size is 6.
     *
     * @param heightPerGlyph vertical spacing for nodes
     * @param apply          transformation of generated coordinate
     * @param startX         min x
     * @param glyphs         list of entitites
     * @return the max x and y
     */
    private Coordinate compact(double heightPerGlyph, BiConsumer<EntityGlyph, Coordinate> apply, double startX, double startY, List<EntityGlyph> glyphs) {
        // final int columns = (glyphs.size() - 1) / 6 + 1;
        final int columns = 2; // more columns do not improve height
        // the width of each column
        final double[] ws = new double[columns];
        for (int i = 0; i < glyphs.size(); i++) {
            final double width = getBounds(glyphs.get(i)).getWidth();
            final int j = i % columns;
            if (width > ws[j]) ws[j] = width;
        }
        // the x for each column (the center)
        final double[] xs = new double[columns];
        xs[0] = REACTION_MIN_H_DISTANCE + startX + 0.5 * ws[0];
        for (int i = 1; i < columns; i++) {
            xs[i] = xs[i - 1] + 0.5 * ws[i - 1] + 0.5 * ws[i] + COLUMN_PADDING;
        }
        // the y offset of each column (the center of the first node)
        final double[] ys = new double[columns];
        ys[0] = 0;
        for (int i = 1; i < columns; i++) {
            ys[i] = ys[i - 1] + 0.5 * heightPerGlyph + 1;
        }
        // vertical distance between two nodes in the same column
        final double shift;
        if (columns == 2) shift = heightPerGlyph + 2;
        else shift = ys[columns - 1] + 0.5 * heightPerGlyph;
        // else shift = (columns - 2) * (0.5 * heightPerGlyph + (columns - 1) * 2);
        double x = 0, y = 0;
        for (int i = 0; i < glyphs.size(); i++) {
            final EntityGlyph entityGlyph = glyphs.get(i);
            final int column = i % columns;
            final int row = i / columns;
            x = xs[column];
            y = ys[column] + row * shift;
            apply.accept(entityGlyph, new CoordinateImpl(x, y));
        }
        return new CoordinateImpl(x, y);
    }

    /**
     * This is a deep first recursive method, so the entities of the inner compartments are rendered first. Entities are
     * placed left to right in the order of appearance in the list. All glyph share its <em>y</em> coordinate, depending
     * on the level of the compartment and heights of entities. The <em>x</em> coordinated is calculated as
     * <code>x = -xOffset + i * widthPerGlyph</code>. Glyphs are centered at <em>(x,y)</em>
     *
     * @param compartment   current compartment
     * @param entities      list of sorted entities
     * @param xOffset       position of the first entity in the list
     * @param widthPerGlyph height that each glyph occupies, including padding
     * @param apply         operation to apply to entities once its position has been calculated, use this method to
     *                      invert axis or rotate the position.
     */
    private double layoutHorizontalEntities(CompartmentGlyph compartment, List<EntityGlyph> entities, double xOffset, double widthPerGlyph, BiConsumer<EntityGlyph, Coordinate> apply) {
        double startY = COMPARTMENT_PADDING;
        for (CompartmentGlyph child : compartment.getChildren()) {
            startY = Math.max(startY, layoutHorizontalEntities(child, entities, xOffset, widthPerGlyph, apply));
        }
        final List<EntityGlyph> glyphs = entities.stream()
                .filter(entityGlyph -> compartment.getContainedGlyphs().contains(entityGlyph))
                .collect(Collectors.toList());
        if (glyphs.isEmpty()) return COMPARTMENT_PADDING;
        double height = MIN_GLYPH_HEIGHT;
        for (EntityGlyph entity : entities) {
            final Position bounds = getBounds(entity);
            if (bounds.getHeight() > height) height = bounds.getHeight();
        }
        height += VERTICAL_PADDING;
        final double x = REACTION_MIN_V_DISTANCE + startY + 0.5 * height;
        for (EntityGlyph glyph : glyphs) {
            final int i = entities.indexOf(glyph);
            final double y = -xOffset + i * widthPerGlyph;
            apply.accept(glyph, new CoordinateImpl(-x, y));
        }
        return 2 * COMPARTMENT_PADDING + height;
    }

    /**
     * This step should be run after placing the entities.
     */
    private void layoutReaction(Layout layout) {
        final ReactionGlyph reaction = layout.getReaction();
        setSize(reaction);
        // We have to check whether the reaction is in the inner compartment or not to avoid rendering it over a wrong
        // compartment
        final Position position = reaction.getPosition();
        position.setCenter(0., 0.);
        final Collection<CompartmentGlyph> clashingCompartments = clashingCompartments(layout, reaction);
        if (clashingCompartments.size() > 0) {
            // We add COMPARTMENT_PADDING + 5 just in case reaction has to be moved to a sibling compartment
            final double reactionSep = 5 + COMPARTMENT_PADDING + getBounds(reaction).getWidth() * 0.5;
            if (canMoveTo(index.getInputs(), reaction)) {
                double x = 0;
                for (CompartmentGlyph compartment : clashingCompartments)
                    if (compartment.getPosition().getX() < x) x = compartment.getPosition().getX();
                if (x != 0) {
                    reaction.getPosition().setCenter(x - reactionSep, 0);
                    for (EntityGlyph input : index.getInputs()) move(input, x - reactionSep, 0);
                    followReactionInHorizontal(reaction, index.getCatalysts());
                    followReactionInHorizontal(reaction, index.getRegulators());
                }
            } else if (canMoveTo(index.getOutputs(), reaction)) {
                double x = 0;
                for (CompartmentGlyph child : clashingCompartments) {
                    if (child.getPosition().getMaxX() > x) x = child.getPosition().getMaxX();
                }
                if (x != 0) {
                    reaction.getPosition().setCenter(x + reactionSep, 0);
                    for (EntityGlyph output : index.getOutputs()) move(output, x + reactionSep, 0);
                    followReactionInHorizontal(reaction, index.getCatalysts());
                    followReactionInHorizontal(reaction, index.getRegulators());
                }
            } else if (canMoveTo(index.getRegulators(), reaction)) {
                double y = 0;
                for (CompartmentGlyph child : clashingCompartments) {
                    if (child.getPosition().getMaxY() > y) y = child.getPosition().getMaxY();
                }
                if (y != 0) {
                    reaction.getPosition().setCenter(0, y + reactionSep);
                    for (EntityGlyph regulator : index.getRegulators()) move(regulator, 0, y + reactionSep);
                    followReactionInVertical(reaction, index.getInputs());
                    followReactionInVertical(reaction, index.getOutputs());
                }
            } else if (canMoveTo(index.getCatalysts(), reaction)) {
                double y = 0;
                for (CompartmentGlyph child : clashingCompartments)
                    if (child.getPosition().getY() < y) y = child.getPosition().getY();
                if (y != 0) {
                    reaction.getPosition().setCenter(0, y - reactionSep);
                    for (EntityGlyph catalyst : index.getCatalysts()) move(catalyst, 0, y - reactionSep);
                    followReactionInVertical(reaction, index.getInputs());
                    followReactionInVertical(reaction, index.getOutputs());
                }
            }
            // reaction cannot be moved to any side, it's trapped
        }
        // Add backbones
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getX(), position.getCenterY()),
                new CoordinateImpl(position.getX() - BACKBONE_LENGTH, position.getCenterY())));
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getMaxX(), position.getCenterY()),
                new CoordinateImpl(position.getMaxX() + BACKBONE_LENGTH, position.getCenterY())));
    }

    private void followReactionInVertical(ReactionGlyph reaction, List<EntityGlyph> glyphs) {
        final List<EntityGlyph> canMove = glyphs.stream()
                .filter(g -> !hasRole(g, CATALYST))
                .filter(g -> g.getCompartment() == reaction.getCompartment() || isAncestor(g.getCompartment(), reaction.getCompartment()))
                .collect(Collectors.toList());
        if (canMove.isEmpty()) return;
        double miny = canMove.get(0).getPosition().getY();
        double maxy = canMove.get(canMove.size() - 1).getPosition().getMaxY();
        final double dy = reaction.getPosition().getCenterY() - 0.5 * (maxy + miny);
        for (final EntityGlyph glyph : canMove) {
            Transformer.move(glyph, 0, dy);
        }
    }

    private void followReactionInHorizontal(ReactionGlyph reaction, List<EntityGlyph> glyphs) {
        final List<EntityGlyph> canMove = glyphs.stream()
                .filter(g -> g.getCompartment() == reaction.getCompartment() || isAncestor(g.getCompartment(), reaction.getCompartment()))
                .collect(Collectors.toList());
        if (canMove.isEmpty()) return;
        double minx = canMove.get(0).getPosition().getX();
        double maxx = canMove.get(canMove.size() - 1).getPosition().getMaxX();
        final double dx = reaction.getPosition().getCenterX() - 0.5 * (maxx + minx);
        for (final EntityGlyph glyph : canMove) {
            Transformer.move(glyph, dx, 0);
        }
    }

    /**
     * Get the list of compartments that clash with the reaction.
     */
    private Collection<CompartmentGlyph> clashingCompartments(Layout layout, ReactionGlyph reaction) {
        return layout.getCompartments().stream()
                .filter(compartment -> clashes(reaction, compartment))
                .collect(Collectors.toList());
    }

    /**
     * A compartment clashes with the reaction if they intersect and the reaction does not belong to the compartment or
     * any of its children.
     */
    private boolean clashes(ReactionGlyph reaction, CompartmentGlyph compartment) {
        return compartment != reaction.getCompartment()
                && !isAncestor(compartment, reaction.getCompartment())
                && compartment.getPosition().intersects(reaction.getPosition());
    }

    /**
     * Test if child is a descendant of compartment.
     */
    private boolean isAncestor(CompartmentGlyph compartment, CompartmentGlyph child) {
        CompartmentGlyph parent = child.getParent();
        while (parent != null) {
            if (parent == compartment) return true;
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * A reaction can be moved to a group of entities if none of the entities compartments clash with the reaction.
     */
    private boolean canMoveTo(Collection<EntityGlyph> entities, ReactionGlyph reaction) {
        return entities.stream().allMatch(glyph -> fitsInto(reaction.getCompartment(), glyph.getCompartment()));
    }

    private boolean fitsInto(CompartmentGlyph compartment, CompartmentGlyph parent) {
        return compartment == parent || isAncestor(parent, compartment);
    }

    private void layoutConnectors(Layout layout) {
        inputConnectors(layout);
        outputConnectors(layout);
        catalystConnectors(layout);
        regulatorConnectors(layout);
    }

    private void inputConnectors(Layout layout) {
        // we have to deal with the case that inputs can be catalysts as well, in that case two connector have to be
        // created. This method supposes that the inputs are on the top left corner of the diagram.
        final Position reactionPosition = layout.getReaction().getPosition();
        final double mx = index.getInputs().stream().map(Transformer::getBounds).mapToDouble(Position::getMaxX).max().orElse(0);
        final double vRule = mx + MIN_SEGMENT;
        final double port = reactionPosition.getX() - BACKBONE_LENGTH;
        for (EntityGlyph entity : index.getInputs()) {
            final Position position = entity.getPosition();
            // is catalyst and input
            final boolean biRole = entity.getRoles().size() > 1;
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            // Input
            if (entity.getRenderableClass() == RenderableClass.GENE) {
                // Genes need an extra segment from the arrow
                segments.add(new SegmentImpl(position.getMaxX() + 8, position.getY(),
                        position.getMaxX() + GENE_SEGMENT_LENGTH, position.getCenterY()));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getMaxX() + GENE_SEGMENT_LENGTH, position.getCenterY()),
                        new CoordinateImpl(vRule, position.getCenterY())));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(vRule, position.getCenterY()),
                        new CoordinateImpl(port, reactionPosition.getCenterY())));
            } else {
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getMaxX(), position.getCenterY()),
                        new CoordinateImpl(vRule, position.getCenterY())));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(vRule, position.getCenterY()),
                        new CoordinateImpl(port, reactionPosition.getCenterY())));
            }
            if (biRole) {
                // Add catalyst segments
                final double top = min(position.getY(), reactionPosition.getY()) - 50;
                final double catalystPosition = reactionPosition.getCenterX() - 20;
                segments.add(new SegmentImpl(position.getCenterX(), position.getY(), position.getCenterX(), top));
                segments.add(new SegmentImpl(position.getCenterX(), top, catalystPosition, top));
                segments.add(new SegmentImpl(catalystPosition, top, reactionPosition.getCenterX(), reactionPosition.getCenterY()));
                connector.setPointer(ConnectorType.CATALYST);
            } else {
                connector.setPointer(ConnectorType.INPUT);
            }
            // We expect to have stoichiometry only in input role
            for (Role role : entity.getRoles())
                if (role.getType() == INPUT) {
                    connector.setStoichiometry(getStoichiometry(segments, role));
                    break;
                }
        }
    }

    private ConnectorImpl createConnector(EntityGlyph entity) {
        final ConnectorImpl connector = new ConnectorImpl();
        final List<Segment> segments = new ArrayList<>();
        connector.setSegments(segments);
        entity.setConnector(connector);
        return connector;
    }

    /**
     * Flattens the compartment tree, putting the outermost ones at the beginning. root is always the first
     */
    private List<CompartmentGlyph> flattenCompartments(CompartmentGlyph compartment) {
        final List<CompartmentGlyph> order = new ArrayList<>();
        order.add(compartment);
        for (final CompartmentGlyph child : compartment.getChildren()) {
            order.addAll(flattenCompartments(child));
        }
        return order;
    }


    private void outputConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getMaxX() + BACKBONE_LENGTH;
        final double mx = index.getOutputs().stream().map(Transformer::getBounds).mapToDouble(Position::getX).min().orElse(0);
        final double vRule = mx - MIN_SEGMENT - ARROW_SIZE;
        for (EntityGlyph entity : index.getOutputs()) {
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            final Position position = entity.getPosition();
            segments.add(new SegmentImpl(
                    new CoordinateImpl(position.getX() - 4, position.getCenterY()),
                    new CoordinateImpl(vRule, position.getCenterY())));
            segments.add(new SegmentImpl(
                    new CoordinateImpl(vRule, position.getCenterY()),
                    new CoordinateImpl(port, reactionPosition.getCenterY())));
            // only one role expected: OUTPUT
            for (Role role : entity.getRoles()) {
                connector.setPointer(getConnectorType(role.getType()));
                connector.setStoichiometry(getStoichiometry(segments, role));
            }
        }
    }

    private ConnectorType getConnectorType(EntityRole type) {
        switch (type) {
            case OUTPUT:
                return ConnectorType.OUTPUT;
            case CATALYST:
                return ConnectorType.CATALYST;
            case NEGATIVE_REGULATOR:
                return ConnectorType.INHIBITOR;
            case POSITIVE_REGULATOR:
                return ConnectorType.ACTIVATOR;
            case INPUT:
            default:
                return ConnectorType.INPUT;
        }
    }

    private void catalystConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getCenterY();
        double my = index.getCatalysts().stream().map(Transformer::getBounds).mapToDouble(Position::getMaxY).max().orElse(0);
        final double hRule = my + MIN_SEGMENT;
        for (EntityGlyph entity : index.getCatalysts()) {
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            final Position position = entity.getPosition();
            segments.add(new SegmentImpl(
                    new CoordinateImpl(position.getCenterX(), position.getMaxY()),
                    new CoordinateImpl(position.getCenterX(), hRule)));
            segments.add(new SegmentImpl(
                    new CoordinateImpl(position.getCenterX(), hRule),
                    new CoordinateImpl(reactionPosition.getCenterX(), port)));
            // only one role expected: CATALYST
            for (Role role : entity.getRoles()) {
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setType(role.getType().name());
                connector.setPointer(getConnectorType(role.getType()));
            }
        }
    }

    private void regulatorConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        double my = index.getRegulators().stream().map(Transformer::getBounds).mapToDouble(Position::getY).min().orElse(0);
        final double hRule = my - MIN_SEGMENT;
        // we want to fit all catalysts in a semi-circumference, not using the corners
        final int sectors = index.getRegulators().size() + 1;
        // the semicircle is centered into the reaction, and its length (PI*radius) should be enough to fit all the
        // shapes without touching each other
        final double radius = reactionPosition.getHeight() / 2 + REGULATOR_SIZE * sectors / PI;
        int i = 1;
        final ArrayList<EntityGlyph> regulators = new ArrayList<>(index.getRegulators());
        regulators.sort(Comparator.comparingDouble(v -> v.getPosition().getCenterX()));
        for (EntityGlyph entity : regulators) {
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            final Position position = entity.getPosition();
            segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(), position.getCenterX(), hRule));
            final double x = reactionPosition.getCenterX() - radius * cos(PI * i / sectors);
            final double y = reactionPosition.getCenterY() + radius * sin(PI * i / sectors);
            segments.add(new SegmentImpl(position.getCenterX(), hRule, x, y));
            // Only one role expected (negative or positive)
            for (Role role : entity.getRoles()) {
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setPointer(getConnectorType(role.getType()));
            }
            i++;
        }
    }

    /**
     * Creates the stoichiometry box in the first segment.
     */
    private Stoichiometry getStoichiometry(List<Segment> segments, Role role) {
        if (role.getStoichiometry() == 1)
            return new StoichiometryImpl(1, null);
        final Segment segment = segments.get(0);
        final Coordinate center = center(segment);
        final Coordinate a = new CoordinateImpl(center.getX() - 6, center.getY() - 6);
        final Coordinate b = new CoordinateImpl(center.getX() + 6, center.getY() + 6);
        final Shape shape = new BoxImpl(a, b, true, role.getStoichiometry().toString());
        return new StoichiometryImpl(role.getStoichiometry(), shape);
    }

    private Coordinate center(Segment segment) {
        return new CoordinateImpl(
                0.5 * (segment.getFrom().getX() + segment.getTo().getX()),
                0.5 * (segment.getFrom().getY() + segment.getTo().getY())
        );
    }

    private void layoutCompartments(Layout layout) {
        layoutCompartment(layout.getCompartmentRoot());
    }

    /**
     * Calculates the size of the compartments so each of them surrounds all of its contained glyphs and children.
     */
    private void layoutCompartment(CompartmentGlyph compartment) {
        for (CompartmentGlyph child : compartment.getChildren()) {
            layoutCompartment(child);
        }
        Position position = null;
        for (CompartmentGlyph child : compartment.getChildren()) {
            if (position == null) position = new Position(child.getPosition());
            else position.union(child.getPosition());
        }
        for (Glyph glyph : compartment.getContainedGlyphs()) {
            if (position == null) position = new Position(getBounds(glyph));
            else position.union(getBounds(glyph));
        }
        position.setX(position.getX() - COMPARTMENT_PADDING);
        position.setY(position.getY() - COMPARTMENT_PADDING);
        position.setWidth(position.getWidth() + 2 * COMPARTMENT_PADDING);
        position.setHeight(position.getHeight() + 2 * COMPARTMENT_PADDING);

        final double textWidth = FontProperties.getTextWidth(compartment.getName());
        final double textHeight = FontProperties.getTextHeight();
        final double textPadding = textWidth + 30;
        // If the text is too large, we increase the size of the compartment
        if (position.getWidth() < textPadding) {
            double diff = textPadding - position.getWidth();
            position.setWidth(textPadding);
            position.setX(position.getX() - 0.5 * diff);
        }
        // Puts text in the bottom right corner of the compartment
        final Coordinate coordinate = new CoordinateImpl(
                position.getMaxX() - textWidth - 15,
                position.getMaxY() + 0.5 * textHeight - COMPARTMENT_PADDING);
        compartment.setLabelPosition(coordinate);
        compartment.setPosition(position);
    }

    private void computeDimension(Layout layout) {
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            layout.getPosition().union(compartment.getPosition());
        }
        for (EntityGlyph entity : layout.getEntities()) {
            layout.getPosition().union(Transformer.getBounds(entity));
            for (final Segment segment : entity.getConnector().getSegments()) {
                final double minX = Math.min(segment.getFrom().getX(), segment.getTo().getX());
                final double maxX = Math.max(segment.getFrom().getX(), segment.getTo().getX());
                final double minY = Math.min(segment.getFrom().getY(), segment.getTo().getY());
                final double maxY = Math.max(segment.getFrom().getY(), segment.getTo().getY());
                layout.getPosition().union(new Position(minX, minY, maxX - minX, maxY - minY));
            }
        }
        layout.getPosition().union(Transformer.getBounds(layout.getReaction()));
    }

    private void moveToOrigin(Layout layout) {
        final double dx = -layout.getPosition().getX();
        final double dy = -layout.getPosition().getY();
        final Coordinate delta = new CoordinateImpl(dx, dy);
        layout.getPosition().move(dx, dy);
        move(layout.getCompartmentRoot(), delta, true);
    }

    /**
     * This operation should be called in the last steps, to avoid being exported to a Diagram object.
     */
    private void removeExtracellular(Layout layout) {
        layout.getCompartments().remove(layout.getCompartmentRoot());
    }

    /**
     * Adds some extra spacing when no inputs or outputs are present
     */
    private void failedReactions(Layout layout) {
        if (index.getOutputs().isEmpty()) layout.getPosition().setWidth(layout.getPosition().getWidth() + 150);
        if (index.getInputs().isEmpty()) {
            layout.getPosition().setWidth(layout.getPosition().getWidth() + 150);
        }
    }

}
