package rdfpgmapper.mapper.pgt;

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
import java.util.stream.Collectors;

public class PgtSimple implements Mapper {

    private final Neo4jClient neo4jClient;

    public PgtSimple(Neo4jClient client) {
        this.neo4jClient = client;
    }

    @Override
    public List<String> mapRdfToPgSchema(Model model) {
        List<String> cypher = new ArrayList<>();
        cypher.add("CREATE CONSTRAINT FOR (r:Resource) REQUIRE r.iri IS NOT NULL;");
        cypher.add("CREATE CONSTRAINT FOR (b:BlankNode) REQUIRE b.id IS NOT NULL;");

        cypher.add("CREATE CONSTRAINT FOR (r:Resource) REQUIRE r.iri IS UNIQUE;");
        cypher.add("CREATE CONSTRAINT FOR (b:BlankNode) REQUIRE b.id IS UNIQUE;");

        return cypher;
    }

    @Override
    public List<String> mapRdfToPgInstance(Model model) {
        List<String> cypher = new ArrayList<>();

        Iterator<Statement> statementIterator = model.listStatements();

        while (statementIterator.hasNext()) {
            Statement statement = statementIterator.next();

            Resource subject = statement.getSubject();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();

            if (object.isLiteral()) {
                if (subject.isURIResource()) {
                    cypher.add(mergeRessourceLiteral(subject, predicate, (Literal) object, model));
                } else {
                    cypher.add(mergeBlankNodeLiteral(subject, predicate, (Literal) object, model));
                }
            } else {
                String[] subjectArr;

                String propertyName = Helper.getPrefixedName(predicate.getURI(), model);
                if (propertyName.equals("rdf_type")) {
                    if (subject.isURIResource()) {
                        cypher.add(mergeResourceLabel(subject, (Resource) object, model));
                    } else {
                        cypher.add(mergeBlankNodeLabel(subject, (Resource) object, model));
                    }
                }
                if (subject.isURIResource()) {
                    subjectArr = mergeResource(subject, 'a', model);
                } else {
                    subjectArr = mergeBlankNode(subject, 'a');
                }

                String[] objectArr;

                if (object.isURIResource()) {
                    objectArr = mergeResource((Resource) object, 'b', model);
                } else {
                    objectArr = mergeBlankNode((Resource) object, 'b');
                }

                cypher.add(subjectArr[1] + "\n" +
                        objectArr[1] + "\n" +
                        mergeProperty(predicate, subjectArr[0], objectArr[0], model));

            }
        }

        return Helper.addNodeForNsPrefixUriDeclaration(model, cypher);
    }

    private String mergeResourceLabel(Resource resource, Resource type, Model model) {
        List<String> classHierarchy = Helper.getClassHierarchy(type, model);

        String labelsCypher = classHierarchy.stream()
                .map(uri -> Helper.getPrefixedName(uri, model))
                .map(label -> "res:" + label)
                .collect(Collectors.joining(","));
        if (!labelsCypher.isEmpty()) {
            labelsCypher = "," + labelsCypher;
        }

        return "MERGE (res:Resource {iri: '" + Helper.getPrefixedName(resource.getURI(), model) + "'})" +
                "SET res:" + Helper.getPrefixedName(type.getURI(), model) + labelsCypher;
    }

    private String mergeBlankNodeLabel(Resource resource, Resource type, Model model) {
        List<String> classHierarchy = Helper.getClassHierarchy(type, model);
        String labelsCypher = classHierarchy.stream()
                .map(uri -> Helper.getPrefixedName(uri, model))
                .map(label -> "SET b:" + label)
                .collect(Collectors.joining(" "));

        return "MERGE (b" + ":BlankNode {id: '_:" + resource.getId() + "'})" +
                "SET b:" + Helper.getPrefixedName(type.getURI(), model) + labelsCypher;
    }

    private String[] mergeResource(Resource resource, char postfix, Model model) {
        return new String[]{"res" + postfix, "MERGE (res" + postfix + ":Resource {iri: '" + Helper.getPrefixedName(resource.getURI(), model) + "'})"};
    }

    private String[] mergeBlankNode(Resource resource, char postfix) {
        return new String[]{"b" + postfix, "MERGE (b" + postfix + ":BlankNode {id: '_:" + resource.getId() + "'})"};
    }

    private String mergeRessourceLiteral(Resource resource, Property predicate, Literal literal, Model model) {
        return "MERGE (res:Resource {iri: '" + Helper.getPrefixedName(resource.getURI(), model) + "'})" +
                "SET res." + Helper.getPrefixedName(predicate.getURI(), model) + " = '" + literal.getValue() + "^^" + Helper.getPrefixedName(literal.getDatatypeURI(), model) + "'";
    }

    private String mergeBlankNodeLiteral(Resource resource, Property predicate, Literal literal, Model model) {
        return "MERGE (b:BlankNode {id: '_:" + resource.getId() + "'})" +
                "SET b." + Helper.getPrefixedName(predicate.getURI(), model) + " = '" + literal.getValue() + "^^" + Helper.getPrefixedName(literal.getDatatypeURI(), model) + "'";
    }

    private String mergeProperty(Property property, String subject, String object, Model model) {

        return "MERGE (" + subject + ")-[:" + Helper.getPrefixedName(property.getURI(), model) + "]->(" + object + ")";
    }

    @Override
    public Model mapPgToRdf() {
        Model model = ModelFactory.createDefaultModel();


        List<Record> results = neo4jClient.readFromNeo4j(
                "MATCH (n)-[r]->(m) " +
                        "RETURN n.iri AS subjectIri, n.id AS subjectId, properties(n) as subjectProperties, " +
                        "TYPE(r) AS predicateUri, " +
                        "m.iri AS objectIri, m.id AS objectId, properties(m) as objectProperties"
        );

        List<Record> nsPrefixUriRecord = neo4jClient.readFromNeo4j("MATCH (n:PrefixUriNode) RETURN properties(n) as nsPrefixUri");

        Map<String, Object> nsPrefixUri = nsPrefixUriRecord.getFirst().get("nsPrefixUri").asMap();

        for (Map.Entry<String, Object> prefixUri : nsPrefixUri.entrySet()) {
            model.setNsPrefix(prefixUri.getKey(), prefixUri.getValue().toString());
        }

        for (Record result : results) {
            String subjectIri = result.get("subjectIri").asString();
            String subjectId = result.get("subjectId").asString();
            Map<String, Object> subjectProperties = result.get("subjectProperties").asMap();

            String predicateUri = result.get("predicateUri").asString();

            String objectIri = result.get("objectIri").asString();
            String objectId = result.get("objectId").asString();
            Map<String, Object> objectProperties = result.get("objectProperties").asMap();

            Resource subject;
            Property predicate = model.createProperty(Helper.getUri(predicateUri, nsPrefixUri));
            Resource object;

            if (!subjectId.equals("null") || !subjectIri.equals("null")) {

                subject = addResourceWithLiterals(model, nsPrefixUri, subjectIri, subjectId, subjectProperties);

                object = addResourceWithLiterals(model, nsPrefixUri, objectIri, objectId, objectProperties);

                model.add(subject, predicate, object);
            }
        }
        return model;
    }

    private Resource addResourceWithLiterals(Model model, Map<String, Object> nsPrefixUri, String resourceIri, String resourceId, Map<String, Object> objectProperties) {
        Resource resource;
        if (!resourceId.equals("null")) {
            resource = model.createResource(new AnonId(resourceId));
        } else {
            resource = model.createResource(Helper.getUri(resourceIri, nsPrefixUri));
        }
        for (Map.Entry<String, Object> entry : objectProperties.entrySet()) {
            if (!entry.getKey().equals("iri") && !entry.getKey().equals("id")) {
                String[] parts = entry.getValue().toString().split("\\^\\^");
                String dataType = Helper.getUri(parts[1], nsPrefixUri);
                Literal literal = model.createTypedLiteral(parts[0], dataType);
                Property property = model.createProperty(Helper.getUri(entry.getKey(), nsPrefixUri));
                resource.addLiteral(property, literal);
            }
        }
        return resource;
    }
}
