package org.reactome.server.tools.reaction.exporter.layout.result;

import org.neo4j.driver.Value;

public class Role {
    private String type;
    private Integer stoichiometry;

    public Role() {
    }

    public Role(String type, Integer stoichiometry) {
        this.type = type;
        this.stoichiometry = stoichiometry;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getStoichiometry() {
        return stoichiometry;
    }

    public void setStoichiometry(Integer stoichiometry) {
        this.stoichiometry = stoichiometry;
    }

    public static Role build(Value v) {
        return new Role(v.get("type").asString(null), v.get("n").asInt(1));
    }
}
