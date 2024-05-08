package rdfpgmapper.mapper.pgt.pgtcomplete.schemamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Repräsentiert eine RDF-Klasse im RDF-Graphenmodell.
 * Diese Klasse speichert die URI der RDF-Klasse sowie Listen ihrer Unter- und Oberklassen.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public class RDFClass {
    private String uri;
    private List<String> subclasses = new ArrayList<>();
    private List<String> superclasses = new ArrayList<>();

    /**
     * Konstruktor, der die URI der RDF-Klasse initialisiert.
     *
     * @param uri Die URI der Klasse.
     */
    public RDFClass(String uri) {
        this.uri = uri;
    }

    /**
     * Fügt eine Unterklasse zur Liste der Unterklasse hinzu.
     *
     * @param subclass Die URI der Unterklasse, die hinzugefügt wird.
     */
    public void addSubclass(String subclass) {
        subclasses.add(subclass);
    }

    /**
     * Fügt eine Oberklasse zur Liste der Oberklasse hinzu.
     *
     * @param superclass Die URI der Oberklasse, die hinzugefügt wird.
     */
    public void addSuperclass(String superclass) {
        superclasses.add(superclass);
    }

    /**
     * Gibt die URI der Klasse zurück.
     *
     * @return Die URI der Klasse.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Gibt eine Liste der URIs der Unterklasse zurück.
     *
     * @return Eine Liste der Unterklasse.
     */
    public List<String> getSubclasses() {
        return subclasses;
    }

    /**
     * Gibt eine Liste der URIs der Oberklasse zurück.
     *
     * @return Eine Liste der Oberklasse.
     */
    public List<String> getSuperclasses() {
        return superclasses;
    }
}
