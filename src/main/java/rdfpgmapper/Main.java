package rdfpgmapper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import rdfpgmapper.rdf.JenaClient;

public class Main {
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Eingabe nur im Format: java Main <path-to-input-rdf-file> <input-rdf-format> <output-file-path>");
            System.exit(1);
        }

        String filePath = args[0];
        String format = args[1].toUpperCase();
        String outputFilePath = args[2];

        switch (format) {
            case "TTL":
                break;
            case "RDF/XML":
                break;
            case "N-TRIPLE":
                break;
            default:
                System.out.println("Format nicht unterstützt: " + format);
                System.exit(2);
        }

        JenaClient jenaClient = new JenaClient();
        Model model = jenaClient.parseRDFFile(filePath, format);

        RDFFormat outputFormat = RDFFormat.TURTLE_PRETTY;

        jenaClient.writeModel(model, outputFilePath, outputFormat);

    }

}
