package rdfpgmapper.mapper;

import org.apache.jena.rdf.model.Model;

import java.util.List;

/**
 * Interface zur Definition der Methoden für Mapping zwischen RDF-Modellen und Neo4j Property-Graphen.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public interface Mapper {

    /**
     * Konvertiert ein RDF-Modell in eine Liste von Cypher-Schema-Anweisungen.
     * Diese Methode dient dazu, das Schema des Property-Graphen in Neo4j zu definieren, basierend auf dem Struktur des RDF-Modells.
     *
     * @param model Das RDF-Modell, das in ein Property-Graph-Schema gemappt werden soll.
     * @return Eine Liste von Cypher-Anweisungen, die das Schema im Neo4j darstellen.
     */
    public List<String> mapRdfToPgSchema(Model model);

    /**
     * Konvertiert ein RDF-Modell in eine Liste von Cypher-Instanz-Anweisungen.
     * Diese Methode erzeugt die Daten, die in den Property-Graphen von Neo4j als Instanzen des zuvor erstellten Schemas eingefügt werden.
     *
     * @param model Das RDF-Modell, das in Neo4j-Instanzdaten gemappt werden soll.
     * @return Eine Liste von Cypher-Anweisungen, die die Instanzdaten im Neo4j darstellen.
     */
    public List<String> mapRdfToPgInstance(Model model);

    /**
     * Konvertiert die Daten aus einem Neo4j Property-Graphen zurück in ein RDF-Modell.
     * Diese Methode ermöglicht die Rückkonversion von Graphdaten in das RDF-Format.
     *
     * @return Ein RDF-Modell, das die Daten aus dem Neo4j Property-Graphen repräsentiert.
     */
    public Model mapPgToRdf();
}
