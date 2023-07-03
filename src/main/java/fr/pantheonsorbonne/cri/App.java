package fr.pantheonsorbonne.cri;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import fr.pantheonsorbonne.cri.app.Agent;
import fr.pantheonsorbonne.cri.configuration.model.AppConfiguration;
import fr.pantheonsorbonne.cri.configuration.model.GeneralConfiguration;
import fr.pantheonsorbonne.cri.configuration.model.collectors.GitHubIssueCollectorConfig;
import fr.pantheonsorbonne.cri.configuration.model.collectors.IssueCollectorsConfig;
import fr.pantheonsorbonne.cri.configuration.model.differ.DifferAlgorithmConfig;
import fr.pantheonsorbonne.cri.configuration.model.differ.DifferConfig;
import fr.pantheonsorbonne.cri.configuration.model.publisher.FilePublisherConfig;
import fr.pantheonsorbonne.cri.configuration.model.publisher.PublisherConfig;
import fr.pantheonsorbonne.cri.configuration.variables.DiffAlgorithm;
import fr.pantheonsorbonne.cri.scm.issues.github.GitHubIssueCollector;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jline.reader.*;
import org.jline.reader.impl.*;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.*;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Terminal;

import java.io.*;
import java.util.*;

public class App {

    public static void main(String[] args) throws IOException, XmlPullParserException {

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = pomReader.read(new FileReader("pom.xml"));

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File("./.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        GeneralConfiguration configuration = new GeneralConfiguration();
        configuration.inheritedModules=Collections.emptyList();
        AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setDiffAlgorithmName("ginstructions");
        appConfiguration.setInstrumentedPackage(model.getGroupId());
        appConfiguration.setIssueCollectorName("dummy");
        appConfiguration.setPublisherName("console1");
        appConfiguration.setSourceRootDirs(Lists.asList("src/main/java", new String[0]));
        appConfiguration.setCoverageFolder("");
        appConfiguration.setIdeToolExportPath("dextorm-vscode-export.json");
        appConfiguration.setUseCache(true);
        configuration.setApp(appConfiguration);
        DifferConfig differConfig = new DifferConfig();
        DifferAlgorithmConfig differAlgorithmConfig = new DifferAlgorithmConfig();
        differAlgorithmConfig.setDiffAlgorithm(DiffAlgorithm.GUMTREE);
        differAlgorithmConfig.setInstructions(true);
        differAlgorithmConfig.setMethods(false);
        configuration.differs = Map.of("ginstructions", differAlgorithmConfig);
        IssueCollectorsConfig issueCollectorsConfig = new IssueCollectorsConfig();
        configuration.setIssueCollectors(issueCollectorsConfig);
        GitHubIssueCollectorConfig gitHubIssueCollectorConfig = new GitHubIssueCollectorConfig();
        issueCollectorsConfig.setGithub(Map.of("dummy", gitHubIssueCollectorConfig));
        String url = repository.getConfig().getString("remote", "origin", "url");
        if (url.contains("git@github.com:")) {
            String origin = url.substring(15);
            origin = origin.substring(0, origin.length() - 4);
            gitHubIssueCollectorConfig.setGitHubRepoName(origin);
        } else if (url.contains("https://github.com/")) {
            String origin = url.substring(19);
            origin = origin.substring(0, origin.length() - 4);
            gitHubIssueCollectorConfig.setGitHubRepoName(origin);
        }
        gitHubIssueCollectorConfig.setRepoAddress("https://github.com/"+gitHubIssueCollectorConfig.getGitHubRepoName()+".git");
        PublisherConfig publisherConfig = new PublisherConfig();

        FilePublisherConfig filePublisherConfig = new FilePublisherConfig();
        filePublisherConfig.setFilePath("stdout");
        publisherConfig.filePublishers = Map.of("console1", filePublisherConfig);
        configuration.setPublishers(publisherConfig);

        FileWriter sw = new FileWriter("dextorm.yaml");
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        om.writeValue(sw,configuration);

        Agent.main("dextorm.yaml");


    }
}