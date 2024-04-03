package rdfpgmapper.mapper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFFormat;
import rdfpgmapper.mapper.rpt.RptMapperImpl;
import rdfpgmapper.neo4j.Neo4jClient;
import rdfpgmapper.rdf.JenaClient;

import java.util.List;

public class MapperApi {

    private final Neo4jClient neo4jClient;
    private final JenaClient jenaClient;

    public MapperApi() {
        neo4jClient = new Neo4jClient("bolt://localhost:7687", "neo4j", "12345678");
        jenaClient = new JenaClient();
    }

    public void importRdf(String filePath, String format) {

        Model model = jenaClient.parseRDFFile(filePath, format);

        Mapper mapper = new RptMapperImpl();
        List<String> cypherCommandSchema = mapper.mapRdfToPgSchema(model);
        List<String> cypherCommandInstance = mapper.mapRdfToPgInstance(model);

        neo4jClient.writeToNeo4j(cypherCommandSchema);
        neo4jClient.writeToNeo4j(cypherCommandInstance);

    }


    public void exportRdf(String filePath, String format) {
        Model model = ModelFactory.createDefaultModel();

        RDFFormat outputFormat = switch (format) {
            case "TTL" -> RDFFormat.TURTLE;
            case "RDF/XML" -> RDFFormat.RDFXML;
            case "N-TRIPLE" -> RDFFormat.NTRIPLES;
            default -> RDFFormat.TURTLE_PRETTY;
        };

        jenaClient.writeModel(model, filePath, outputFormat);


    }

    public void clearDatabase() {
        neo4jClient.writeToNeo4j(List.of("CALL apoc.schema.assert({}, {})", "MATCH (n) DETACH DELETE n"));
    }

    public void close() {
        neo4jClient.close();
    }
}
