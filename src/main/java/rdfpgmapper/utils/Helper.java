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

public class Helper {

    public static String getPrefixedName(String uri, Model model) {
        for (Map.Entry<String, String> entry : model.getNsPrefixMap().entrySet()) {
            if (uri.startsWith(entry.getValue())) {
                return uri.replace(entry.getValue(), entry.getKey() + "_");
            }
        }
        return uri;
    }

    public static String getUri(String name, Map<String,Object> nsPrefixUri) {
        for (Map.Entry<String, Object> entry : nsPrefixUri.entrySet()) {
            if (name.startsWith(entry.getKey() + "_")) {
                return name.replace(entry.getKey() + "_", entry.getValue().toString());
            }
        }
        return name;
    }

    public static List<String> addNodeForNsPrefixUriDeclaration(Model model, List<String> cypher) {
        if (!model.getNsPrefixMap().isEmpty()) {
            StringBuilder prefixUri = new StringBuilder("MERGE (n:PrefixUriNode) " +
                    "SET ");

            for (Map.Entry<String, String> entry : model.getNsPrefixMap().entrySet()) {
                prefixUri.append("n.").append(entry.getKey()).append("='").append(entry.getValue()).append("',");
            }

            prefixUri.deleteCharAt(prefixUri.length() - 1);
            cypher.add(prefixUri.toString());
        }

        return cypher;
    }

    public static List<String> getClassHierarchy(Resource type, Model model) {
        List<String> hierarchy = new ArrayList<>();
        String typeUri = type.getURI();
        String queryStr = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT ?superclass WHERE { " +
                "<" + typeUri + "> rdfs:subClassOf* ?superclass . }";

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
