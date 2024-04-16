/**
 * Copyright 2014 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.leinad75.maven.plugin.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.github.leinad75.maven.plugin.json.util.FileUtils;
import io.github.leinad75.maven.plugin.json.util.JsonTreeWalker;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaId;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import io.github.leinad75.maven.plugin.json.util.PrettyPrintIterable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;

/**
 * Default implementation for the ValidatorExecutor.
 *
 * @since 1.0
 */
//@Component(role = ValidatorExecutor.class, hint = "default", instantiationStrategy = "per-lookup")
@Mojo(name = "json-schema-validator")
public class DefaultValidatorExecutor implements ValidatorExecutor {

    private static final String PROP_ADDITIONAL_PROPERTIES = "additionalProperties";
    private ValidatorRequest request;

    public void executeValidator(final ValidatorRequest requestInput) throws MojoFailureException, MojoExecutionException {
        request = requestInput;

        for (final Validation validation : request.getValidations()) {
            performValidation(validation);
        }
    }

    private void performValidation(final Validation validation) throws MojoExecutionException, MojoFailureException {
        final List<String> jsonFiles = new ArrayList<>();
        String schemaFile = null;

        if (!StringUtils.isEmpty(validation.getDirectory())) {
            jsonFiles.addAll(FileUtils.getListOfFiles(validation));
        }
        if (!StringUtils.isEmpty(validation.getJsonFile())) {
            jsonFiles.add(validation.getJsonFile());
        }
        if (jsonFiles.isEmpty()) {
            request.getLog().warn("No JSON files to validate");
            return;
        }
        if (StringUtils.isEmpty(validation.getJsonSchema())) {
            request.getLog().warn("No schema file file given");
            return;
        }
        schemaFile = validation.getJsonSchema();
        List<Exception> exceptions = new ArrayList<>();
        for (final String jsonFile : jsonFiles) {
            try {
                validateAgainstSchema(jsonFile, schemaFile, validation.isStrict());
            } catch (MojoExecutionException | MojoFailureException e) {
                exceptions.add(e);
            }
        }

        if (!exceptions.isEmpty()) {
            request.getLog().error("Failed validating json files, " + exceptions.size() + " failures");
            for (Exception e : exceptions) {
                request.getLog().error(e.getMessage());
            }
            throw new MojoFailureException("Failed while validating json files.");
        } else {
            request.getLog().info("Succesfully processed " + jsonFiles.size() + " files.");
        }
    }


    private void validateAgainstSchema(final String jsonDataFile, final String schemaFile, boolean isStrict) throws MojoExecutionException, MojoFailureException {
        request.getLog().debug("File: " + jsonDataFile + " - validating against " + schemaFile + ", isStrict=" + isStrict);

        try {
            JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V7, builder ->
                builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://localhost", "classpath:"))
            );
            SchemaValidatorsConfig config = new SchemaValidatorsConfig();
            config.setPathType(PathType.JSON_POINTER);

            JsonNode schemaNode = loadJsonNode(schemaFile);
            JsonNode schemaNodeSchemaId = schemaNode.get("$schema");
            String schemaId = schemaNodeSchemaId instanceof TextNode  ? schemaNodeSchemaId.asText() : SchemaId.V7;

            // validate schema against meta schema
            JsonSchema metaSchema = jsonSchemaFactory.getSchema(SchemaLocation.of(schemaId), config);
            Set<ValidationMessage> schemaValidationMessages = metaSchema.validate(schemaNode);
            if (!schemaValidationMessages.isEmpty()) {
                request.getLog().error(new PrettyPrintIterable<>(schemaValidationMessages).toString());
                throw new MojoFailureException("Illegal schema " + schemaFile + ", not a valid schema " + SchemaId.V7);
            }

            // validate test file against schema
            if (isStrict) {
                forceAdditionalProperties(schemaNode);
            }
            JsonSchema schema = jsonSchemaFactory.getSchema(schemaNode, config);

            JsonNode testFileJsonNode = loadJsonNode(jsonDataFile);
            Set<ValidationMessage> validationMessages = schema.validate(testFileJsonNode);

            if (!validationMessages.isEmpty()) {
                PrettyPrintIterable<ValidationMessage> prettyPrintIterable = new PrettyPrintIterable<>(validationMessages);
                request.getLog().debug(prettyPrintIterable.toString());
                throw new MojoFailureException("Failed to validate JSON from file " + jsonDataFile + " against " + schemaFile + ": " + prettyPrintIterable);
            }
            request.getLog().info("File: " + jsonDataFile + " - validated - Success");

        } catch (final Exception e) {
            request.getLog().error(e);
            throw new MojoFailureException(e.getMessage());
        }
    }

    private void forceAdditionalProperties(JsonNode schemaNode) {
        new JsonTreeWalker().walkTree(schemaNode, (nodeName, objectNode) -> {
            JsonNode typeNode = objectNode.get("type");
            if (typeNode instanceof TextNode && "object".equals(typeNode.asText())) {
                request.getLog().debug("found schema object: " + objectNode);
                JsonNode addPropsNode = objectNode.get(PROP_ADDITIONAL_PROPERTIES);
                // set if no additionalProperties
                if (addPropsNode == null) {
                    request.getLog().debug("disabling additional properties for node " + nodeName);
                    objectNode.set(PROP_ADDITIONAL_PROPERTIES, BooleanNode.FALSE);
                }
            }
        });
    }

    private JsonNode loadJsonNode(final String file) throws MojoExecutionException {
        try {
            JsonNode node = new ObjectMapper().readTree(new File(file));
            request.getLog().debug("File: " + file + " - parsing - Success");
            return node;
        } catch (final IOException e) {
            request.getLog().error("File: " + file + " - parsing - Failure");
            throw new MojoExecutionException("Failed to parse JSON from file " + file + " - " + e.getMessage(), e);
        }
    }

}
