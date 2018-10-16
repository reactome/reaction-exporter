package org.reactome.server.tools.reaction.exporter.layout.common;

/**
 * Each participant will play only one or more of these roles
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public enum EntityRole {
    INPUT,
    OUTPUT,
    CATALYST,
    NEGATIVE_REGULATOR,
    POSITIVE_REGULATOR
}
