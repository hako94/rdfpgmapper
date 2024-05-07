# rdfpgmapper
Eine Java Applikation, um RDF Graphen in neo4j Datenbanken zu importieren und exortieren.

## Allgemeines
Das Projekt war Teil einer Studienarbeit an der DHBW Stuttgart - Campus Horb, welche als Forschungsfrage verfolgt hat, wie gut RDF-Graphen in Property Graphen (neo4j) abbildbar sind.
Es wurden dabei vier verschiedene Transformationsalgorithmen verfolgt.
 
## Verwendung
### Vorbereitung
Um die Anwendung zu verwenden, muss eine Neo4j-Desktop Instanz (>= Version 5.12.0) mit folgender Konfiguration geöffnet sein:
* Database-Uri = "bolt://localhost:7687";
* Database-User = "neo4j";
* Database-Password = "12345678";

Um alle Funktion zu verwenden, muss weiterhin das APOC-Plugin (>= Version 5.12.0) installiert werden.
Weiterhin muss in den Neo4j-Datenbank-Konfigurationen eine Datei *apoc.conf* mit dem Eintrag ``apoc.trigger.enabled=true`` erstellt werden.

### Ausführung
Die Anwendung kann mit folgendem Befehl als Konsolenanwendung ausgeführt werden:

``$ java -jar rdfpgmapper``

Mit dem Start kann die gewünschte Mapping Variante ausgewählt werden, anschließend können RDF-Graphen importiert und exportiert, sowie die Datenbank gelöscht und das Mapping-Format gewechselt werden.

Für den Import und Export der RDF-Daten ist folgendes Format erforderlich:

``<pfad>,<rdf-syntax>``

Bsp.: ``/Users/maxmuster/foaf.ttl,TTL``

Folgende RDF-Serialisierungen werden unterstützt:
* TTL
* RDF/XML
* N-TRIPLE

Für das Testen wird folgender Graph empfohlen: [SimpleFOAF](./ressources/simplefoaf.ttl)