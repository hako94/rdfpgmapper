package rdfpgmapper.mapper;

import org.apache.jena.rdf.model.Model;

import java.util.List;

public interface Mapper {

    public List<String> mapRdfToPgSchema(Model model);
    public List<String> mapRdfToPgInstance(Model model);

    public Model mapPgToRdf();
}
