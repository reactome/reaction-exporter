package org.reactome.server.tools.reaction.exporter.renderer;

import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.*;
import org.reactome.server.tools.reaction.exporter.renderer.text.TextUtils;

import java.awt.geom.Dimension2D;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class LayoutRenderer {

//	private static final Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
//	private static final FontMetrics FONT_METRICS = graphics.getFontMetrics(new Font("arial", Font.BOLD, 8));


	private static final int COMPARTMENT_PADDING = 20;

	private static final List<RenderableClass> CLASS_ORDER = Arrays.asList(
			RenderableClass.PROCESS_NODE,
			RenderableClass.ENCAPSULATED_NODE,
			RenderableClass.COMPLEX,
			RenderableClass.ENTITY_SET,
			RenderableClass.PROTEIN,
			RenderableClass.RNA,
			RenderableClass.CHEMICAL,
			RenderableClass.GENE,
			RenderableClass.ENTITY);
	private static final Comparator<Boolean> FALSE_FIRST = Comparator.nullsFirst((o1, o2) -> o1.equals(o2) ? 0 : o1 ? 1 : -1);
	private static final int MIN_GLYPH_HEIGHT = 40;
	private static final double VERTICAL_PADDING = 15;
	private static final double MIN_GLYPH_WIDTH = 80;
	private static final double HORIZONTAL_PADDING = 15;
	private static final double REACTION_MIN_DISTANCE = 120;

	public void apply(Layout layout) {
		addDuplicates(layout);
		layoutReaction(layout);
		layoutParticipants(layout);
		layoutCompartments(layout);

	}

	private void addDuplicates(Layout layout) {
		// Duplicate entities
		// only when roles are not consecutive
		// in case it is in two or three consecutive roles, no duplication is needed
		final List<EntityGlyph> added = new ArrayList<>();
		for (EntityGlyph entity : layout.getEntities()) {
			final Collection<Role> roles = entity.getRoles();
			if (roles.size() > 1) {
				final Set<EntityRole> roleSet = roles.stream().map(Role::getType).collect(Collectors.toSet());
				if (roleSet.equals(EnumSet.of(EntityRole.INPUT, EntityRole.OUTPUT))
						|| roleSet.equals(EnumSet.of(EntityRole.CATALYST, EntityRole.POSITIVE_REGULATOR))
						|| roleSet.equals(EnumSet.of(EntityRole.CATALYST, EntityRole.NEGATIVE_REGULATOR))) {
					// When we have 2 opposite roles, one remains in entity and the other is set to copy
					final Role role = roles.iterator().next();
					final EntityGlyph copy = new EntityGlyph(entity);
					copy.setRole(role);
					entity.getRoles().remove(role);
					added.add(copy);
					addCopyToCompartment(layout, entity, copy);
				} else if (roleSet.equals(EnumSet.of(EntityRole.CATALYST, EntityRole.NEGATIVE_REGULATOR, EntityRole.POSITIVE_REGULATOR))) {
					// Special case: catalyst/positive/negative
					// In this case we split in 2 entities: catalyst and positive/negative
					final Role role = roles.stream().filter(r -> r.getType() == EntityRole.CATALYST).findFirst().orElse(null);
					final EntityGlyph copy = new EntityGlyph(entity);
					copy.setRole(role);
					entity.getRoles().remove(role);
					added.add(copy);
					addCopyToCompartment(layout, entity, copy);
				}
			}
		}
		for (EntityGlyph entity : added) {
			layout.add(entity);
		}
		for (EntityGlyph entity : layout.getEntities()) {
			setSize(entity);
		}
	}


	private void setSize(EntityGlyph glyph) {
		final Dimension2D dimension = TextUtils.splitText(glyph.getName());
		switch (glyph.getRenderableClass()) {
			case ATTACHMENT:
				glyph.getPosition().setWidth(12);
				glyph.getPosition().setHeight(12);
				break;
			case CHEMICAL:
			case CHEMICAL_DRUG:
			case COMPLEX:
			case COMPLEX_DRUG:
			case ENTITY:
			case PROTEIN:
			case PROTEIN_DRUG:
			case RNA:
			case RNA_DRUG:
				glyph.getPosition().setWidth(6 + dimension.getWidth());
				glyph.getPosition().setHeight(6 + dimension.getHeight());
				break;
			case ENCAPSULATED_NODE:
			case PROCESS_NODE:
			case ENTITY_SET:
			case ENTITY_SET_DRUG:
				glyph.getPosition().setWidth(15 + dimension.getWidth());
				glyph.getPosition().setHeight(15 + dimension.getHeight());
				break;
			case REACTION:
				glyph.getPosition().setHeight(12);
				glyph.getPosition().setWidth(12);
				break;
			case GENE:
				glyph.getPosition().setWidth(6 + dimension.getWidth());
				glyph.getPosition().setHeight(30 + dimension.getHeight());
				break;
		}
	}

	private void addCopyToCompartment(Layout layout, EntityGlyph entity, EntityGlyph copy) {
		for (CompartmentGlyph compartment : layout.getCompartments()) {
			if (compartment.getContainedGlyphs().contains(entity)) {
				compartment.getContainedGlyphs().add(copy);
				break;
			}
		}
	}

	private void layoutReaction(Layout layout) {
		layout.getReaction().getPosition().setCenter(0, 0);
	}

	private void layoutParticipants(Layout layout) {
		final Map<EntityRole, Collection<EntityGlyph>> participants = new HashMap<>();
		for (EntityGlyph entity : layout.getEntities()) {
			for (Role role : entity.getRoles()) {
				participants.computeIfAbsent(role.getType(), r -> new ArrayList<>()).add(entity);
			}
		}
		inputs(layout, participants.get(EntityRole.INPUT));
		outputs(layout, participants.get(EntityRole.OUTPUT));
		catalysts(layout, participants.get(EntityRole.CATALYST));
		final ArrayList<EntityGlyph> regulators = new ArrayList<>(participants.getOrDefault(EntityRole.NEGATIVE_REGULATOR, Collections.emptyList()));
		regulators.addAll(participants.getOrDefault(EntityRole.POSITIVE_REGULATOR, Collections.emptyList()));
		regulators(layout, regulators);
	}

	private void inputs(Layout layout, Collection<EntityGlyph> entities) {
		final ArrayList<EntityGlyph> inputs = new ArrayList<>(entities);
		inputs.sort(Comparator
				.comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
				.thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
				.thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass())));

		double heightPerGlyph = MIN_GLYPH_HEIGHT;
		for (EntityGlyph input : inputs) {
			if (input.getPosition().getHeight() > heightPerGlyph) {
				heightPerGlyph = input.getPosition().getHeight();
			}
		}
		heightPerGlyph += VERTICAL_PADDING;
		final double totalHeight = heightPerGlyph * inputs.size();
		final double yOffset = 0.5 * totalHeight;
		layoutVerticalEntities(layout.getCompartmentRoot(), inputs, yOffset, heightPerGlyph, (glyph, coord) -> {
			glyph.getPosition().setX(-coord.getX());
			glyph.getPosition().setY(coord.getY());
		});
	}

	private void outputs(Layout layout, Collection<EntityGlyph> entities) {
		final ArrayList<EntityGlyph> outputs = new ArrayList<>(entities);
		outputs.sort(Comparator
				.comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
				.thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
				.thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass())));

		double heightPerGlyph = MIN_GLYPH_HEIGHT;
		for (EntityGlyph input : outputs) {
			if (input.getPosition().getHeight() > heightPerGlyph) {
				heightPerGlyph = input.getPosition().getHeight();
			}
		}
		heightPerGlyph += VERTICAL_PADDING;
		final double totalHeight = heightPerGlyph * outputs.size();
		final double yOffset = 0.5 * totalHeight;
		layoutVerticalEntities(layout.getCompartmentRoot(), outputs, yOffset, heightPerGlyph, (glyph, coord) -> {
			glyph.getPosition().setX(coord.getX());
			glyph.getPosition().setY(coord.getY());
		});
	}

	private void catalysts(Layout layout, Collection<EntityGlyph> entities) {
		final ArrayList<EntityGlyph> catalysts = new ArrayList<>(entities);
		catalysts.sort(Comparator
				.comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
				.thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
				.thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass())));

		double widthPerGlyph = MIN_GLYPH_WIDTH;
		for (EntityGlyph input : catalysts) {
			if (input.getPosition().getWidth() > widthPerGlyph) {
				widthPerGlyph = input.getPosition().getWidth();
			}
		}
		widthPerGlyph += HORIZONTAL_PADDING;
		final double totalWidth = widthPerGlyph * catalysts.size();
		final double xOffset = 0.5 * totalWidth;
		layoutHorizontalEntities(layout.getCompartmentRoot(), catalysts, xOffset, widthPerGlyph, (glyph, coord) -> {
			glyph.getPosition().setX(coord.getY());
			glyph.getPosition().setY(coord.getX());
		});
	}

	private void regulators(Layout layout, Collection<EntityGlyph> entities) {
		final ArrayList<EntityGlyph> regulators = new ArrayList<>(entities);
		regulators.sort(Comparator
				.comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
				.thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
				.thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass())));

		double widthPerGlyph = MIN_GLYPH_WIDTH;
		for (EntityGlyph input : regulators) {
			if (input.getPosition().getWidth() > widthPerGlyph) {
				widthPerGlyph = input.getPosition().getWidth();
			}
		}
		widthPerGlyph += HORIZONTAL_PADDING;
		final double totalWidth = widthPerGlyph * regulators.size();
		final double xOffset = 0.5 * totalWidth;
		layoutHorizontalEntities(layout.getCompartmentRoot(), regulators, xOffset, widthPerGlyph, (glyph, coord) -> {
			glyph.getPosition().setX(coord.getY());
			glyph.getPosition().setY(-coord.getX());
		});
	}

	private double layoutVerticalEntities(CompartmentGlyph compartment, List<EntityGlyph> entities, double yOffset, double heightPerGlyph, BiConsumer<Glyph, Coordinate> apply) {
		double startX = 0;
		for (CompartmentGlyph child : compartment.getChildren()) {
			startX = Math.max(startX, layoutVerticalEntities(child, entities, yOffset, heightPerGlyph, apply));
		}
		final List<EntityGlyph> glyphs = entities.stream()
				.filter(entityGlyph -> compartment.getContainedGlyphs().contains(entityGlyph))
				.collect(Collectors.toList());
		if (glyphs.isEmpty()) return COMPARTMENT_PADDING;
		double width = MIN_GLYPH_WIDTH;
		for (EntityGlyph input : entities) {
			if (input.getPosition().getWidth() > width)
				width = input.getPosition().getWidth();
		}
		width += HORIZONTAL_PADDING;
		final double x = REACTION_MIN_DISTANCE + startX + 0.5 * width;
		for (EntityGlyph glyph : glyphs) {
			final int i = entities.indexOf(glyph);
			final double y = -yOffset + i * heightPerGlyph;
			apply.accept(glyph, new Coordinate((int) -x, (int) y));
		}
		return COMPARTMENT_PADDING + width;
	}

	private double layoutHorizontalEntities(CompartmentGlyph compartment, List<EntityGlyph> entities, double xOffset, double widthPerGlyph, BiConsumer<Glyph, Coordinate> apply) {
		double startY = 0;
		for (CompartmentGlyph child : compartment.getChildren()) {
			startY = Math.max(startY, layoutHorizontalEntities(child, entities, xOffset, widthPerGlyph, apply));
		}
		final List<EntityGlyph> glyphs = entities.stream()
				.filter(entityGlyph -> compartment.getContainedGlyphs().contains(entityGlyph))
				.collect(Collectors.toList());
		if (glyphs.isEmpty()) return COMPARTMENT_PADDING;
		final double height = entities.stream().map(Glyph::getPosition).mapToDouble(Position::getHeight).max().orElse((double) MIN_GLYPH_HEIGHT) + VERTICAL_PADDING;
		final double x = REACTION_MIN_DISTANCE + startY + 0.5 * height;
		for (EntityGlyph glyph : glyphs) {
			final int i = entities.indexOf(glyph);
			final double y = -xOffset + i * widthPerGlyph;
			apply.accept(glyph, new Coordinate((int) -x, (int) y));
		}
		return COMPARTMENT_PADDING + height;
	}

	private void layoutCompartments(Layout layout) {
		layoutCompartment(layout.getCompartmentRoot());
	}

	private void layoutCompartment(CompartmentGlyph compartment) {
		for (CompartmentGlyph child : compartment.getChildren()) {
			layoutCompartment(child);
		}
		final Position position = compartment.getPosition();
		for (CompartmentGlyph child : compartment.getChildren()) {
			union(position, child.getPosition());
		}
		for (Glyph glyph : compartment.getContainedGlyphs()) {
			union(position, glyph.getPosition());
		}
		position.setX(position.getX() - COMPARTMENT_PADDING);
		position.setY(position.getY() - COMPARTMENT_PADDING);
		position.setWidth(position.getWidth() + 2 * COMPARTMENT_PADDING);
		position.setHeight(position.getHeight() + 2 * COMPARTMENT_PADDING);

		compartment.setLabelPosition(position.getCenterX(), 0.5 * COMPARTMENT_PADDING);
	}

	/**
	 * Creates the union between <em>a</em> and <em>b</em> and sets the result into a. The union of two
	 * rectangles is defined as the smallest rectangle that contains both rectangles.
	 *
	 * @param a where the result is to be stored
	 * @param b the second position for the union
	 */
	private void union(Position a, Position b) {
		a.setX(Math.min(a.getX(), b.getX()));
		a.setY(Math.min(a.getY(), b.getY()));
		final double maxX = Math.max(a.getMaxX(), b.getMaxX());
		final double maxY = Math.max(a.getMaxY(), b.getMaxY());
		a.setWidth(maxX - a.getX());
		a.setHeight(maxY - a.getY());
	}


//	private LayoutResult layoutInputs(CompartmentGlyph compartment, java.util.List<EntityGlyph> nodes, double distance) {
//		int laidOut = 0;
//		double thisDistance = 0;
//		for (CompartmentGlyph child : compartment.getChildren()) {
//			final LayoutResult layoutResult = layoutInputs(child, nodes, distance);
//			laidOut += layoutResult.elements;
//			if (thisDistance < layoutResult.distance)
//				thisDistance = layoutResult.distance;
//		}
//		final java.util.List<EntityGlyph> entities = nodes.stream()
//				.filter(EntityGlyph -> EntityGlyph.getCompartment() == compartment)
//				.sorted(PARTICIPANT_COMPARATOR)
//				.collect(Collectors.toList());
//
//		if (entities.isEmpty()) return new LayoutResult(laidOut, thisDistance + COMPARTMENT_PADDING);
//
//		final double maxHeight = entities.stream().mapToDouble(LayoutObject::getHeight).max().orElse(40) + 5;
//		final double maxWidth = entities.stream().mapToDouble(LayoutObject::getWidth).max().orElse(120) + COMPARTMENT_PADDING;
//
//		// Forces a grid for entities
////		final int elementsPerCol = (int) (MAX_HEIGHT / maxHeight);
//		// One column inputs
//		final int elementsPerCol = nodes.size();
//		final int cols = ceilIntegerDivision(entities.size(), elementsPerCol);
//
//		final double vShift = elementsPerCol * maxHeight * 0.5 - 0.5 * maxHeight;
//
//		// Using grid of (cols x elementsPerCol)
//		for (int i = 0; i < entities.size(); i++) {
//			final int col = (laidOut + i) / elementsPerCol;
//			int row = (laidOut + i) % elementsPerCol;
//			if (col == cols - 1) {
//				final int lastColEmpties = (cols * elementsPerCol) % entities.size();
//				row += lastColEmpties / 2;
//			}
//			final double x = distance + col * maxWidth + thisDistance;
//			final double y = row * maxHeight - vShift;
//			entities.get(i).setCenter(-x, y);
//		}
//		return new LayoutResult(entities.size() + laidOut, thisDistance + cols * maxWidth + COMPARTMENT_PADDING);
//	}
//
//	private LayoutResult layoutOutputs(CompartmentGlyph compartment, java.util.List<EntityGlyph> nodes, double distance) {
//		int laidOut = 0;
//		double thisDistance = 0;
//		for (CompartmentGlyph child : compartment.getChildren()) {
//			final LayoutResult layoutResult = layoutOutputs(child, nodes, distance);
//			laidOut += layoutResult.elements;
//			if (thisDistance < layoutResult.distance)
//				thisDistance = layoutResult.distance;
//		}
//		final java.util.List<EntityGlyph> entities = nodes.stream()
//				.filter(EntityGlyph -> EntityGlyph.getCompartment() == compartment)
//				.sorted(PARTICIPANT_COMPARATOR)
//				.collect(Collectors.toList());
//
//		if (entities.isEmpty()) return new LayoutResult(laidOut, thisDistance + COMPARTMENT_PADDING);
//
//		final double maxHeight = entities.stream().mapToDouble(LayoutObject::getHeight).max().orElse(40) + 5;
//		final double maxWidth = entities.stream().mapToDouble(LayoutObject::getWidth).max().orElse(120) + COMPARTMENT_PADDING;
//
//		// Forces a grid for entities
////		final int elementsPerCol = (int) (MAX_HEIGHT / maxHeight);
//		// One column inputs
//		final int elementsPerCol = nodes.size();
//		final int cols = ceilIntegerDivision(entities.size(), elementsPerCol);
//
//		final double vShift = elementsPerCol * maxHeight * 0.5 - 0.5 * maxHeight;
//
//		// Using grid of (cols x elementsPerCol)
//		for (int i = 0; i < entities.size(); i++) {
//			final int col = (laidOut + i) / elementsPerCol;
//			int row = (laidOut + i) % elementsPerCol;
//			if (col == cols - 1) {
//				final int lastColEmpties = (cols * elementsPerCol) % entities.size();
//				row += lastColEmpties / 2;
//			}
//			final double x = distance + col * maxWidth + thisDistance;
//			final double y = row * maxHeight - vShift;
//			entities.get(i).setCenter(x, y);
//		}
//		return new LayoutResult(entities.size() + laidOut, thisDistance + cols * maxWidth + COMPARTMENT_PADDING);
//	}
//
//	private LayoutResult layoutCatalysts(ReactionGlyph reaction, CompartmentGlyph compartment, java.util.List<EntityGlyph> nodes, double distance) {
//		int laidOut = 0;
//		double thisDistance = 0;
//		for (CompartmentGlyph child : compartment.getChildren()) {
//			final LayoutResult result = layoutCatalysts(reaction, child, nodes, distance);
//			laidOut += result.elements;
//			if (thisDistance < result.distance)
//				thisDistance = result.distance;
//		}
//		final java.util.List<EntityGlyph> entities = nodes.stream()
//				.filter(entity -> entity.getCompartment() == compartment)
//				.sorted(PARTICIPANT_COMPARATOR)
//				.collect(Collectors.toList());
//
//		if (entities.isEmpty()) return new LayoutResult(laidOut, thisDistance + COMPARTMENT_PADDING);
//
//		final double maxHeight = entities.stream().mapToDouble(LayoutObject::getHeight).max().orElse(40) + 5;
//		final double maxWidth = entities.stream().mapToDouble(LayoutObject::getWidth).max().orElse(120) + COMPARTMENT_PADDING;
//
//		final int elementsPerRow = (int) (MAX_WIDTH / maxWidth);
//		final int rows = ceilIntegerDivision(entities.size(), elementsPerRow);
//
//		// Using size
//		for (int i = 0; i < entities.size(); i++) {
//			// order in the total elements laid out
//			int absoluteOrder = (laidOut + i) % elementsPerRow;
//			// order in the side of the circle
//			int relativeOrder = absoluteOrder / 2;
//			double x = relativeOrder * maxWidth;
//			if (nodes.size() % 2 == 0) x += 0.5 * maxWidth;
//			if (absoluteOrder % 2 == 1) x = -x;
//
//			double y = distance + thisDistance + (i / elementsPerRow) * maxHeight;
//			entities.get(i).setCenter(x, -y);
//		}
//		return new LayoutResult(entities.size() + laidOut, thisDistance + rows * maxHeight + COMPARTMENT_PADDING);
//	}
//
//	private LayoutResult layoutRegulators(ReactionGlyph reaction, CompartmentGlyph compartment, java.util.List<EntityGlyph> nodes, double distance) {
//		int laidOut = 0;
//		double thisDistance = 0;
//		for (CompartmentGlyph child : compartment.getChildren()) {
//			final LayoutResult result = layoutRegulators(reaction, child, nodes, distance);
//			laidOut += result.elements;
//			if (thisDistance < result.distance)
//				thisDistance = result.distance;
//		}
//		final List<EntityGlyph> entities = nodes.stream()
//				.filter(EntityGlyph -> EntityGlyph.getCompartment() == compartment)
//				.sorted(PARTICIPANT_COMPARATOR)
//				.collect(Collectors.toList());
//
//		if (entities.isEmpty()) return new LayoutResult(laidOut, thisDistance + COMPARTMENT_PADDING);
//
//		final double maxHeight = entities.stream().mapToDouble(LayoutObject::getHeight).max().orElse(40) + 5;
//		final double maxWidth = entities.stream().mapToDouble(LayoutObject::getWidth).max().orElse(120) + COMPARTMENT_PADDING;
//
//		final int elementsPerRow = (int) (MAX_WIDTH / maxWidth);
//		final int rows = ceilIntegerDivision(entities.size(), elementsPerRow);
//
//		// Using size
//		for (int i = 0; i < entities.size(); i++) {
//			// order in the total elements laid out
//			int absoluteOrder = (laidOut + i) % elementsPerRow;
//			// order in the side of the circle
//			int relativeOrder = absoluteOrder / 2;
//			double x = relativeOrder * maxWidth;
//			if (nodes.size() % 2 == 0) x += 0.5 * maxWidth;
//			if (absoluteOrder % 2 == 1) x = -x;
//
//			double y = distance + thisDistance + (i / elementsPerRow) * maxHeight;
//			entities.get(i).setCenter(x, y);
//		}
//		return new LayoutResult(entities.size() + laidOut, thisDistance + rows * maxHeight + COMPARTMENT_PADDING);
//	}
//
//	private int ceilIntegerDivision(int num, int divisor) {
//		return (divisor + num - 1) / divisor;
//	}
//
//	private class LayoutResult {
//		private int elements;
//		private double distance;
//
//		LayoutResult(int elements, double distance) {
//			this.elements = elements;
//			this.distance = distance;
//		}
//	}
}
