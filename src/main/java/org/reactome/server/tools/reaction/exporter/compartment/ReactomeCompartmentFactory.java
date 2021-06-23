package org.reactome.server.tools.reaction.exporter.compartment;

import org.reactome.server.graph.domain.model.GO_CellularComponent;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.reaction.exporter.ontology.GoTerm;
import org.reactome.server.tools.reaction.exporter.ontology.RelationshipType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ReactomeCompartmentFactory {

    private static SchemaService schemaService;

    /**
     * Creates a tree from the Reactome GO Cellular Component hierarchy and connects them.
     *
     * @return a Reactome GO Cellular Component master containing <em>ids</em>
     */
    public static Map<String, GoTerm> getMasterTree() {
        if (schemaService == null) schemaService = ReactomeGraphCore.getService(SchemaService.class);

        Collection<GO_CellularComponent> compartments = schemaService.getByClass(GO_CellularComponent.class);

        Map<String, GoTerm> rtn = new HashMap<>();
        for (GO_CellularComponent component : compartments) {
            rtn.put(getGoIdentifier(component), new GoTerm(component));
        }

        for (GO_CellularComponent component : compartments) {
            GoTerm goTerm = rtn.get(getGoIdentifier(component));

            for (GO_CellularComponent parent : component.getInstanceOf()) {
                goTerm.addParent(rtn.get(getGoIdentifier(parent)));
            }

            for (GO_CellularComponent sb : component.getSurroundedBy()) {
                GoTerm term = rtn.get(getGoIdentifier(sb));
                goTerm.createRelationship(GoTerm.Directionality.OUTGOING, RelationshipType.surrounded_by, term);
            }
        }

        return rtn;
    }

    public static void setSchemaService(SchemaService schemaService) {
        ReactomeCompartmentFactory.schemaService = schemaService;
    }

    private static String getGoIdentifier(GO_CellularComponent component) {
        return component.getDatabaseName() + ":" + component.getAccession();
    }
}
