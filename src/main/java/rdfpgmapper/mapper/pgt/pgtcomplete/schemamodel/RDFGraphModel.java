package rdfpgmapper.mapper.pgt.pgtcomplete.schemamodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Repräsentiert das Graphenmodell eines RDF-Schemas, das RDF-Klassen und -Properties speichert.
 * Diese Klasse ermöglicht die Verwaltung und den Zugriff auf die Komponenten eines RDF-Graphen.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public class RDFGraphModel {
    Map<String, RDFClass> classes = new HashMap<>();
    Map<String, RDFProperty> properties = new HashMap<>();

    /**
     * Fügt eine RDF-Klasse zum Modell hinzu.
     *
     * @param rdfClass Die RDF-Klasse, die dem Modell hinzugefügt werden soll.
     */
    public void addClass(RDFClass rdfClass) {
        classes.put(rdfClass.getUri(), rdfClass);
    }

    /**
     * Fügt eine RDF-Property zum Modell hinzu.
     *
     * @param rdfProperty Die RDF-Property, die dem Modell hinzugefügt werden soll.
     */
    public void addProperty(RDFProperty rdfProperty) {
        properties.put(rdfProperty.getUri(), rdfProperty);
    }

    /**
     * Gibt eine RDF-Klasse basierend auf ihrer URI zurück.
     *
     * @param uri Die URI der Klasse.
     * @return Die entsprechende RDF-Klasse, falls vorhanden, sonst null.
     */
    public RDFClass getClass(String uri) {
        return classes.get(uri);
    }

    /**
     * Gibt eine RDF-Property basierend auf ihrer URI zurück.
     *
     * @param uri Die URI der Property.
     * @return Die entsprechende RDF-Property, falls vorhanden, sonst null.
     */
    public RDFProperty getProperty(String uri) {
        return properties.get(uri);
    }

    /**
     * Gibt eine Sammlung aller RDF-Properties im Modell zurück.
     *
     * @return Eine Sammlung aller im Modell gespeicherten RDF-Properties.
     */
    public Collection<RDFProperty> getProperties() {
        return properties.values();
    }

    /**
     * Gibt eine Sammlung aller RDF-Klassen im Modell zurück.
     *
     * @return Eine Sammlung aller im Modell gespeicherten RDF-Klassen.
     */
    public Collection<RDFClass> getClasses() {
        return classes.values();
    }
}