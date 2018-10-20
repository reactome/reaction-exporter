[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)
# Reaction exporter
Library to export a single reaction from Reactome into an image.

An automatic layout is applied to the reaction.

Supported output formats include raster (png, jpg, jpeg, gif) and vector (svg, pdf).


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
     <version>0.0.1-SNAPSHOT</version>
</dependency>
```
```xml
<repository>
    <id>pst-release</id>
    <name>EBI Nexus Repository</name>
    <url>http://www.ebi.ac.uk/Tools/maven/repos/content/repositories/pst-release</url>
</repository>
```