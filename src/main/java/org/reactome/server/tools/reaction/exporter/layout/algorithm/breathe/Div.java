package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;

import java.util.Collection;

public interface Div {

    Position getBounds();

    void setPadding(double padding);

    void setHorizontalPadding(double padding);

    void setVerticalPadding(double padding);

    void center(double x, double y);

    void move(double dx, double dy);

    Collection<EntityRole> getContainedRoles();

    CompartmentGlyph getCompartment();
}
