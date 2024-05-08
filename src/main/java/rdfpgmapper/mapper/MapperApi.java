package rdfpgmapper.mapper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import rdfpgmapper.mapper.pgt.pgtcomplete.PgtComplete;
import rdfpgmapper.mapper.pgt.PgtSimple;
import rdfpgmapper.mapper.rpt.RptGeneric;
import rdfpgmapper.mapper.rpt.RptSimple;
import rdfpgmapper.neo4j.Neo4jClient;
import rdfpgmapper.rdf.JenaClient;

import java.util.List;

/**
 * Haupt-API-Klasse für das RDF-zu-Property-Graph-Mapping.
 * Diese Klasse fungiert als Fassade zur Interaktion mit verschiedenen Mapper-Typen und
 * steuert Import- und Exportfunktionen zwischen RDF-Modellen und Neo4j.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public class MapperApi {

    private final Neo4jClient neo4jClient;
    private final JenaClient jenaClient;
    private final Mapper mapper;

    /**
     * Konstruktor für die MapperApi.
     * Initialisiert die benötigten Clients und wählt den entsprechenden Mapper-Typ basierend auf der Eingabe.
     *
     * @param mapper Nummer des gewählten Mappers (1-4), die bestimmt, welche Mapper-Klasse verwendet wird.
     */
    public MapperApi(int mapper) {
        neo4jClient = new Neo4jClient("bolt://localhost:7687", "neo4j", "12345678");
        jenaClient = new JenaClient();

        switch (mapper) {
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

    /**
     * Importiert RDF-Daten von einem gegebenen Pfad und konvertiert sie in Cypher-Befehle, die in Neo4j ausgeführt werden.
     *
     * @param filePath Pfad zur RDF-Datei.
     * @param format   Format der RDF-Datei (z.B. "RDF/XML").
     */
    public void importRdf(String filePath, String format) {

        Model model = jenaClient.parseRDFFile(filePath, format);

        List<String> cypherCommandSchema = mapper.mapRdfToPgSchema(model);
        List<String> cypherCommandInstance = mapper.mapRdfToPgInstance(model);

        neo4jClient.writeToNeo4j(cypherCommandSchema);
        neo4jClient.writeToNeo4j(cypherCommandInstance);

    }

    /**
     * Exportiert Daten aus Neo4j in ein RDF-Format und speichert sie an einem angegebenen Pfad.
     *
     * @param filePath Pfad, an dem die RDF-Datei gespeichert werden soll.
     * @param format   Das RDF-Format, in das exportiert werden soll (z.B. "TTL", "RDF/XML").
     */
    public void exportRdf(String filePath, String format) {
        Model model = mapper.mapPgToRdf();

        RDFFormat outputFormat = switch (format) {
            case "TTL" -> RDFFormat.TURTLE;
            case "RDF/XML" -> RDFFormat.RDFXML;
            case "N-TRIPLE" -> RDFFormat.NTRIPLES;
            default -> RDFFormat.TURTLE_PRETTY;
        };

        jenaClient.writeModel(model, filePath, outputFormat);
    }

    /**
     * Löscht alle Daten in der Neo4j-Datenbank.
     */
    public void clearDatabase() {
        neo4jClient.writeToNeo4j(List.of("CALL apoc.schema.assert({}, {})", "CALL apoc.trigger.removeAll()", "MATCH (n) DETACH DELETE n"));
    }
}
