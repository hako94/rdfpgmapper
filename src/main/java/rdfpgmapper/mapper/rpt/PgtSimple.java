package rdfpgmapper.mapper.rpt;

import org.apache.jena.rdf.model.Model;
import rdfpgmapper.mapper.Mapper;
import rdfpgmapper.neo4j.Neo4jClient;

import java.util.List;

public class PgtSimple implements Mapper {

    private final Neo4jClient neo4jClient;

    public PgtSimple(Neo4jClient client) {
        this.neo4jClient = client;
    }

    @Override
    public List<String> mapRdfToPgSchema(Model model) {
        return null;
    }

    @Override
    public List<String> mapRdfToPgInstance(Model model) {
        return null;
    }

    @Override
    public Model mapPgToRdf() {
        return null;
    }
}
