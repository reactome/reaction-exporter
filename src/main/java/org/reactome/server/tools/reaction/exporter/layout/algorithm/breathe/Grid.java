package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * Generic matrix, indexed by row and col. All rows have the same number of columns.
 *
 * @param <T> type of elements in grid
 */
public class Grid<T> {

    private int rows;
    private int columns;
    private T[][] grid;

    public Grid() {
        this(0,0);
    }

    public Grid(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.grid = (T[][]) new Object[rows][columns];
    }

    public Grid(T[][] grid) {
        this.grid = grid;
        rows = grid.length;
        columns = grid[0].length;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    T get(int row, int column) {
        return grid[row][column];
    }

    void set(int row, int column, T element) {
        if (rows <= row)
            insertRows(rows, row - rows + 1);
        if (columns <= column)
            insertColumns(columns, column - columns + 1);
        grid[row][column] = element;
    }

    List<T> getRow(int row) {
        return row < rows ? Arrays.asList(grid[row]) : null;
    }

    List<T> getColumn(int col) {
        if (col < columns) {
            final List<T> rtn = new ArrayList<>(rows);
            for (final T[] row : grid) {
                rtn.add(row[col]);
            }
            return rtn;
        }
        return null;
    }

    void insertRows(int index, int n) {
        if (n <= 0) return;
        final T[][] rtn = (T[][]) new Object[rows + n][columns];
        if (index >= 0) System.arraycopy(grid, 0, rtn, 0, index);
        if (rows - index >= 0) System.arraycopy(grid, index, rtn, index + n, rows - index);
        rows += n;
        grid = rtn;
    }

    void insertColumns(int index, int n) {
        if (n <= 0) return;
        final T[][] rtn = (T[][]) new Object[rows][columns + n];
        for (int r = 0; r < rows; r++) {
            if (index >= 0) System.arraycopy(grid[r], 0, rtn[r], 0, index);
            if (columns - index >= 0)
                System.arraycopy(grid[r], index, rtn[r], index + n, columns - index);
        }
        columns += n;
        grid = rtn;
    }

    void removeRows(int index, int n) {
        n = Math.min(n, rows - index);
        if (n <= 0) return;
        int p = index + n;
        final T[][] rtn = (T[][]) new Object[rows - n][columns];
        if (index >= 0) System.arraycopy(grid, 0, rtn, 0, index);
        if (p < rows) System.arraycopy(grid, p, rtn, index, rows - p);
        rows -= n;
        grid = rtn;
    }

    void removeColumns(int index, int n) {
        n = Math.min(n, columns - index);
        if (n <= 0) return;
        int p = index + n;
        final T[][] rtn = (T[][]) new Object[rows][columns - n];
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
                else line.add(String.valueOf(grid[r][c].toString().charAt(0)));
            }
            rtn.add(line.toString());
        }
        return rtn.toString();
    }
}
