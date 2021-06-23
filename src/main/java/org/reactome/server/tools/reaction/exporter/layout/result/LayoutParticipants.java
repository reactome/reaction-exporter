package org.reactome.server.tools.reaction.exporter.layout.result;

import org.neo4j.driver.Value;

public class LayoutParticipants {

    private Role role;
    private String physicalEntity;
    private boolean drug;
    private boolean dashed;
    private boolean crossed;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPhysicalEntity() {
        return physicalEntity;
    }

    public void setPhysicalEntity(String physicalEntity) {
        this.physicalEntity = physicalEntity;
    }

    public boolean isDrug() {
        return drug;
    }

    public void setDrug(boolean drug) {
        this.drug = drug;
    }

    public boolean isDashed() {
        return dashed;
    }

    public void setDashed(boolean dashed) {
        this.dashed = dashed;
    }

    public boolean isCrossed() {
        return crossed;
    }

    public void setCrossed(boolean crossed) {
        this.crossed = crossed;
    }

    public static LayoutParticipants build(Value v) {
        LayoutParticipants lp = new LayoutParticipants();
        lp.setCrossed(v.get("crossed").asBoolean(false));
        lp.setDashed(v.get("dashed").asBoolean(false));
        lp.setDrug(v.get("drug").asBoolean(false));
        lp.setPhysicalEntity(v.get("physicalEntity").asString(null));
        lp.setRole(Role.build(v.get("role")));
        return lp;
    }
}
