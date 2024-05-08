package rdfpgmapper.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Diese Klasse bietet eine Schnittstelle zur Interaktion mit einer Neo4j-Datenbank.
 * Sie ermöglicht das Schreiben und Lesen von Cypher-Statements in einer Neo4j-Datenbank.
 *
 * @author Hannes Kollert
 * @version 1.0
 */
public class Neo4jClient implements AutoCloseable {

    private Driver driver;
    private final String databaseUri;
    private final String databaseUser;
    private final String databasePassword;


    /**
     * Konstruktor zur Initialisierung eines Neo4jClient-Objekts mit den Zugangsdaten zur Datenbank.
     *
     * @param uri      Die URI der Neo4j-Datenbank.
     * @param user     Der Benutzername für die Authentifizierung.
     * @param password Das Passwort für die Authentifizierung.
     */
    public Neo4jClient(String uri, String user, String password) {
        databaseUri = uri;
        databaseUser = user;
        databasePassword = password;
    }

    /**
     * Schreibt eine Liste von Cypher-Statements in die Neo4j-Datenbank.
     *
     * @param cypherStatements Eine Liste von Cypher-Statements, die in Neo4j ausgeführt werden sollen.
     */
    public void writeToNeo4j(List<String> cypherStatements) {
        driver = GraphDatabase.driver(databaseUri, AuthTokens.basic(databaseUser, databasePassword));
        try (Session session = driver.session()) {
            for (String statement : cypherStatements) {
                session.run(statement);
            }
        } catch (Exception e) {
            System.err.println("Ein Fehler ist aufgetreten: " + e.getMessage());
        }
    }

    /**
     * Liest Daten aus der Neo4j-Datenbank basierend auf einem gegebenen Cypher-Query.
     *
     * @param cypherQuery Das Cypher-Query, das ausgeführt werden soll.
     * @return Eine Liste von Records, die die Ergebnisse des Queries enthalten.
     */
    public List<Record> readFromNeo4j(String cypherQuery) {
        driver = GraphDatabase.driver(databaseUri, AuthTokens.basic(databaseUser, databasePassword));

        List<Record> records = new ArrayList<>();
        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                Result result = tx.run(cypherQuery);
                while (result.hasNext()) {
                    records.add(result.next());
                }
                tx.commit();
            } catch (Exception e) {
                System.err.println("Ein Fehler ist aufgetreten beim Lesen von Daten aus Neo4j: " + e.getMessage());
            }
        }
        return records;
    }

    /**
     * Schließt die Verbindung zur Neo4j-Datenbank.
     * Muss aufgerufen werden, um Ressourcen ordnungsgemäß freizugeben.
     */
    @Override
    public void close() throws RuntimeException {
        driver.close();
    }
}
