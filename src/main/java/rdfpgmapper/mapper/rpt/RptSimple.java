package rdfpgmapper.mapper.rpt;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.neo4j.driver.Record;
import rdfpgmapper.mapper.Mapper;
import rdfpgmapper.neo4j.Neo4jClient;
import rdfpgmapper.utils.Helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RptSimple implements Mapper {
    private final Neo4jClient neo4jClient;

    public RptSimple(Neo4jClient client) {
        this.neo4jClient = client;
    }

    @Override
    public List<String> mapRdfToPgSchema(Model model) {
        List<String> cypher = new ArrayList<>();
        cypher.add("CREATE CONSTRAINT FOR (r:Node) REQUIRE r.name IS NOT NULL;");
        cypher.add("CREATE CONSTRAINT FOR (r:Node) REQUIRE r.name IS UNIQUE;");

        return cypher;
    }

    @Override
    public List<String> mapRdfToPgInstance(Model model) {
        List<String> cypher = new ArrayList<>();

        Iterator<Statement> statementIterator = model.listStatements();

        while (statementIterator.hasNext()) {
            Statement statement = statementIterator.next();

            String[] subjectArr;
            Resource subject = statement.getSubject();
            if (subject.isURIResource()) {
                subjectArr = mergeRessource(subject, 'a', model);
            } else {
                subjectArr = mergeBlankNode(subject, 'a');
            }

            String[] objectArr;
            RDFNode object = statement.getObject();
            if (object.isURIResource()) {
                objectArr = mergeRessource((Resource) object, 'b', model);
            } else if (object.isAnon()) {
                objectArr = mergeBlankNode((Resource) object, 'b');
            } else {
                objectArr = mergeLiteral((Literal) object, model);
            }

            Property predicate = statement.getPredicate();

            cypher.add(
                    subjectArr[1] + "\n" +
                            objectArr[1] + "\n" +
                            mergeProperty(predicate, subjectArr[0], objectArr[0], model));
        }


        return Helper.addNodeForNsPrefixUriDeclaration(model, cypher);
    }


    private String[] mergeRessource(Resource resource, char postfix, Model model) {
        String iri = Helper.getPrefixedName(resource.getURI(), model);
        return new String[]{"res" + postfix, "MERGE (res" + postfix + ":Node {name: '" + iri + "'})"};
    }

    private String[] mergeBlankNode(Resource resource, char postfix) {
        String id = resource.getId().toString().replace("'", "_");
        return new String[]{"b" + postfix, "MERGE (b" + postfix + ":Node {name: '_:" + id + "'})"};
    }

    private String[] mergeLiteral(Literal literal, Model model) {
        String value = literal.getValue().toString().replace("'", "_");
        return new String[]{"lit", "MERGE (lit" + ":Node {name: '" + value + "^^" + Helper.getPrefixedName(literal.getDatatypeURI(), model) + "'})"};
    }

    private String mergeProperty(Property property, String subject, String object, Model model) {
        return "MERGE (" + subject + ")-[:Property {name: '" + Helper.getPrefixedName(property.getURI(), model) + "'}]->(" + object + ")";
    }

    @Override
    public Model mapPgToRdf() {
        Model model = ModelFactory.createDefaultModel();

        List<Record> results = neo4jClient.readFromNeo4j(
                "MATCH (n)-[r]->(m) RETURN n.name AS subjectName, r.name AS predicateUri, m.name AS objectName"
        );

        List<Record> nsPrefixUriRecord = neo4jClient.readFromNeo4j("MATCH (n:PrefixUriNode) RETURN properties(n) as nsPrefixUri");

        Map<String, Object> nsPrefixUri = nsPrefixUriRecord.getFirst().get("nsPrefixUri").asMap();

        for (Map.Entry<String, Object> prefixUri : nsPrefixUri.entrySet()) {
            model.setNsPrefix(prefixUri.getKey(), prefixUri.getValue().toString());
        }

        for (Record result : results) {
            String subjectName = result.get("subjectName").asString();
            String predicateUri = result.get("predicateUri").asString();
            String objectName = result.get("objectName").asString();

            Resource subject;
            if (!subjectName.startsWith("_")){
                subject = model.createResource(Helper.getUri(subjectName, nsPrefixUri));
            } else {
                subject = model.createResource(new AnonId(subjectName.replace("_:", "")));
            }

            Property predicate = model.createProperty(Helper.getUri(predicateUri, nsPrefixUri));

            RDFNode object;
            if (objectName.contains("^^")) {
                String[] parts = objectName.split("\\^\\^");
                object = model.createTypedLiteral(parts[0], Helper.getUri(parts[1], nsPrefixUri));
            } else {
                if (!objectName.startsWith("_")){
                    object = model.createResource(Helper.getUri(objectName, nsPrefixUri));
                } else {
                    object = model.createResource(new AnonId(objectName.replace("_:", "")));
                }
            }

            model.add(subject, predicate, object);
        }

        return model;
    }

}
