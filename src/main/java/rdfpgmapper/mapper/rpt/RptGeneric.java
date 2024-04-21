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
import java.util.Objects;

public class RptGeneric implements Mapper {

    private final Neo4jClient neo4jClient;

    public RptGeneric(Neo4jClient client) {
        this.neo4jClient = client;
    }

    @Override
    public List<String> mapRdfToPgSchema(Model model) {
        List<String> cypher = new ArrayList<>();
        cypher.add("CREATE CONSTRAINT FOR (r:Resource) REQUIRE r.iri IS NOT NULL;");
        cypher.add("CREATE CONSTRAINT FOR (b:BlankNode) REQUIRE b.id IS NOT NULL;");
        cypher.add("CREATE CONSTRAINT FOR (l:Literal) REQUIRE l.value IS NOT NULL;");
        cypher.add("CREATE CONSTRAINT FOR (l:Literal) REQUIRE l.type IS NOT NULL;");
        cypher.add("CREATE CONSTRAINT FOR (op:ObjectProperty) REQUIRE op.type IS NOT NULL;");
        cypher.add("CREATE CONSTRAINT FOR (dp:DatatypeProperty) REQUIRE dp.type IS NOT NULL;");

        cypher.add("CREATE CONSTRAINT FOR (r:Resource) REQUIRE r.iri IS UNIQUE;");
        cypher.add("CREATE CONSTRAINT FOR (b:BlankNode) REQUIRE b.id IS UNIQUE;");
        cypher.add("CREATE CONSTRAINT FOR (l:Literal) REQUIRE (l.value, l.type) IS UNIQUE;");
        cypher.add("CREATE CONSTRAINT FOR (op:ObjectProperty) REQUIRE op.type IS UNIQUE;");
        cypher.add("CREATE CONSTRAINT FOR (dp:DatatypeProperty) REQUIRE dp.type IS UNIQUE;");

        cypher.add("CALL apoc.trigger.add('validate_object_domain_range', " +
                "'MATCH (n)-[r:DatatypeProperty]->(m) " +
                "WITH r, startNode(r) AS domainNode, endNode(r) AS rangeNode, r.type AS propType " +
                "WHERE NOT (domainNode:Resource OR domainNode:BlankNode) AND (rangeNode:Resource OR rangeNode:BlankNode) " +
                //"CALL apoc.log.info('ObjectProperty Beziehung von %s zu %s mit Typ %s abgelehnt.', domainNode.iri, rangeNode.iri, propType) " +
                "DELETE r', {phase:'before'});");

        cypher.add("CALL apoc.trigger.add('validate_literal_domain_range', " +
                "'MATCH (n)-[r:ObjectProperty]->(m) " +
                "WITH r, startNode(r) AS domainNode, endNode(r) AS rangeNode, r.type AS propType " +
                "WHERE NOT (domainNode:Resource OR domainNode:BlankNode) AND (rangeNode:Literal) " +
                //"CALL apoc.log.info('DatatypeProperty Beziehung von %s zu %s mit Typ %s abgelehnt.', domainNode.iri, rangeNode.iri, propType) " +
                "DELETE r', {phase:'before'});");

/*        cypher.add("CREATE INDEX FOR (r:Resource) ON r.iri");
        cypher.add("CREATE INDEX FOR (b:BlankNode) ON b.id");
        cypher.add("CREATE INDEX FOR (l:Literal) ON l.value");*/

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
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();

            String propertyName = Helper.getPrefixedName(predicate.getURI(), model);

            if (propertyName.equals("rdf_type")) {
                if (subject.isURIResource()) {
                    subjectArr = mergeResourceType(subject, 'a', (Resource) object, model);
                } else {
                    subjectArr = mergeBlankNodeType(subject, 'a', (Resource) object, model);
                }
            } else {
                if (subject.isURIResource()) {
                    subjectArr = mergeResource(subject, 'a', model);
                } else {
                    subjectArr = mergeBlankNode(subject, 'a');
                }
            }

            String[] objectArr;
            if (object.isURIResource()) {
                objectArr = mergeResource((Resource) object, 'b', model);
            } else if (object.isAnon()) {
                objectArr = mergeBlankNode((Resource) object, 'b');
            } else {
                objectArr = mergeLiteral((Literal) object, model);
            }


            if (object.isLiteral()) {
                cypher.add(subjectArr[1] + "\n" +
                        objectArr[1] + "\n" +
                        mergeDatatypeProperty(predicate, subjectArr[0], objectArr[0], model));
            } else {
                cypher.add(subjectArr[1] + "\n" +
                        objectArr[1] + "\n" +
                        mergeObjectProperty(predicate, subjectArr[0], objectArr[0], model));
            }
        }

        return Helper.addNodeForNsPrefixUriDeclaration(model, cypher);
    }

    private String[] mergeResourceType(Resource resource, char postfix, Resource type, Model model) {
        String iri = Helper.getPrefixedName(resource.getURI(), model);
        String typeString = Helper.getPrefixedName(type.getURI(), model);
        return new String[]{"res" + postfix, "MERGE (res" + postfix + ":Resource {iri: '" + iri + "'}) " +
                "SET res" + postfix + ".type = '" + typeString + "'"};
    }

    private String[] mergeBlankNodeType(Resource resource, char postfix, Resource type, Model model) {
        String id = resource.getId().toString().replace("'", "_");
        String typeString = Helper.getPrefixedName(type.getURI(), model);
        return new String[]{"b" + postfix, "MERGE (b" + postfix + ":BlankNode {id: '_:" + id + "'}) " +
                "SET b" + postfix + ".type = '" + typeString + "'"};
    }

    private String[] mergeResource(Resource resource, char postfix, Model model) {
        String iri = Helper.getPrefixedName(resource.getURI(), model);
        return new String[]{"res" + postfix, "MERGE (res" + postfix + ":Resource {iri: '" + iri + "'})"};
    }

    private String[] mergeBlankNode(Resource resource, char postfix) {
        String id = resource.getId().toString().replace("'", "_");
        return new String[]{"b" + postfix, "MERGE (b" + postfix + ":BlankNode {id: '_:" + id + "'})"};
    }

    private String[] mergeLiteral(Literal literal, Model model) {
        String value = literal.getValue().toString().replace("'", "_");
        return new String[]{"lit", "MERGE (lit" + ":Literal {value: '" + value + "', type: '" + Helper.getPrefixedName(literal.getDatatypeURI(), model) + "'})"};
    }

    private String mergeDatatypeProperty(Property property, String subject, String object, Model model) {
        return "MERGE (" + subject + ")-[:DatatypeProperty {type: '" + Helper.getPrefixedName(property.getURI(), model) + "'}]->(" + object + ")";
    }


    private String mergeObjectProperty(Property property, String subject, String object, Model model) {
        return "MERGE (" + subject + ")-[:ObjectProperty {type: '" + Helper.getPrefixedName(property.getURI(), model) + "'}]->(" + object + ")";

    }

    @Override
    public Model mapPgToRdf() {
        Model model = ModelFactory.createDefaultModel();

        List<Record> results = new ArrayList<>();
        results.addAll(neo4jClient.readFromNeo4j(
                "MATCH (n:Resource)-[r:ObjectProperty]->(m:Resource) RETURN n.iri AS subjectName, r.type AS predicateUri, m.iri AS objectName"));
        results.addAll(neo4jClient.readFromNeo4j(
                "MATCH (n:Resource)-[r:ObjectProperty]->(m:BlankNode) RETURN n.iri AS subjectName, r.type AS predicateUri, m.id AS objectName"));
        results.addAll(neo4jClient.readFromNeo4j(
                "MATCH (n:Resource)-[r:DatatypeProperty]->(m:Literal) RETURN n.iri AS subjectName, r.type AS predicateUri, m.value AS literalValue, m.type AS literalType"));
        results.addAll(neo4jClient.readFromNeo4j(
                "MATCH (n:BlankNode)-[r:ObjectProperty]->(m:Resource) RETURN n.id AS subjectName, r.type AS predicateUri, m.iri AS objectName"));
        results.addAll(neo4jClient.readFromNeo4j(
                "MATCH (n:BlankNode)-[r:ObjectProperty]->(m:BlankNode) RETURN n.id AS subjectName, r.type AS predicateUri, m.id AS objectName"));
        results.addAll(neo4jClient.readFromNeo4j(
                "MATCH (n:BlankNode)-[r:DatatypeProperty]->(m:Literal) RETURN n.id AS subjectName, r.type AS predicateUri, m.value AS literalValue, m.type AS literalType"));

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
            Property predicate = model.createProperty(Helper.getUri(predicateUri, nsPrefixUri));
            RDFNode object;

            if (subjectName.contains("_:")) {
                String blankNodeId = subjectName.replace("_:", "");
                subject = model.createResource(new AnonId(blankNodeId));
            } else {
                subject = model.createResource(Helper.getUri(subjectName, nsPrefixUri));
            }

            if (Objects.equals(objectName, "null")) {
                String literalValue = result.get("literalValue").asString();
                String literalType = Helper.getUri(result.get("literalType").asString(), nsPrefixUri);
                object = model.createTypedLiteral(literalValue, literalType);
            } else {
                if (objectName.contains("_:")) {
                    String blankNodeId = objectName.replace("_:", "");
                    object = model.createResource(new AnonId(blankNodeId));
                } else {
                    object = model.createResource(Helper.getUri(objectName, nsPrefixUri));
                }
            }

            model.add(subject, predicate, object);
        }

        return model;
    }
}
