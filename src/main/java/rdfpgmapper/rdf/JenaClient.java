package rdfpgmapper.rdf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.FileManager;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Diese Klasse bietet Methoden zum Einlesen und Schreiben von RDF-Modellen.
 * Sie verwendet Apache Jena, eine Bibliothek zur Verarbeitung von RDF-Daten.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public class JenaClient {

    /**
     * Liest eine RDF-Datei von einem angegebenen Pfad und lädt sie in ein Jena Model.
     * Unterstützt verschiedene RDF-Formate.
     *
     * @param filePath Der Dateipfad, von dem das RDF gelesen werden soll.
     * @param format   Das Format des RDF-Dokuments (z.B. "RDF/XML", "TTL").
     * @return Ein Jena Model, das die geladenen RDF-Daten enthält.
     * @throws IllegalArgumentException wenn die angegebene Datei nicht gefunden wird.
     */
    public Model parseRDFFile(String filePath, String format) {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = FileManager.get().open(filePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Datei: " + filePath + " nicht gefunden.");
            }
            model.read(in, null, format);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }

    /**
     * Schreibt ein Jena Model in eine RDF-Datei im angegebenen Format.
     *
     * @param model          Das Jena Model, das geschrieben werden soll.
     * @param outputFilePath Der Pfad, an dem die RDF-Datei gespeichert werden soll.
     * @param format         Das RDF-Format, in dem das Model geschrieben wird (z.B. RDFFormat.RDFXML).
     */
    public void writeModel(Model model, String outputFilePath, RDFFormat format) {
        try (OutputStream out = new FileOutputStream(outputFilePath)) {
            RDFDataMgr.write(out, model, format);
            System.out.println("RDF erfolgreich geschrieben: " + outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
