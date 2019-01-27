package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

/**
 * Matrix coordinates expressed as <b>row</b> and <b>col</b>. Useful to index a {@link Grid}.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Point {
    private int row;
    private int col;

    public Point(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public void setRow(int row) {
        this.row = row;
    }

    Point add(Point point) {
        return new Point(row + point.row, col + point.col);
    }
}
