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
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.SpecVersionDetector;
import io.github.leinad75.maven.plugin.json.util.FileUtils;
import io.github.leinad75.maven.plugin.json.util.JsonTreeWalker;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import io.github.leinad75.maven.plugin.json.util.PrettyPrintIterable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Default implementation for the ValidatorExecutor.
 *
 * @since 1.0
 */
public class DefaultValidatorExecutor implements ValidatorExecutor {

    private static final String PROP_ADDITIONAL_PROPERTIES = "additionalProperties";
    private final ValidatorRequest request;
    private final Validation validation;

    private final List<String> jsonFiles;
    private final String schemaFile;
    private final SchemaValidatorsConfig config;

    private JsonNode schemaNode;
    private JsonNode strictSchemaNode;
    JsonSchemaFactory jsonSchemaFactory;

    public DefaultValidatorExecutor(final ValidatorRequest requestInput, final Validation validation)
        throws MojoExecutionException {
        this.request = requestInput;
        this.validation = validation;

        jsonFiles = new ArrayList<>();
        schemaFile = validation.getJsonSchema();

        if (!StringUtils.isEmpty(validation.getDirectory())) {
            jsonFiles.addAll(FileUtils.getListOfFiles(validation));
        }
        if (!StringUtils.isEmpty(validation.getJsonFile())) {
            jsonFiles.add(validation.getJsonFile());
        }

        config = new SchemaValidatorsConfig();
        config.setPathType(PathType.JSON_POINTER);
    }

    public void performValidation() throws MojoExecutionException, MojoFailureException {

        if (jsonFiles.isEmpty()) {
            request.getLog().warn("No JSON files to validate");
            return;
        }
        if (StringUtils.isEmpty(validation.getJsonSchema())) {
            request.getLog().warn("No schema file file given");
            return;
        }

        schemaNode = loadSchema(schemaFile, validation.isMetaValidation());

        List<Exception> exceptions = new ArrayList<>();
        for (final String jsonFile : jsonFiles) {
            try {
                validateAgainstSchema(jsonFile, validation.isStrict());
            } catch (MojoFailureException e) {
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

    protected JsonNode loadSchema(String schemaFile, boolean isMetaValidation)
        throws MojoFailureException {
        schemaNode = loadJsonNode(schemaFile);
        VersionFlag versionFlag;
        try {
            versionFlag = SpecVersionDetector.detect(schemaNode);
        } catch (JsonSchemaException jse) {
            request.getLog().debug(jse);
            throw new MojoFailureException("Illegal schema " + schemaFile + ": " + jse.getMessage());
        }

        jsonSchemaFactory = JsonSchemaFactory.getInstance(versionFlag, builder ->
            builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://localhost", "classpath:"))
        );

        if (isMetaValidation) {
            // validate schema against meta schema
            JsonSchema metaSchema = jsonSchemaFactory.getSchema(SchemaLocation.of(versionFlag.getId()), config);
            Set<ValidationMessage> schemaValidationMessages = metaSchema.validate(schemaNode);
            if (!schemaValidationMessages.isEmpty()) {
                request.getLog().error(new PrettyPrintIterable<>(schemaValidationMessages).toString());
                throw new MojoFailureException("Illegal schema " + schemaFile + ", not a valid schema " + versionFlag.getId());
            }
        }
        return schemaNode;
    }

    private void validateAgainstSchema(final String jsonDataFile, boolean isStrict) throws MojoFailureException {
        request.getLog().debug("File: " + jsonDataFile + " - validating against " + schemaFile + ", isStrict=" + isStrict);

        try {
            JsonNode testFileJsonNode = loadJsonNode(jsonDataFile);

            // do a strict validation to show either warnings or fail
            JsonSchema strictSchema = jsonSchemaFactory.getSchema(getStrictSchemaNode(), config);
            Set<ValidationMessage> strictValidationMessages = strictSchema.validate(testFileJsonNode);

            if (isStrict && !strictValidationMessages.isEmpty()) {
                PrettyPrintIterable<ValidationMessage> prettyPrintIterable = new PrettyPrintIterable<>(strictValidationMessages);
                throw new MojoFailureException("Failed to validate JSON from file " + jsonDataFile + " against " + schemaFile + ": " + prettyPrintIterable);
            }

            // default validation in non-strict mode
            if (!isStrict) {
                JsonSchema schema = jsonSchemaFactory.getSchema(schemaNode, config);
                Set<ValidationMessage> defaultValidationMessages = schema.validate(testFileJsonNode);
                if (!defaultValidationMessages.isEmpty()) {

                    // log all results from strict validation, which are not errors, as warnings
                    Collection<ValidationMessage> warningMessages = retainAllValidationMessages(strictValidationMessages, defaultValidationMessages);
                    if (!warningMessages.isEmpty()) {
                        request.getLog().warn(new PrettyPrintIterable<>(warningMessages).toString());
                    }

                    PrettyPrintIterable<ValidationMessage> prettyPrintIterable = new PrettyPrintIterable<>(defaultValidationMessages);
                    request.getLog().debug(prettyPrintIterable.toString());
                    throw new MojoFailureException("Failed to validate JSON from file " + jsonDataFile + " against " + schemaFile + ": " + prettyPrintIterable);
                }
            }


            request.getLog().info("File: " + jsonDataFile + " - validated - Success");

        } catch (final Exception e) {
            request.getLog().error(e);
            throw new MojoFailureException(e.getMessage());
        }
    }

    /**
     * Returns all elements of {@code strictValidationMessages} which are not contained in {@code defaultValidationMessages}
     * Workaround because result Sets from validations are not real sets and don't support the retainAll() method.
     * @see com.networknt.schema.utils.SetView#retainAll(Collection)
     */
    private Collection<ValidationMessage> retainAllValidationMessages(Iterable<ValidationMessage> strictValidationMessages,
        Collection<ValidationMessage> defaultValidationMessages) {
        return StreamSupport.stream(strictValidationMessages.spliterator(), false)
                .filter(m -> !defaultValidationMessages.contains(m))
                    .collect(Collectors.toList());
    }

    private synchronized JsonNode getStrictSchemaNode() {
        if (strictSchemaNode == null) {
            strictSchemaNode = forceAdditionalProperties(schemaNode);
        }
        return strictSchemaNode;
    }

    private JsonNode forceAdditionalProperties(JsonNode schemaNode) {
        JsonNode clonedSchema = schemaNode.deepCopy();
        new JsonTreeWalker().walkTree(clonedSchema, (nodeName, objectNode) -> {
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
        return clonedSchema;
    }

    private JsonNode loadJsonNode(final String file) throws MojoFailureException {
        try {
            JsonNode node = new ObjectMapper().readTree(new File(file));
            request.getLog().debug("File: " + file + " - parsing - Success");
            return node;
        } catch (final IOException e) {
            request.getLog().error("File: " + file + " - parsing - Failure");
            throw new MojoFailureException("Failed to parse JSON from file " + file + " - " + e.getMessage(), e);
        }
    }

}
