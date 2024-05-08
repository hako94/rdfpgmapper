package rdfpgmapper.mapper.pgt.pgtcomplete.schemamodel;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import rdfpgmapper.utils.Helper;

/**
 * Eine Hilfsklasse zur Erstellung eines RDFGraphModel aus einem Jena RDF Model.
 * Diese Klasse verarbeitet ein RDF Model, um daraus ein strukturiertes Schema-Modell
 * für die weitere Verarbeitung und Mapping auf Neo4j zu erstellen.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public class RDFModelBuilder {

    /**
     * Erstellt ein RDFGraphModel aus einem gegebenen Jena Model. Dieses Modell umfasst
     * die Definitionen von Klassen, Eigenschaften und deren Hierarchien und Beziehungen
     * gemäß RDF- und RDFS-Spezifikationen.
     *
     * @param model Das RDF Model, aus dem das RDFGraphModel erstellt wird.
     * @return Ein vollständig aufgebautes RDFGraphModel, das für das Mapping verwendet wird.
     */
    public static RDFGraphModel buildGraphModel(Model model) {
        RDFGraphModel graphModel = new RDFGraphModel();

        // Extraktion von RDF-Klassen und deren Hierarchien
        StmtIterator classesStatements = model.listStatements(null, RDF.type, RDFS.Class);
        while (classesStatements.hasNext()) {
            Statement stmt = classesStatements.nextStatement();
            String subjectUri = Helper.getPrefixedName(stmt.getSubject().getURI(), model);
            RDFClass rdfSubjectClass = graphModel.getClass(subjectUri);
            if (rdfSubjectClass == null) {
                rdfSubjectClass = new RDFClass(subjectUri);
                graphModel.addClass(rdfSubjectClass);
            }
        }

        // Lesen von RDF-Eigenschaften und deren Hierarchien
        StmtIterator subClassStatements = model.listStatements(null, RDFS.subClassOf, (Resource) null);
        while (subClassStatements.hasNext()) {
            Statement stmt = subClassStatements.nextStatement();
            String subjectUri = Helper.getPrefixedName(stmt.getSubject().getURI(), model);
            String objectUri = Helper.getPrefixedName(stmt.getObject().asResource().getURI(), model);
            RDFClass rdfSubjectClass = graphModel.getClass(subjectUri);
            if (rdfSubjectClass == null) {
                rdfSubjectClass = new RDFClass(subjectUri);
                graphModel.addClass(rdfSubjectClass);
            }
            RDFClass rdfObjectClass = graphModel.getClass(subjectUri);
            if (rdfObjectClass == null) {
                rdfObjectClass = new RDFClass(objectUri);
                graphModel.addClass(rdfObjectClass);
            }
            graphModel.getClass(objectUri).addSubclass(subjectUri);
            graphModel.getClass(subjectUri).addSuperclass(objectUri);
        }

        // Lesen von Subklassen-Beziehungen
        StmtIterator propertyStatements = model.listStatements(null, RDF.type, RDF.Property);
        while (propertyStatements.hasNext()) {
            Statement stmt = propertyStatements.nextStatement();
            String subjectUri = Helper.getPrefixedName(stmt.getSubject().getURI(), model);
            if (!graphModel.properties.containsKey(subjectUri)) {
                graphModel.addProperty(new RDFProperty(subjectUri));
            }
        }

        // Lesen von Subproperty-Beziehungen
        StmtIterator subPropertyStatements = model.listStatements(null, RDFS.subPropertyOf, (Resource) null);
        while (subPropertyStatements.hasNext()) {
            Statement stmt = subPropertyStatements.nextStatement();
            String subjectUri = Helper.getPrefixedName(stmt.getSubject().getURI(), model);
            String objectUri = Helper.getPrefixedName(stmt.getObject().asResource().getURI(), model);
            if (!graphModel.properties.containsKey(subjectUri)) {
                graphModel.addProperty(new RDFProperty(subjectUri));
            }
            if (!graphModel.properties.containsKey(objectUri)) {
                graphModel.addProperty(new RDFProperty(objectUri));
            }
            graphModel.getProperty(objectUri).addSubproperty(subjectUri);
            graphModel.getProperty(subjectUri).addSuperproperty(objectUri);
        }

        // Zuweisung von Domains zu Properties
        StmtIterator domainStatements = model.listStatements(null, RDFS.domain, (Resource) null);
        while (domainStatements.hasNext()) {
            Statement stmt = domainStatements.nextStatement();
            String propertyUri = Helper.getPrefixedName(stmt.getSubject().getURI(), model);
            String domainUri = Helper.getPrefixedName(stmt.getObject().asResource().getURI(), model);
            if (!graphModel.properties.containsKey(propertyUri)) {
                graphModel.addProperty(new RDFProperty(propertyUri));
            }
            graphModel.getProperty(propertyUri).addDomain(domainUri);
        }

        // Zuweisung von Ranges zu Properties
        StmtIterator rangeStatements = model.listStatements(null, RDFS.range, (Resource) null);
        while (rangeStatements.hasNext()) {
            Statement stmt = rangeStatements.nextStatement();
            String propertyUri = Helper.getPrefixedName(stmt.getSubject().getURI(), model);
            String rangeUri = Helper.getPrefixedName(stmt.getObject().asResource().getURI(), model);
            if (!graphModel.properties.containsKey(propertyUri)) {
                graphModel.addProperty(new RDFProperty(propertyUri));
            }
            graphModel.getProperty(propertyUri).addRange(rangeUri);
            if (rangeUri.startsWith("xsd") && !rangeUri.equalsIgnoreCase("xsd_anyuri")) {
                graphModel.getProperty(propertyUri).setIsLiteral();
            }
        }

        return graphModel;
    }
}