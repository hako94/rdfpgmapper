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

public class Neo4jClient implements AutoCloseable{

    private final Driver driver;

    public Neo4jClient(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    public void writeToNeo4j(List<String> cypherStatements) {
        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                for (String statement : cypherStatements) {
                    tx.run(statement);
                }
                tx.commit();
            } catch (Exception e) {
                System.err.println("Ein Fehler ist aufgetreten: " + e.getMessage());
            }
        }
    }

    public List<Record> readFromNeo4j(String cypherQuery) {
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

    @Override
    public void close() throws RuntimeException {
        driver.close();
    }
}
