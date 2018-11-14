package org.reactome.server.tools.reaction.exporter.layout.algorithm.gridbreathe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InfiniteCanvas {
    private final Map<Integer, Map<Integer, Tile>> tiles = new HashMap<>();

    public void set(int row, int col, Tile tile) {
        tiles.computeIfAbsent(row, k -> new HashMap<>()).put(col, tile);
    }

    public Tile get(int row, int col) {
        return tiles.getOrDefault(row, Collections.emptyMap()).get(col);
    }

}
