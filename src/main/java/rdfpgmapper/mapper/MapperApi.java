package rdfpgmapper.mapper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import rdfpgmapper.mapper.rpt.PgtComplete;
import rdfpgmapper.mapper.rpt.PgtSimple;
import rdfpgmapper.mapper.rpt.RptGeneric;
import rdfpgmapper.mapper.rpt.RptSimple;
import rdfpgmapper.neo4j.Neo4jClient;
import rdfpgmapper.rdf.JenaClient;

import java.util.List;

public class MapperApi {

    private final Neo4jClient neo4jClient;
    private final JenaClient jenaClient;
    private final Mapper mapper;

    public MapperApi(int mapper) {
        neo4jClient = new Neo4jClient("bolt://localhost:7687", "neo4j", "12345678");
        jenaClient = new JenaClient();

        switch (mapper){
            case 1:
                this.mapper = new RptSimple(this.neo4jClient);
                break;
            case 2:
                this.mapper = new RptGeneric(this.neo4jClient);
                break;
            case 3:
                this.mapper = new PgtSimple(this.neo4jClient);
                break;
            case 4:
                this.mapper = new PgtComplete(this.neo4jClient);
                break;
            default:
                this.mapper = new RptSimple(this.neo4jClient);
        }
    }

    public void importRdf(String filePath, String format) {

        Model model = jenaClient.parseRDFFile(filePath, format);

        List<String> cypherCommandSchema = mapper.mapRdfToPgSchema(model);
        List<String> cypherCommandInstance = mapper.mapRdfToPgInstance(model);

        neo4jClient.writeToNeo4j(cypherCommandSchema);
        neo4jClient.writeToNeo4j(cypherCommandInstance);

    }


    public void exportRdf(String filePath, String format) {
        Model model = mapper.mapPgToRdf();

        RDFFormat outputFormat = switch (format) {
            case "TTL" -> RDFFormat.TTL;
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
