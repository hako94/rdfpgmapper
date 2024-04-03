package rdfpgmapper.rdf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.FileManager;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class JenaClient {
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
    public void writeModel(Model model, String outputFilePath, RDFFormat format) {
        try (OutputStream out = new FileOutputStream(outputFilePath)) {
            RDFDataMgr.write(out, model, format);
            System.out.println("RDF data successfully written to " + outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
