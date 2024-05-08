package rdfpgmapper.mapper.pgt.pgtcomplete.schemamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Diese Klasse repräsentiert eine RDF-Property innerhalb eines RDF-Graphenmodells.
 * Sie speichert Informationen wie URI, Literalzugehörigkeit, Domain, Rangebereiche und Beziehungen zu anderen Eigenschaften.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public class RDFProperty {
    private String uri;
    private Boolean literal = false;
    private List<String> domains = new ArrayList<>();
    private List<String> ranges = new ArrayList<>();
    private List<String> subproperties = new ArrayList<>();
    private List<String> superproperties = new ArrayList<>();

    /**
     * Konstruktor, der die URI der RDF-Eigenschaft initialisiert.
     *
     * @param uri Die URI der Eigenschaft.
     */
    public RDFProperty(String uri) {
        this.uri = uri;
    }

    /**
     * Setzt die Property als Literal.
     */
    public void setIsLiteral() {
        literal = true;
    }

    /**
     * Gibt zurück, ob die Property als Literal definiert ist.
     *
     * @return true, wenn die Property ein Literal ist, sonst false.
     */
    public Boolean isLiteral() {
        return literal;
    }

    /**
     * Fügt eine Domain zur Property hinzu.
     *
     * @param domain Die Domain, die hinzugefügt wird.
     */
    public void addDomain(String domain) {
        domains.add(domain);
    }

    /**
     * Fügt eine Range zur Property hinzu.
     *
     * @param range Der Range Bereich, der hinzugefügt wird.
     */
    public void addRange(String range) {
        ranges.add(range);
    }

    /**
     * Fügt eine Super-Property hinzu.
     *
     * @param subproperty Die Super-Property, die hinzugefügt wird.
     */
    public void addSubproperty(String subproperty) {
        subproperties.add(subproperty);
    }

    /**
     * Fügt eine Super-Property hinzu.
     *
     * @param superproperty Die Super-Property, die hinzugefügt wird.
     */
    public void addSuperproperty(String superproperty) {
        superproperties.add(superproperty);
    }

    /**
     * Gibt die URI der Property zurück.
     *
     * @return Die URI der Property.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Gibt die Liste der Domains zurück.
     *
     * @return Eine Liste der Domains der Property.
     */
    public List<String> getDomains() {
        return domains;
    }

    /**
     * Gibt die Liste der Ranges zurück.
     *
     * @return Eine Liste der Ranges der Property.
     */
    public List<String> getRanges() {
        return ranges;
    }

    /**
     * Gibt die Liste der Unter-Properties zurück.
     *
     * @return Eine Liste der Unter-Properties.
     */
    public List<String> getSubproperties() {
        return subproperties;
    }

    /**
     * Gibt die Liste der Super-Properties zurück.
     *
     * @return Eine Liste der Super-Properties.
     */
    public List<String> getSuperproperties() {
        return superproperties;
    }

}