package org.reactome.server.tools.reaction.exporter.layout.common;

/**
 * Each participant will play only one or more of these roles
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public enum EntityRole {
    INPUT("input"),
    OUTPUT("output"),
    CATALYST("catalyst"),
    NEGATIVE_REGULATOR("negative"),
    POSITIVE_REGULATOR("positive");

    private String role;

    EntityRole(String role) {
        this.role = role;
    }

    public static EntityRole get(String role){
        for (EntityRole value : values()) {
            if(value.role.equals(role.toLowerCase())) return value;
        }
        return null;
    }
}
