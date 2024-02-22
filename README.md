# maven-redhat-dependency-utils

The main class purpose is to elaborate dependencies that comes from a Maven pom, including in the specified output file only dependencies that match a filter criteria

The input file should contain dependencies only:

```asciidoc
<dependencies>
    <dependency>
          .
          .  (each dependency props [groupId, artifactId, version, scope, etc ...])
          .
    </dependency>
        .
        .   (and so on)
        .
</dependencies>
```

## How Run with JBang

* Usage:

`jbang run Parser.java [inputFile] [outputFile] [filter]`

* Ex.:

`jbang run Parser.java dependencies.xml filtered-deps.xml redhat`

In this example, only `redhat` productuzed artifacts are included in `filtered-deps.xml` file, excluding community ones


