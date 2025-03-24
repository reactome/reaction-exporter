package org.reactome.server.tools.reaction.exporter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.reactome.server.graph.aop.LazyFetchAspect;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.reaction.exporter.compartment.ReactomeCompartmentFactory;
import org.reactome.server.tools.reaction.exporter.config.ReactomeNeo4jConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@ContextConfiguration(classes = ReactomeNeo4jConfig.class)
public abstract class BaseTest {

    static final Logger logger = LoggerFactory.getLogger("testLogger");

    private static Boolean checkedOnce = false;
    private static Boolean isFit = false;

    @Autowired
    protected GeneralService generalService;

    @Autowired
    protected SchemaService schemaService;


    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeAll
    public static void beforeAll(
            @Value("${spring.neo4j.uri}") String uri,
            @Value("${spring.neo4j.authentication.username}") String user,
            @Value("${spring.neo4j.authentication.password}") String pwd,
            @Value("${spring.data.neo4j.database}") String database
    ) {
        ReactomeGraphCore.initialise(uri, user, pwd, database);
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
