package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

import java.lang.reflect.Array;
import java.util.StringJoiner;

/**
 * Generic matrix, indexed by row and col. All rows have the same number of columns. It wraps an Array of Arrays, and
 * contain operations to add or remove rows and columns and work with columns as rows.
 *
 * @param <T> type of elements in grid
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Grid<T extends HasInitial> {

    private int rows;
    private int columns;
    private T[][] grid;
    private Class<?> clz;

    Grid(Class<T> clz, T[][] grid) {
        this.clz = clz;
        this.grid = grid;
        rows = grid.length;
        columns = grid[0].length;
    }

    public Grid(Class<T> clz) {
        this(clz, 0, 0);
    }

    public Grid(Class<T> clz, int rows, int columns) {
        this.clz = clz;
        this.rows = rows;
        this.columns = columns;
        this.grid = createGrid(rows, columns);
    }

    public Grid(Grid<T> that) {
        this.clz = that.clz;
        this.rows = that.rows;
        this.columns = that.columns;
        this.grid = createGrid(rows, columns);
        for (int r = 0; r < rows; r++) {
            System.arraycopy(that.grid[r], 0, this.grid[r], 0, columns);
        }
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public T[][] getGrid() {
        return grid;
    }

    public T get(int row, int column) {
        return grid[row][column];
    }

    public void set(int row, int column, T element) {
        if (rows <= row)
            insertRows(rows, row - rows + 1);
        if (columns <= column)
            insertColumns(columns, column - columns + 1);
        grid[row][column] = element;
    }

    /**
     * Creates n rows starting in index
     *
     * @param index position of the first inserted row
     * @param n     number of columns to insert
     */
    public void insertRows(int index, int n) {
        if (n <= 0) return;
        final T[][] rtn = createGrid(rows + n, columns);
        if (index >= 0) System.arraycopy(grid, 0, rtn, 0, index);
        if (rows - index >= 0) System.arraycopy(grid, index, rtn, index + n, rows - index);
        rows += n;
        grid = rtn;
    }

    /**
     * Inserts n columns just before the column index
     *
     * @param index position where to insert columns
     * @param n     number of columns to insert
     */
    public void insertColumns(int index, int n) {
        if (n <= 0) return;
        final T[][] rtn = createGrid(rows, columns + n);
        for (int r = 0; r < rows; r++) {
            if (index >= 0) System.arraycopy(grid[r], 0, rtn[r], 0, index);
            if (columns - index >= 0)
                System.arraycopy(grid[r], index, rtn[r], index + n, columns - index);
        }
        columns += n;
        grid = rtn;
    }

    /**
     * Creates one row in index. This is a shortcut for <code>insertRows(index, 1)</code>.
     *
     * @param index position of the inserted row
     */
    public void insertRow(int index) {
        insertRows(index, 1);
    }

    /**
     * Creates one column in index. This is a shortcut for <code>insertColumns(index, 1)</code>.
     *
     * @param index position of the inserted row
     */
    public void insertColumn(int index) {
        insertColumns(index, 1);
    }


    public T[] getRow(int row) {
        return row < rows ? grid[row] : null;
    }

    public T[] getColumn(int col) {
        if (col < columns) {
            final T[] rtn = createArray(rows);
            for (int i = 0; i < grid.length; i++) {
                final T[] row = grid[i];
                rtn[i] = row[col];
            }
            return rtn;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private T[] createArray(int n) {
        return (T[]) Array.newInstance(clz, n);
    }

    @SuppressWarnings("unchecked")
    private T[][] createGrid(int rows, int cols) {
        return (T[][]) Array.newInstance(clz, rows, cols);
    }

    public void removeRows(int index, int n) {
        n = Math.min(n, rows - index);
        if (n <= 0) return;
        int p = index + n;
        final T[][] rtn = createGrid(rows - n, columns);
        if (index >= 0) System.arraycopy(grid, 0, rtn, 0, index);
        if (p < rows) System.arraycopy(grid, p, rtn, index, rows - p);
        rows -= n;
        grid = rtn;
    }

    public void removeColumns(int index, int n) {
        n = Math.min(n, columns - index);
        if (n <= 0) return;
        int p = index + n;
        final T[][] rtn = createGrid(rows, columns - n);
        for (int r = 0; r < rows; r++) {
            if (index >= 0) System.arraycopy(grid[r], 0, rtn[r], 0, index);
            if (p < columns) System.arraycopy(grid[r], p, rtn[r], index, columns - p);
        }
        columns -= n;
        grid = rtn;
    }

    @Override
    public String toString() {
        final StringJoiner rtn = new StringJoiner(System.lineSeparator());
        for (int r = 0; r < rows; r++) {
            final StringJoiner line = new StringJoiner(" ");
            for (int c = 0; c < columns; c++) {
                if (grid[r][c] == null) line.add("-");
                else line.add(String.valueOf(grid[r][c].getInitial()));
            }
            rtn.add(line.toString());
        }
        return rtn.toString();
    }
}
