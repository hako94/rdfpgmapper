package rdfpgmapper.utils;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hilfsklasse mit statischen Methoden zur Unterstützung der Funktionalität.
 * Bietet Methoden zum Manipulieren von RDF-URIs und Abfrage von RDF-Modellen.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public class Helper {

    /**
     * Konvertiert eine vollständige URI in eine Präfix-basierte Darstellung unter Verwendung der im Modell definierten Namensräume.
     *
     * @param uri   Die vollständige URI, die umgewandelt werden soll.
     * @param model Das RDF-Modell, das die Namensraum-Präfixe enthält.
     * @return Die präfixierte Darstellung der URI oder die ursprüngliche URI, wenn kein entsprechender Namensraum gefunden wird.
     */
    public static String getPrefixedName(String uri, Model model) {
        for (Map.Entry<String, String> entry : model.getNsPrefixMap().entrySet()) {
            if (uri.startsWith(entry.getValue())) {
                return uri.replace(entry.getValue(), entry.getKey() + "_");
            }
        }
        return uri;
    }

    /**
     * Konvertiert einen präfixierten Namen zurück in seine vollständige URI unter Verwendung der angegebenen Namensraum-Präfix-Konfiguration.
     *
     * @param name        Der präfixierte Name.
     * @param nsPrefixUri Eine Map von Namensraum-Präfixen zu vollständigen URIs.
     * @return Die vollständige URI oder der präfixierte Name, wenn kein entsprechender Namensraum gefunden wird.
     */
    public static String getUri(String name, Map<String, Object> nsPrefixUri) {
        for (Map.Entry<String, Object> entry : nsPrefixUri.entrySet()) {
            if (name.startsWith(entry.getKey() + "_")) {
                return name.replace(entry.getKey() + "_", entry.getValue().toString());
            }
        }
        return name;
    }

    /**
     * Fügt Cypher-Befehle zum Erstellen oder Aktualisieren eines Knotens hinzu, der Namensraum-Präfix-URI-Declarations repräsentiert.
     *
     * @param model  Das RDF-Modell mit Namensraum-Präfixen.
     * @param cypher Die Liste von Cypher-Befehlen, zu der der neue Befehl hinzugefügt wird.
     * @return Die aktualisierte Liste von Cypher-Befehlen.
     */
    public static List<String> addNodeForNsPrefixUriDeclaration(Model model, List<String> cypher) {
        if (!model.getNsPrefixMap().isEmpty()) {
            StringBuilder prefixUri = new StringBuilder("MERGE (n:PrefixUriNode) " + "SET ");

            for (Map.Entry<String, String> entry : model.getNsPrefixMap().entrySet()) {
                prefixUri.append("n.").append(entry.getKey()).append("='").append(entry.getValue()).append("',");
            }

            prefixUri.deleteCharAt(prefixUri.length() - 1);
            cypher.add(prefixUri.toString());
        }

        return cypher;
    }

    /**
     * Ermittelt die Klassenhierarchie eines RDF-Typs in einem gegebenen Modell.
     *
     * @param type  Der RDF-Typ, dessen Hierarchie abgefragt wird.
     * @param model Das Modell, das die Daten enthält.
     * @return Eine Liste der URIs der Oberklassen des Typs, einschließlich des Typs selbst.
     */
    public static List<String> getClassHierarchy(Resource type, Model model) {
        List<String> hierarchy = new ArrayList<>();
        String typeUri = type.getURI();
        String queryStr = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "SELECT ?superclass WHERE { " + "<" + typeUri + "> rdfs:subClassOf* ?superclass . }";

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Resource superclass = soln.getResource("superclass");
                hierarchy.add(superclass.getURI());
            }
        }
        return hierarchy;
    }

}
