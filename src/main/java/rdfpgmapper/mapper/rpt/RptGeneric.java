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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
            if (subject.isURIResource()) {
                subjectArr = mergeResource(subject, 'a');
            } else {
                subjectArr = mergeBlankNode(subject, 'a');
            }

            String[] objectArr;
            RDFNode object = statement.getObject();
            if (object.isURIResource()) {
                objectArr = mergeResource((Resource) object, 'b');
            } else if (object.isAnon()) {
                objectArr = mergeBlankNode((Resource) object, 'b');
            } else {
                objectArr = mergeLiteral((Literal) object);
            }

            Property predicate = statement.getPredicate();

            if (object.isLiteral()) {
                cypher.add(subjectArr[1] + "\n" +
                        objectArr[1] + "\n" +
                        mergeDatatypeProperty(predicate, subjectArr[0], objectArr[0]));
            } else {
                cypher.add(subjectArr[1] + "\n" +
                        objectArr[1] + "\n" +
                        mergeObjectProperty(predicate, subjectArr[0], objectArr[0]));
            }
        }

        return cypher;
    }


    private String[] mergeResource(Resource resource, char postfix) {
        String iri = resource.getURI().replace("'", "_");
        return new String[]{"res" + postfix, "MERGE (res" + postfix + ":Resource {iri: '" + iri + "'})"};
    }

    private String[] mergeBlankNode(Resource resource, char postfix) {
        String id = resource.getId().toString().replace("'", "_");
        return new String[]{"b" + postfix, "MERGE (b" + postfix + ":BlankNode {id: '_:" + id + "'})"};
    }

    private String[] mergeLiteral(Literal literal) {
        String value = literal.getValue().toString().replace("'", "_");
        return new String[]{"lit", "MERGE (lit" + ":Literal {value: '" + value + "', type: '" + literal.getDatatypeURI() + "'})"};
    }

    private String mergeDatatypeProperty(Property property, String subject, String object) {
        return "MERGE (" + subject + ")-[:DatatypeProperty {type: '" + property.getURI() + "'}]->(" + object + ")";
    }


    private String mergeObjectProperty(Property property, String subject, String object) {
        return "MERGE (" + subject + ")-[:ObjectProperty {type: '" + property.getURI() + "'}]->(" + object + ")";

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

        for (Record result : results) {
            String subjectName = result.get("subjectName").asString();
            String predicateUri = result.get("predicateUri").asString();
            String objectName = result.get("objectName").asString();

            Resource subject;
            Property predicate = model.createProperty(predicateUri);
            RDFNode object;

            if (subjectName.contains("_:")) {
                String blankNodeId = subjectName.replace("_:", "");
                subject = model.createResource(new AnonId(blankNodeId));
            } else {
                subject = model.createResource(subjectName);
            }

            if (Objects.equals(objectName, "null")) {
                String literalValue = result.get("literalValue").asString();
                String literalType = result.get("literalType").asString();
                object = model.createTypedLiteral(literalValue, literalType);
            } else {
                if (objectName.contains("_:")) {
                    String blankNodeId = objectName.replace("_:", "");
                    object = model.createResource(new AnonId(blankNodeId));
                } else {
                    object = model.createResource(objectName);
                }
            }

            model.add(subject, predicate, object);
        }

        return model;
    }
}
