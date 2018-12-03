package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.junit.Test;

public class GridTest {

    @Test
    public void testGrid() {
        final Grid<String> grid = new Grid<>(String.class);
        grid.set(5, 5, "A");
        grid.set(2, 3, "B");
        grid.set(0, 2, "B");
        grid.removeRows(4, 0);
        grid.removeColumns(3, 0);
    }

}