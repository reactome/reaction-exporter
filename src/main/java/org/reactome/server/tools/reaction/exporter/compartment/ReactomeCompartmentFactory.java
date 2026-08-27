package org.reactome.server.tools.reaction.exporter.compartment;

import org.reactome.server.graph.domain.model.ExternalOntology;
import org.reactome.server.graph.domain.model.GO_CellularComponent;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.reaction.exporter.ontology.GoTerm;
import org.reactome.server.tools.reaction.exporter.ontology.RelationshipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReactomeCompartmentFactory {

    private static SchemaService schemaService;
    private static AdvancedDatabaseObjectService ads;

    /**
     * Creates a tree from the Reactome GO Cellular Component hierarchy and connects them.
     *
     * @return a Reactome GO Cellular Component master containing <em>ids</em>
     */
    public static Map<String, GoTerm> getMasterTree() {
        if (schemaService == null) schemaService = ReactomeGraphCore.getService(SchemaService.class);

        Collection<GO_CellularComponent> compartments = schemaService.getByClass(GO_CellularComponent.class);
        compartments = compartments.stream().map(c -> (GO_CellularComponent) ads.findById(c.getDbId(), 1)).collect(Collectors.toList());

        Map<String, GoTerm> rtn = new HashMap<>();
        for (GO_CellularComponent component : compartments) {
            rtn.put(getGoIdentifier(component), new GoTerm(component));
        }

        for (GO_CellularComponent component : compartments) {
            GoTerm goTerm = rtn.get(getGoIdentifier(component));

            if (component.getInstanceOf() != null) {
                for (ExternalOntology parent : component.getInstanceOf()) {
                    goTerm.addParent(rtn.get(getGoIdentifier(parent)));
                }
            }
            
            // Not sure why instanceOf is used. But it should be componentOf really.
            if (component.getComponentOf() != null) {
                for (GO_CellularComponent parent : component.getComponentOf()) {
                    GoTerm parentTerm = rtn.get(getGoIdentifier(parent));
                    if (parentTerm == null)
                        continue;
                    goTerm.addParent(parentTerm);
                }
            }

            if (component.getSurroundedBy() != null) {
                for (GO_CellularComponent sb : component.getSurroundedBy()) {
                    GoTerm term = rtn.get(getGoIdentifier(sb));
                    goTerm.createRelationship(GoTerm.Directionality.OUTGOING, RelationshipType.surrounded_by, term);
                }
            }
        }

        return rtn;
    }

    @Autowired
    public static void setSchemaService(SchemaService schemaService) {
        ReactomeCompartmentFactory.schemaService = schemaService;
    }
    
    public static void setAdvancedDatabaseObjectService(AdvancedDatabaseObjectService service) {
        ReactomeCompartmentFactory.ads = service;
    }

    private static String getGoIdentifier(ExternalOntology component) {
        // component (and its instanceOf targets) are always GO_CellularComponent, sourced from the
        // "GO" reference database. Hardcoded rather than derived from component.getDatabaseName(),
        // since instanceOf targets are shallow-loaded (one hop from a different root node) and never
        // have their own referenceDatabase relationship populated, which getDatabaseName() needs.
        return "GO:" + component.getIdentifier();
    }
}
