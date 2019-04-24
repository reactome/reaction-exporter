package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.tools.reaction.exporter.layout.common.Bounds;

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

    private Bounds bounds = new Bounds();

    AbstractGlyph() {
        id = NEXT_ID.incrementAndGet();
    }

    public Long getId() {
        return id;
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
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
