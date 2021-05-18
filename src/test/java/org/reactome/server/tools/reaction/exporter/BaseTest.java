package org.reactome.server.tools.reaction.exporter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactome.server.graph.aop.LazyFetchAspect;
import org.reactome.server.graph.config.Neo4jConfig;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.tools.reaction.exporter.compartment.ReactomeCompartmentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ContextConfiguration(classes = { Neo4jConfig.class })
@ExtendWith(SpringExtension.class)
public abstract class BaseTest {

    static final Logger logger = LoggerFactory.getLogger("testLogger");

    private static Boolean checkedOnce = false;
    private static Boolean isFit = false;

    @Autowired
    protected GeneralService generalService;

    @Autowired
    protected SchemaService schemaService;

    @Autowired
    protected LazyFetchAspect lazyFetchAspect;

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        if (!checkedOnce) {
            isFit = generalService.fitForService();
            checkedOnce = true;
        }
        assumeTrue(isFit);
        ReactomeCompartmentFactory.setSchemaService(schemaService);
    }
}
