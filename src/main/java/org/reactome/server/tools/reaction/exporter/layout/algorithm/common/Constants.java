package org.reactome.server.tools.reaction.exporter.layout.algorithm.common;

/**
 * Constant values shared by more than one class.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Constants {
    public static final double COMPARTMENT_PADDING = 20;
    public static final int VERTICAL_PADDING = 40;
    public static final int HORIZONTAL_PADDING = 75;
    /**
     * Length of the backbone of the reaction
     */
    public static final double BACKBONE_LENGTH = 20;
    /**
     * Size of the box surrounding regulator and catalysts shapes
     */
    public static final int REGULATOR_SIZE = 6;
    /**
     * Minimum length of segments departing participants.
     */
    public static final int MIN_SEGMENT = 50;
    public static final double ARROW_SIZE = 8;
    public static final double GENE_SEGMENT_LENGTH = 20;
}
