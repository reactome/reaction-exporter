[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)

# Reaction exporter
Creates diagram files out of single reactions, using the graph database as resource.

Taking a single reaction, this library performs 3 steps:
1. queries the graph database to retrieve the reaction, its participants and the compartments containing them.
2. applies an automatic layout to show a left to right view of the reaction, while keeping every participant in its compartment.
3. generates a diagram (_.json_) and a graph (_.graph.json_) files following Reactome [specifications](https://reactome.org/dev/diagram/pathway-diagram-specs).

## Usage:
This project can be used as a standalone compiled tool through the provided _reaction-exporter.jar_ file.
```
java -jar reaction-exporter.jar [--help] [(-h|--host) <host>] [(-p|--port) <port>] [(-u|--user) <user>] (-d|--password) <password> (-s|--stId) <stId> (-o|--path) <path> [(-f|--format) <format>]
  [--help]
        Prints this help message.

  [(-h|--host) <host>]
        The neo4j host (default: localhost)
  [(-p|--port) <port>]
        The neo4j port (default: 7474)
  [(-u|--user) <user>]
        The neo4j user (default: neo4j)
  (-d|--password) <password>
        The neo4j password (default: neo4j)

  (-s|--stId) <stId>
        Reaction stable identifier

  (-o|--path) <path>
        Output path. File will be named 'path'/'stId'.'format'

  [(-f|--format) <format>]
        Output format (default: png)
```

Or as a maven dependency:

```xml
<dependency>
     <groupId>org.reactome.server.tools</groupId>
     <artifactId>reaction-exporter</artifactId>
     <version>1.1.1</version>
</dependency>
```
```xml
<repository>
    <id>pst-release</id>
    <name>EBI Nexus Repository</name>
    <url>http://www.ebi.ac.uk/Tools/maven/repos/content/repositories/pst-release</url>
</repository>
```
