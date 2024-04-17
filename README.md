json-schema-validator
=====================

<a href="https://raw.githubusercontent.com/leinad75/json-schema-validator/master/LICENSE">
    <img src="https://img.shields.io/hexpm/l/plug.svg"
         alt="License: Apache 2">
</a>
<a href="http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.github.leinad75%22%20a%3A%22json-schema-validator%22">
    <img src="https://img.shields.io/maven-central/v/io.github.leinad75/json-schema-validator.svg"
         alt="Maven Artifact">
</a>

Maven plugin to validate json files against a json schema. Uses https://github.com/eclipse-vertx/vertx-json-schema library under the covers.

Usage
-----

Determine the latest version of the validator in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.github.leinad75%22%20a%3A%22json-schema-validator%22).

Add the plugin to your pom either in the __plugins__ or __pluginManagement__ block:

```xml
<plugin>
    <groupId>io.github.leinad75</groupId>
    <artifactId>json-validator-maven-plugin</artifactId>
    <version>VERSION</version>
</plugin>
```

Configure one or more __validation__ blocks for the plugin in the __plugins__ block:

```xml
<plugin>
    <groupId>io.github.leinad75</groupId>
    <artifactId>json-validator-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals>
                <goal>validate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <validations>
            <validation>
                <strict>false</strict>
                <metaValidation>false</metaValidation>
                <jsonSchema>${basedir}/src/test/resources/schema.json</jsonSchema>
                <directory>${basedir}/src/main/resources/data</directory>
                <includes>
                    <include>**/*.json</include>
                </includes>
            </validation>
        </validations>
    </configuration>
</plugin>
```

Each __validation__ block specifies the __jsonSchema__ file to validate with as well as the json file(s) to validate from a root __directory__ with standard __includes__ and __excludes__ to select specific file(s).

The configuration option __strict__ (default=false) forces all additionalProperties to false on each node where this property is not set. This allows to detect unknown attributes.
The configuration option __metaValidation__ (default=true) validates the schema file against the meta schema.

Building
--------

Prerequisites:
* JDK11
* Maven 3.3.3+

Building:

    json-validator-maven-plugin> mvn verify

To use the local version you must first install it locally:

    json-validator-maven-plugin> mvn install

You can determine the version of the local build from the pom file.  Using the local version is intended only for testing or development.

License
-------

Published under Apache Software License 2.0, see LICENSE

