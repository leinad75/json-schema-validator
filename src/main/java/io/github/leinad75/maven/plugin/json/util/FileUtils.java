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
package io.github.leinad75.maven.plugin.json.util;

import io.github.leinad75.maven.plugin.json.Validation;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Utility class for file handling.
 *
 * @author Namrata Lele (nlele at groupon dot com)
 * @since 1.0
 */
public final class FileUtils {

    private FileUtils() { }

    public static List<String> getListOfFiles(final Validation validation) throws MojoExecutionException {
        final File directory = new File(validation.getDirectory());
        final String includes;
        if (validation.getIncludes().isEmpty()) {
            includes = "**/*";
        } else {
            includes = String.join(",", validation.getIncludes());
        }
        try {
            return org.codehaus.plexus.util.FileUtils.getFileNames(
                    directory,
                    includes,
                    String.join(",", validation.getExcludes()),
                    true);
        } catch (final IllegalStateException | IOException e) {
            throw new MojoExecutionException(
                    String.format(
                            "Exception while locating json input files; directory=%s includes=%s",
                            directory,
                            includes),
                    e);
        }
    }

}
