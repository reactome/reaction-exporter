package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

import org.reactome.server.tools.reaction.exporter.layout.common.Bounds;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;

import java.util.Collection;

/**
 * Main container. It works like HTML div element. It can be grouped using {@link Grid} class. Its properties are:
 *
 * <dl>
 *     <dt>bounds</dt>
 *     <dd>size and postion of the div. Includes the padding.</dd>
 *     <dt>containdedRoles</dt>
 *     <dd>set of roles that are inside this Div. It should be recursive.</dd>
 *     <dt>busy places</dt>
 *     <dd>set of places this Div occupies. It should be recursive.</dd>
 *     <dt>compartment</dt>
 *     <dd>compartment around this Div. Only the top one.</dd>
 *     <dt>padding (left, right, top, bottom)</dt>
 *     <dd>empty space between the elements inside the div and the elements surrounding this div. There are shortcuts
 *     for left and right (horizontal) or top and bottom (vertical) or all.</dd>
 * </dl>
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public interface Div extends HasInitial {

    Bounds getBounds();

    void setPadding(double padding);

    void setHorizontalPadding(double padding);

    void setVerticalPadding(double padding);

    double getLeftPadding();

    void setLeftPadding(double padding);

    double getRightPadding();

    void setRightPadding(double padding);

    double getTopPadding();

    void setTopPadding(double padding);

    double getBottomPadding();

    void setBottomPadding(double padding);

    void center(double x, double y);

    void move(double dx, double dy);

    Collection<EntityRole> getContainedRoles();

    Collection<Place> getBusyPlaces();

    CompartmentGlyph getCompartment();
}
