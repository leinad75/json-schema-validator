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

import org.apache.maven.model.FileSet;

/**
 * Describes all validation inputs to the plugin.
 * <pre>
 * {@code
 * <validations>
 *     <validation>
 *       <jsonSchema>${project.basedir}/src/main/conf/file.schema</jsonSchema>
 *       <directory>${project.basedir}/src/main/conf</directory>
 *       <includes>
 *           <include>**\*.json</include>
 *       </includes>
 *       <excludes>
 *           <exclude>**\*.schema</exclude>
 *       </excludes>
 *     </validation>
 *     <!-- Additional validation rules -->
 *     <validation>
 *       <strict>true</strict>
 *       <jsonSchema>${project.basedir}/src/main/conf/file.schema</jsonSchema>
 *       <jsonFile>${project.basedir}/src/main/conf/file.json</jsonSchema>
 *     </validation>
 *  </validations>
 * }
 * </pre>
 *
 * @author Namrata Lele (nlele at groupon dot com)
 * @since 1.0
 */
public class Validation extends FileSet {
    private String jsonSchema;
    private String jsonFile;
    private boolean strict = false;
    private boolean metaValidation = true;

    private static final long serialVersionUID = 1L;

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(final String jsonSchemaInput) {
        jsonSchema = jsonSchemaInput;
    }

    public String getJsonFile() {
        return jsonFile;
    }

    public void setJsonFile(final String jsonFileInput) {
        jsonFile = jsonFileInput;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isMetaValidation() {
        return metaValidation;
    }

    public void setMetaValidation(boolean metaValidation) {
        this.metaValidation = metaValidation;
    }

    @Override
    public String toString() {
        return "Validation{" +
            "jsonSchema='" + jsonSchema + '\'' +
            ", jsonFile='" + jsonFile + '\'' +
            ", strict=" + strict +
            '}';
    }
}
