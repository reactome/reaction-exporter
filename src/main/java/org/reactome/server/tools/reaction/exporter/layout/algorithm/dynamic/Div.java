package org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic;

import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;

import java.util.Set;

public interface Div {

    Position getBounds();

    void setPadding(double padding);

    void setHorizontalPadding(double padding);

    void setVerticalPadding(double padding);

    void center(double x, double y);

    void move(double dx, double dy);

    Set<EntityRole> containedRoles();
}
