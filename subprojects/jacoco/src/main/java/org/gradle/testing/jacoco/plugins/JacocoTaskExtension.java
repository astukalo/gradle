/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testing.jacoco.plugins;

import com.google.common.base.Joiner;
import org.gradle.api.Incubating;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.internal.jacoco.JacocoAgentJar;
import org.gradle.process.JavaForkOptions;
import org.gradle.util.GFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Extension for tasks that should run with a Jacoco agent to generate coverage execution data.
 */
@Incubating
public class JacocoTaskExtension {

    /**
     * The types of output that the agent can use for execution data.
     */
    enum Output {
        FILE,
        TCP_SERVER,
        TCP_CLIENT,
        NONE;

        /**
         * Gets type in format of agent argument.
         */
        String getAsArg() {
            return toString().toLowerCase().replaceAll("_", "");
        }
    }

    private final JacocoAgentJar agent;
    private final JavaForkOptions task;

    private boolean enabled = true;
    private File destinationFile;
    private boolean append = true;
    private List<String> includes = new ArrayList<String>();
    private List<String> excludes = new ArrayList<String>();
    private List<String> excludeClassLoaders = new ArrayList<String>();
    private boolean includeNoLocationClasses;
    private String sessionId;
    private boolean dumpOnExit = true;
    private Output output = Output.FILE;
    private String address;
    private Integer port;
    private File classDumpFile;
    private boolean jmx;

    /**
     * Creates a Jacoco task extension.
     *
     * @param agent the agent JAR to use for analysis
     * @param task the task we extend
     */
    public JacocoTaskExtension(JacocoAgentJar agent, JavaForkOptions task) {
        this.agent = agent;
        this.task = task;
    }

    /**
     * Whether or not the task should generate execution data. Defaults to {@code true}.
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * The path for the execution data to be written to.
     */
    public File getDestinationFile() {
        return destinationFile;
    }

    public void setDestinationFile(File destinationFile) {
        this.destinationFile = destinationFile;
    }

    /**
     * Whether or not data should be appended if the {@code destinationFile} already exists. Defaults to {@code true}.
     */
    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * List of class names that should be included in analysis. Names can use wildcards (* and ?). If left empty, all classes will be included. Defaults to an empty list.
     */
    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    /**
     * List of class names that should be excluded from analysis. Names can use wildcard (* and ?). Defaults to an empty list.
     */
    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    /**
     * List of classloader names that should be excluded from analysis. Names can use wildcards (* and ?). Defaults to an empty list.
     */
    public List<String> getExcludeClassLoaders() {
        return excludeClassLoaders;
    }

    public void setExcludeClassLoaders(List<String> excludeClassLoaders) {
        this.excludeClassLoaders = excludeClassLoaders;
    }

    /**
     * Whether or not classes without source location should be instrumented. Defaults to {@code false}.
     *
     * This property is only taken into account if the used JaCoCo version supports this option (JaCoCo version >= 0.7.6)
     */
    public boolean isIncludeNoLocationClasses() {
        return includeNoLocationClasses;
    }

    public void setIncludeNoLocationClasses(boolean includeNoLocationClasses) {
        this.includeNoLocationClasses = includeNoLocationClasses;
    }

    /**
     * An identifier for the session written to the execution data. Defaults to an auto-generated identifier.
     */
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Whether or not to dump the coverage data at VM shutdown. Defaults to {@code true}.
     */
    public boolean isDumpOnExit() {
        return dumpOnExit;
    }

    public void setDumpOnExit(boolean dumpOnExit) {
        this.dumpOnExit = dumpOnExit;
    }

    /**
     * The type of output to generate. Defaults to {@link Output#FILE}.
     */
    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    /**
     * IP address or hostname to use with {@link Output#TCP_SERVER} or {@link Output#TCP_CLIENT}. Defaults to localhost.
     */
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Port to bind to for {@link Output#TCP_SERVER} or {@link Output#TCP_CLIENT}. Defaults to 6300.
     */
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Path to dump all class files the agent sees are dumped to. Defaults to no dumps.
     */
    public File getClassDumpFile() {
        return classDumpFile;
    }

    public void setClassDumpFile(File classDumpFile) {
        this.classDumpFile = classDumpFile;
    }

    /**
     * Whether or not to expose functionality via JMX under {@code org.jacoco:type=Runtime}. Defaults to {@code false}.
     *
     * The configuration of the jmx property is only taken into account if the used JaCoCo version supports this option (JaCoCo version >= 0.6.2)
     */
    public boolean isJmx() {
        return jmx;
    }

    public void setJmx(boolean jmx) {
        this.jmx = jmx;
    }

    /**
     * Gets all properties in the format expected of the agent JVM argument.
     *
     * @return state of extension in a JVM argument
     */
    public String getAsJvmArg() {
        StringBuilder builder = new StringBuilder();
        ArgumentAppender argument = new ArgumentAppender(builder, task.getWorkingDir());
        ConventionValueSupplier convention = new ConventionValueSupplier(this);
        builder.append("-javaagent:");
        builder.append(GFileUtils.relativePath(task.getWorkingDir(), agent.getJar()));
        builder.append('=');
        argument.append("destfile", convention.get(destinationFile, "destinationFile"));
        argument.append("append", convention.get(append, "append"));
        argument.append("includes", convention.get(includes, "includes"));
        argument.append("excludes", convention.get(excludes, "excludes"));
        argument.append("exclclassloader", convention.get(excludeClassLoaders, "excludeClassLoaders"));
        if (agent.supportsInclNoLocationClasses()) {
            argument.append("inclnolocationclasses", convention.get(includeNoLocationClasses, "includeNoLocationClasses"));
        }
        argument.append("sessionid", convention.get(sessionId, "sessionId"));
        argument.append("dumponexit", convention.get(dumpOnExit, "dumpOnExit"));
        argument.append("output", convention.get(output, "output").getAsArg());
        argument.append("address", convention.get(address, "address"));
        argument.append("port", convention.get(port, "port"));
        argument.append("classdumpdir", classDumpFile);

        if (agent.supportsJmx()) {
            argument.append("jmx", convention.get(jmx, "jmx"));
        }

        return builder.toString();
    }

    private static class ConventionValueSupplier {

        private final ConventionMapping mapping;

        public ConventionValueSupplier(Object delegate) {
            this.mapping = delegate instanceof IConventionAware
                ? ((IConventionAware) delegate).getConventionMapping()
                : null;
        }

        public <T> T get(T actualValue, String propertyName) {
            if (mapping != null) {
                return mapping.getConventionValue(actualValue, propertyName, false);
            }
            // For unit tests
            return actualValue;
        }
    }

    private static class ArgumentAppender {

        private final StringBuilder builder;
        private final File workingDirectory;
        private boolean anyArgs;

        public ArgumentAppender(StringBuilder builder, File workingDirectory) {
            this.builder = builder;
            this.workingDirectory = workingDirectory;
        }

        public void append(String name, Object value) {
            if (value != null && (!(value instanceof Collection) || !((Collection) value).isEmpty())) {
                if (anyArgs) {
                    builder.append(',');
                }
                builder.append(name).append('=');
                if (value instanceof Collection) {
                    builder.append(Joiner.on(':').join((Collection) value));
                } else if (value instanceof File) {
                    builder.append(GFileUtils.relativePath(workingDirectory, (File) value));
                } else {
                    builder.append(value);
                }
                anyArgs = true;
            }
        }
    }
}
