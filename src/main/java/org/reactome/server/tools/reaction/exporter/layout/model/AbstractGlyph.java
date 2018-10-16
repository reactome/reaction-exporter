package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Common attributes and methods to all glyphs
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public abstract class AbstractGlyph implements Glyph{

    private final static AtomicLong NEXT_ID = new AtomicLong();
    private final long id;

    private Position position = new Position();

    AbstractGlyph() {
        id = NEXT_ID.getAndIncrement();
    }

    public Long getId() {
        return id;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractGlyph that = (AbstractGlyph) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
