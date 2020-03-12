package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class Role {

    private EntityRole type;
    private Integer n;

    public Integer getStoichiometry() {
        return n;
    }

    public EntityRole getType() {
        return type;
    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setType(String type) {
        this.type = EntityRole.get(type);
    }

    @Override
    public String toString() {
        return "(" + n + "x" + type + ")";
    }

    public void setStoichiometry(Integer n) {
      this.n = n;
    }
}
