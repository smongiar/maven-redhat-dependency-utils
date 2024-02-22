///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.16.1
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.9.0

package org.jboss.fuse.maven;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Parser {

    private static Path pomFile;
    private static String filterVersionMatcher;
    private static String filterPluginMatcher = "maven-plugin";
    private static boolean areMvnPluginsExcluded;

    public Parser(final String dependencyTreeFilePath, final String filterVersionMatcherCriteria) {
        pomFile = Paths.get(dependencyTreeFilePath);
        filterVersionMatcher = filterVersionMatcherCriteria;
    }

    public static void main(String[] args) throws IOException {
        final String dependencies = args[0];
        final String outputFile = args[1];
        String filter = args[2];
        final String includeMvnPlugins = args.length > 3 ? args[3] : "";
        areMvnPluginsExcluded = "--includeMvnPlugins".equals(includeMvnPlugins) ? false : true;

        final Parser parser = new Parser(dependencies, filter);

        Dependencies allDeps = parser.deserializeFromXML(dependencies);
        List<MavenArtifact> filteredArtifacts = getProductizedArtifacts(allDeps.getDependency());

        Dependencies filteredDeps = new Dependencies();
        filteredDeps.setDependency(filteredArtifacts);
        String xmlString = parser.serializeOnXML(filteredDeps, outputFile);
    }

    public Dependencies deserializeFromXML(String dependenciesFile) {
        Dependencies deserializedData = new Dependencies();
        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(
                    DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

            // read file and put contents into the string
            String readContent = new String(Files.readAllBytes(Paths.get(dependenciesFile)));

            // deserialize from the XML into a PhoneDetails object
           deserializedData = xmlMapper.readValue(readContent, Dependencies.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return deserializedData;
    }

    public String serializeOnXML(Dependencies dependencies, String outputFile) {
        String xml = null;
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Serialize the object to XML
		try {
			xml = xmlMapper.writeValueAsString(dependencies);
            Files.write(Paths.get(outputFile), xml.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return xml;
	}

    public static List<MavenArtifact> getProductizedArtifacts(List<MavenArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<MavenArtifact> productizedArtifacts = new ArrayList<>();
            for (MavenArtifact artifact : artifacts) {
                if (artifact.isProductised())
                    if (!(areMvnPluginsExcluded && artifact.isMavenPlugin())) {
                        productizedArtifacts.add(artifact);
                    }
            }
            return productizedArtifacts;
        }
    }


    @JacksonXmlRootElement(localName = "dependencies")
    static class Dependencies {
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<MavenArtifact> dependency;

        public Dependencies() {
        }

        public Dependencies(List<MavenArtifact> dependency) {
            this.dependency = dependency;
        }

        public List<MavenArtifact> getDependency() {
            return dependency;
        }

        public void setDependency(List<MavenArtifact> dependency) {
            this.dependency = dependency;
        }
    }

    @JacksonXmlRootElement(localName = "exclusions")
    static class Exclusions {
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Exclusion> exclusion;

        public Exclusions() {
        }

        public Exclusions(List<Exclusion> exclusion) {
            this.exclusion = exclusion;
        }

        public List<Exclusion> getExclusion() {
            return exclusion;
        }

        public void setExclusion(List<Exclusion> exclusion) {
            this.exclusion = exclusion;
        }

        @Override
        public String toString() {
            StringBuilder excListToString = new StringBuilder();
            excListToString.append("exclusions[");
            exclusion.forEach(excListToString::append);
            excListToString.append("]");
            return excListToString.toString();
        }
    }

    @JsonPropertyOrder({
            "groupId", "artifactId"
    })
    @JacksonXmlRootElement(localName = "exclusions")
    static class Exclusion {

        @JsonProperty("groupId")
        private String groupId;
        @JsonProperty("artifactId")
        private String artifactId;

        public Exclusion() {
        }

        public Exclusion(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        @Override
        public String toString() {
            return "exclusion(" + groupId + ":" + artifactId + ")";
        }
    }

    @JsonPropertyOrder({
            "groupId", "artifactId", "version", "classifier", "type", "scope", "exclusions"
    })
    @JacksonXmlRootElement(localName = "dependencies")
    static class MavenArtifact {

        @JsonProperty("groupId")
        private String groupId;
        @JsonProperty("artifactId")
        private String artifactId;
        @JsonProperty("version")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String version;
        @JsonProperty("classifier")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String classifier;
        @JsonProperty("type")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String type;
        @JsonProperty("scope")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String scope;
        @JsonProperty("exclusions")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Exclusions exclusions;

        public MavenArtifact() {
        }

        public MavenArtifact(String groupId, String artifactId, String version, String classifier, String type, String scope, Exclusions exclusions) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.classifier = classifier;
            this.type = type;
            this.scope = scope;
            this.exclusions = exclusions;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getClassifier() {
            return classifier;
        }

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public Exclusions getExclusions() {
            return exclusions;
        }

        public void setExclusions(Exclusions exclusions) {
            this.exclusions = exclusions;
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version + ":" + classifier + ":" + type + ":" + scope;
        }

         boolean isProductised() {
            return version.contains(filterVersionMatcher);
        }

        boolean isMavenPlugin() {
            return artifactId.contains(filterPluginMatcher);
        }
    }
}
