package rdfpgmapper;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import rdfpgmapper.mapper.MapperApi;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Mit welcher Mapping Methode möchten Sie arbeiten??");

            System.out.println("1 - RPT-Simple");
            System.out.println("2 - RPT-Generic");
            System.out.println("3 - PGT-Simple");
            System.out.println("4 - PGT-Complete");

            int option = scanner.nextInt();
            scanner.nextLine();

            MapperApi mapperApi = new MapperApi(option);

            while (true) {
                try {
                    System.out.println("\nWas möchten Sie tun?");
                    System.out.println("1 - RDF-Graph importieren");
                    System.out.println("2 - RDF-Graph exportieren");
                    System.out.println("3 - Neo4j-Datenbank leeren");
                    System.out.println("4 - Beenden");

                    System.out.print("Wählen Sie eine Option: ");

                    option = scanner.nextInt();
                    scanner.nextLine();

                    switch (option) {
                        case 1:

                            Pair<String, String> input = choosePathAndFormat(scanner);

                            if (input == null) {
                                break;
                            }

                            mapperApi.importRdf(input.getLeft(), input.getRight());

                            System.out.println("RDF-Daten wurden importiert.");

                            break;
                        case 2:
                            Pair<String, String> output = choosePathAndFormat(scanner);

                            if (output == null) {
                                break;
                            }

                            mapperApi.exportRdf(output.getLeft(), output.getRight());

                            System.out.println("RDF-Daten wurden exportiert.");

                            break;
                        case 3:
                            mapperApi.clearDatabase();
                            System.out.println("Alle Daten wurden gelöscht.");

                            break;
                        case 4:
                            System.out.println("Programm beendet.");
                            return;
                        default:
                            System.out.println("Ungültige Option. Bitte wählen Sie 1, 2 oder 3.");
                            break;
                    }
                } finally {

                    mapperApi.close();

                }
            }

        }


    }

    private static Pair<String, String> choosePathAndFormat(Scanner scanner) {

        System.out.println("Geben Sie den Pfad und das gewünschte Format folgendermaßen an: <pfad>,<format>");

        String[] in = scanner.nextLine().split(",");
        if (in.length < 2) {
            System.out.println("Eingabe nur im Format: java Main <pfad>,<format>");
            return null;
        }

        String filePath = in[0];
        String format = in[1].toUpperCase();

        switch (format) {
            case "TTL":
                break;
            case "RDF/XML":
                break;
            case "N-TRIPLE":
                break;
            default:
                System.out.println("Format nicht unterstützt: " + format);
                return null;
        }

        return new ImmutablePair<>(filePath, format);
    }

}
