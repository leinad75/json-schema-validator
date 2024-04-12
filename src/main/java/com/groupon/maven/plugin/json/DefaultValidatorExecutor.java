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
package com.groupon.maven.plugin.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupon.maven.plugin.json.util.FileUtils;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaId;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Default implementation for the ValidatorExecutor.
 *
 * @since 1.0
 */
//@Component(role = ValidatorExecutor.class, hint = "default", instantiationStrategy = "per-lookup")
@Mojo(name = "json-schema-validator")
public class DefaultValidatorExecutor implements ValidatorExecutor {

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
        if (!StringUtils.isEmpty(validation.getJsonSchema())) {
            schemaFile = validation.getJsonSchema();
        }
        List<Exception> exceptions = new ArrayList<>();
        for (final String jsonFile : jsonFiles) {
            try {
                if (schemaFile != null) {
                    validateAgainstSchema(jsonFile, schemaFile);
                } else {
                    loadJsonNode(jsonFile);
                }
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


    private void validateAgainstSchema(final String jsonDataFile, final String schemaFile) throws MojoExecutionException, MojoFailureException {
        request.getLog().debug("File: " + jsonDataFile + " - validating against " + schemaFile);

        try {
            JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V7, builder ->
                builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://localhost", "classpath:"))
            );
            SchemaValidatorsConfig config = new SchemaValidatorsConfig();
            config.setPathType(PathType.JSON_POINTER);

            JsonNode schemaNode = loadJsonNode(schemaFile);

            // validate schema against meta schema
            JsonSchema metaSchema = jsonSchemaFactory.getSchema(SchemaLocation.of(SchemaId.V7), config);
            Set<ValidationMessage> schemaValidationMessages = metaSchema.validate(schemaNode);
            if (!schemaValidationMessages.isEmpty()) {
                request.getLog().debug(schemaValidationMessages.toString());
                throw new MojoFailureException("Illegal schema " + schemaFile + ", not a valid schema " + SchemaId.V7);
            }

            // validate test file against schema
            JsonNode testFileJsonNode = loadJsonNode(jsonDataFile);

            JsonSchema schema = jsonSchemaFactory.getSchema(schemaNode, config);
            Set<ValidationMessage> validationMessages = schema.validate(testFileJsonNode);

            if (!validationMessages.isEmpty()) {
                request.getLog().debug(validationMessages.toString());
                throw new MojoFailureException("Failed to validate JSON from file " + jsonDataFile + " against " + schemaFile + ": " + validationMessages);
            }
            request.getLog().info("File: " + jsonDataFile + " - validated - Success");

        } catch (final Exception e) {
            request.getLog().error(e);
            throw new MojoFailureException(e.getMessage());
        }
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

    // NOTE: Package private for testing.
    /* package private */
    /*
    static void configureInputLocator(final MavenProject project, final ResourceManager inputLocator) {
        inputLocator.setOutputDirectory(new File(project.getBuild().getDirectory()));

        MavenProject parent = project;
        while (parent != null && parent.getFile() != null) {
            final File dir = parent.getFile().getParentFile();
            inputLocator.addSearchPath(FileResourceLoader.ID, dir.getAbsolutePath());
            parent = parent.getParent();
        }
        inputLocator.addSearchPath("url", "");
    }*/
}
