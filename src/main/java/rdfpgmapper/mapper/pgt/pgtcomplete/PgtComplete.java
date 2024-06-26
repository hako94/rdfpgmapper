package rdfpgmapper.mapper.pgt.pgtcomplete;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.neo4j.driver.Record;
import rdfpgmapper.mapper.Mapper;
import rdfpgmapper.mapper.pgt.pgtcomplete.schemamodel.RDFClass;
import rdfpgmapper.mapper.pgt.pgtcomplete.schemamodel.RDFGraphModel;
import rdfpgmapper.mapper.pgt.pgtcomplete.schemamodel.RDFModelBuilder;
import rdfpgmapper.mapper.pgt.pgtcomplete.schemamodel.RDFProperty;
import rdfpgmapper.neo4j.Neo4jClient;
import rdfpgmapper.utils.Helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Eine Implementierung des {@link Mapper} Interfaces, die komplexe PGT-Transformationen von RDF zu Property Graphen ermöglicht.
 * Diese Klasse unterstützt erweiterte Mapping-Strategien, einschließlich Trigger für Domain- und Range-Validierung,
 * sowie die Behandlung von Subklassen- und Subeigenschaften-Beziehungen.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public class PgtComplete implements Mapper {

    private final Neo4jClient neo4jClient;

    /**
     * Konstruktor, der den Neo4jClient initialisiert.
     *
     * @param client Der Client zur Kommunikation mit der Neo4j-Datenbank.
     */
    public PgtComplete(Neo4jClient client) {
        this.neo4jClient = client;
    }

    /**
     * Erstellt ein Schema für Property Graphen in Neo4j, basierend auf RDF-Daten.
     * Generiert Cypher-Statements für die Erstellung von Constraints und Triggern, die Datenintegrität gewährleisten.
     *
     * @param model Das RDF-Modell, aus dem das Schema erstellt wird.
     * @return Eine Liste von Cypher-Statements, die das Schema in Neo4j definieren.
     */
    @Override
    public List<String> mapRdfToPgSchema(Model model) {
        List<String> cypher = new ArrayList<>();
        cypher.add("CREATE CONSTRAINT FOR (r:Resource) REQUIRE r.iri IS NOT NULL;");
        cypher.add("CREATE CONSTRAINT FOR (b:BlankNode) REQUIRE b.id IS NOT NULL;");

        cypher.add("CREATE CONSTRAINT FOR (r:Resource) REQUIRE r.iri IS UNIQUE;");
        cypher.add("CREATE CONSTRAINT FOR (b:BlankNode) REQUIRE b.id IS UNIQUE;");

        RDFGraphModel graphModel = RDFModelBuilder.buildGraphModel(model);

        for (RDFProperty property : graphModel.getProperties()) {
            if (!property.isLiteral()) {
                String domainTrigger = createObjectDomainTrigger(property, graphModel);
                String rangeTrigger = createObjectRangeTrigger(property, graphModel);
                if (domainTrigger != null) cypher.add(domainTrigger);
                if (rangeTrigger != null) cypher.add(rangeTrigger);
            } else {
                String literalTrigger = createLiteralTrigger(property, graphModel);
                if (literalTrigger != null) cypher.add(literalTrigger);
            }
        }

        for (RDFClass rdfClass : graphModel.getClasses()) {
            for (String subclass : rdfClass.getSubclasses()) {
                String triggerCypher = String.format("CALL apoc.trigger.add('superclass_%s', " + "'MATCH (n:%s) WHERE NOT n:%s " + "SET n:%s'," + "{phase:'before'});", subclass, subclass, rdfClass.getUri(), rdfClass.getUri());
                cypher.add(triggerCypher);
            }
        }

        for (RDFProperty rdfProperty : graphModel.getProperties()) {
            for (String subProperty : rdfProperty.getSubproperties()) {
                String triggerCypher = String.format("CALL apoc.trigger.add('superproperty_%s', " + "'MATCH (n)-[r:%s]->(m) " + "WHERE NOT (n)-[:%s]->(m) " + "MERGE (n)-[:%s]->(m)', " + "{phase:'before'});", subProperty, subProperty, rdfProperty.getUri(), rdfProperty.getUri());
                cypher.add(triggerCypher);
            }
        }

        return cypher;
    }

    private String createObjectDomainTrigger(RDFProperty property, RDFGraphModel graphModel) {
        List<String> domainConditions = property.getDomains().stream().map(domain -> {
            Set<String> validClasses = new HashSet<>();
            collectSubclasses(domain, graphModel, validClasses);
            return validClasses.stream().map(cls -> "n:" + cls).collect(Collectors.joining(" OR "));
        }).map(condition -> "(" + condition + ")").collect(Collectors.toList());

        domainConditions.removeIf(condition -> condition.equals("()"));
        domainConditions.removeIf(condition -> condition.equals(""));

        if (domainConditions.isEmpty()) {
            return null;
        }

        String combinedConditions = String.join(" AND ", domainConditions);

        return String.format("CALL apoc.trigger.add('domain_%s', " + "'MATCH (n)-[r:%s]->(m) " +
                //"WHERE NOT %s DELETE r', " +
                "CALL apoc.util.validate(NOT (%s), \\\"Violation of domain constraints for %s\\\", NULL)', " + "{phase:'before'});", property.getUri(), property.getUri(), combinedConditions, property.getUri());
    }

    private String createObjectRangeTrigger(RDFProperty property, RDFGraphModel graphModel) {
        List<String> rangeConditions = property.getRanges().stream().map(range -> {
            Set<String> validClasses = new HashSet<>();
            collectSubclasses(range, graphModel, validClasses);
            return validClasses.stream().map(cls -> "m:" + cls).collect(Collectors.joining(" OR "));
        }).map(condition -> "(" + condition + ")").collect(Collectors.toList());

        rangeConditions.removeIf(condition -> condition.equals("()"));

        if (rangeConditions.isEmpty()) {
            return null;
        }

        String combinedConditions = String.join(" AND ", rangeConditions);

        combinedConditions = (combinedConditions + " OR " + "m:BlankNode");

        return String.format("CALL apoc.trigger.add('range_%s', " + "'MATCH (n)-[r:%s]->(m) " +
                //"WHERE NOT %s DELETE r', " +
                "CALL apoc.util.validate(NOT (%s), \\\"Violation of range constraints for %s\\\", NULL)', " + "{phase:'before'});", property.getUri(), property.getUri(), combinedConditions, property.getUri());
    }

    private void collectSubclasses(String cls, RDFGraphModel graphModel, Set<String> validClasses) {
        RDFClass rdfClass = graphModel.getClass(cls);
        if (rdfClass != null && validClasses.add(cls)) {
            rdfClass.getSubclasses().forEach(subclass -> collectSubclasses(subclass, graphModel, validClasses));
        }
    }

    private String createLiteralTrigger(RDFProperty property, RDFGraphModel graphModel) {
        List<String> domainConditions = property.getDomains().stream().map(domain -> {
            Set<String> validClasses = new HashSet<>();
            collectSubclasses(domain, graphModel, validClasses);
            return validClasses.stream().map(cls -> "n:" + cls).collect(Collectors.joining(" OR "));
        }).map(condition -> "(" + condition + ")").collect(Collectors.toList());

        String domainCondition = domainConditions.isEmpty() ? "" : String.join(" AND ", domainConditions);

        String rangeCondition = property.getRanges().stream().findFirst().map(range -> " AND n." + property.getUri() + " ENDS WITH \\\"" + range + "\\\"").orElse("");

        if (!domainCondition.isEmpty() || !rangeCondition.isEmpty()) {
            String conditionString = (domainCondition.isEmpty() ? "" : domainCondition) + (rangeCondition.isEmpty() ? "" : rangeCondition);
            return String.format("CALL apoc.trigger.add('datatype_%s_validate', " + "'MATCH (n) WHERE n.%s IS NOT NULL " + "CALL apoc.util.validate(NOT (%s), \\\"Violation of datatype constraints for %s\\\", NULL)', " + "{phase:'before'});", property.getUri(), property.getUri(), conditionString, property.getUri());
        }

        return null;
    }

    /**
     * Konvertiert ein RDF-Modell in Cypher-Statements zur Erstellung von Instanzen in Neo4j.
     * Wandelt RDF-Statements in entsprechende Neo4j Graph-Strukturen um, einschließlich Ressourcen, Blank Nodes, Literale und Relationen.
     *
     * @param model Das RDF-Modell, das in Neo4j-Instanzdaten gemappt wird.
     * @return Eine Liste von Cypher-Statements, die die Instanzdaten in Neo4j darstellen.
     */
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

                if (subject.isURIResource()) {
                    subjectArr = mergeResource(subject, 'a', model);
                } else {
                    subjectArr = mergeBlankNode(subject, 'a', model);
                }

                String[] objectArr;

                if (object.isURIResource()) {
                    objectArr = mergeResource((Resource) object, 'b', model);
                } else {
                    objectArr = mergeBlankNode((Resource) object, 'b', model);
                }

                cypher.add(subjectArr[1] + "\n" + objectArr[1] + "\n" + mergeProperty(predicate, subjectArr[0], objectArr[0], model));

            }
        }

        return Helper.addNodeForNsPrefixUriDeclaration(model, cypher);
    }

    private String[] mergeResource(Resource resource, char postfix, Model model) {
        String types = String.valueOf(getRessourceTypes(resource, model));
        return new String[]{"res" + postfix, "MERGE (res" + postfix + ":Resource" + types + "{ iri: '" + Helper.getPrefixedName(resource.getURI(), model) + "'})"};
    }

    private String[] mergeBlankNode(Resource resource, char postfix, Model model) {
        String types = String.valueOf(getRessourceTypes(resource, model));
        return new String[]{"b" + postfix, "MERGE (b" + postfix + ":BlankNode" + types + " {id: '_:" + resource.getId() + "'})"};
    }

    private String mergeRessourceLiteral(Resource resource, Property predicate, Literal literal, Model model) {
        String types = String.valueOf(getRessourceTypes(resource, model));
        String litValue = literal.getValue().toString().replace("'", "_");

        return "MERGE (res:Resource" + types + " {iri: '" + Helper.getPrefixedName(resource.getURI(), model) + "'})" + "SET res." + Helper.getPrefixedName(predicate.getURI(), model) + " = '" + litValue + "^^" + Helper.getPrefixedName(literal.getDatatypeURI(), model) + "'";
    }

    private String mergeBlankNodeLiteral(Resource resource, Property predicate, Literal literal, Model model) {
        String types = String.valueOf(getRessourceTypes(resource, model));
        String litValue = literal.getValue().toString().replace("'", "''");

        return "MERGE (b:BlankNode" + types + " {id: '_:" + resource.getId() + "'})" + "SET b." + Helper.getPrefixedName(predicate.getURI(), model) + " = '" + litValue + "^^" + Helper.getPrefixedName(literal.getDatatypeURI(), model) + "'";
    }

    private String mergeProperty(Property property, String subject, String object, Model model) {

        return "MERGE (" + subject + ")-[:" + Helper.getPrefixedName(property.getURI(), model) + "]->(" + object + ")";
    }

    private StringBuilder getRessourceTypes(Resource resource, Model model) {
        NodeIterator typesIterator = model.listObjectsOfProperty(resource, RDF.type);
        StringBuilder types = new StringBuilder();
        while (typesIterator.hasNext()) {
            String type = Helper.getPrefixedName(typesIterator.next().asResource().getURI(), model);
            types.append(":").append(type);
        }
        return types;
    }

    /**
     * Mappt Daten aus einem Neo4j Property Graph zurück in ein RDF-Modell.
     * Liest Daten aus Neo4j und erstellt ein RDF-Modell, das diese Daten repräsentiert.
     *
     * @return Ein Jena Model, das die aus Neo4j gelesenen Daten repräsentiert.
     */
    @Override
    public Model mapPgToRdf() {
        Model model = ModelFactory.createDefaultModel();


        List<Record> results = neo4jClient.readFromNeo4j("MATCH (n)-[r]->(m) " + "RETURN n.iri AS subjectIri, n.id AS subjectId, properties(n) as subjectProperties, " + "TYPE(r) AS predicateUri, " + "m.iri AS objectIri, m.id AS objectId, properties(m) as objectProperties");

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
