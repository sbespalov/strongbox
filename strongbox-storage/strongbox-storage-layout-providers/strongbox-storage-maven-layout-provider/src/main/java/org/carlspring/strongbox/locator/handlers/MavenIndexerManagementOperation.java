package org.carlspring.strongbox.locator.handlers;

import static org.carlspring.strongbox.util.IndexContextHelper.getContextId;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.carlspring.maven.commons.DetachedArtifact;
import org.carlspring.maven.commons.util.ArtifactUtils;
import org.carlspring.strongbox.io.ArtifactFilenameFilter;
import org.carlspring.strongbox.io.RepositoryPath;
import org.carlspring.strongbox.storage.indexing.IndexTypeEnum;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexManager;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexer;
import org.carlspring.strongbox.storage.metadata.VersionCollectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kate Novik.
 */
public class MavenIndexerManagementOperation
        extends AbstractMavenArtifactLocatorOperation
{

    private static final Logger logger = LoggerFactory.getLogger(MavenIndexerManagementOperation.class);

    private RepositoryIndexManager repositoryIndexManager;


    public MavenIndexerManagementOperation(RepositoryIndexManager repositoryIndexManager)
    {
        this.repositoryIndexManager = repositoryIndexManager;
    }

    @Override
    public void executeOperation(VersionCollectionRequest request,
                                 RepositoryPath artifactPath,
                                 List<RepositoryPath> versionDirectories)
    {
        String repositoryId = getRepository().getId();
        String storageId = getStorage().getId();

        String contextId = getContextId(storageId, repositoryId, IndexTypeEnum.LOCAL.getType());
        RepositoryIndexer indexer = repositoryIndexManager.getRepositoryIndexer(contextId);

        if (indexer == null)
        {
            return;
        }
        for (RepositoryPath versionDirectory : versionDirectories)
        {
            // We're using System.out.println() here for clarity and due to the length of the lines
            // System.out.println(" " + versionDirectory.getAbsolutePath());

            String artifactVersionDirectory = versionDirectory.getPath();
            String artifactVersionDirectoryRelative = artifactVersionDirectory.substring(getStorage().getRepository(repositoryId)
                                                                                                     .getBasedir()
                                                                                                     .length()
                    + 1,
                                                                                         artifactVersionDirectory.length());

            String[] artifactCoordinateElements = artifactVersionDirectoryRelative.split("/");
            StringBuilder groupId = new StringBuilder();
            for (int i = 0; i < artifactCoordinateElements.length - 2; i++)
            {
                String element = artifactCoordinateElements[i];
                groupId.append((groupId.length() == 0) ? element : "." + element);
            }

            String artifactId = artifactCoordinateElements[artifactCoordinateElements.length - 2];
            String version = artifactCoordinateElements[artifactCoordinateElements.length - 1];

            DetachedArtifact artifact = (DetachedArtifact) ArtifactUtils.getArtifactFromGAVTC(groupId + ":" +
                    artifactId + ":" +
                    version);

            // TODO: @Sergey:
            // TODO: Could you please replace this with a fully Path-based implementation?
            Files.walk(versionDirectory)
                 .filter(Files::isRegularFile)
                 .forEach(f -> {
                     String fileName = f.getFileName().toString();
                     String extension = fileName.substring(fileName.lastIndexOf('.') + 1,
                                                           fileName.length());

                     artifact.setFile(f.toFile());

                     // TODO: This is not quite right at the moment...
                     // TODO: SB-778: Figure out artifact extensions using Apache Tika
                     // TODO: Implement SB-778 here.
                     artifact.setType(extension);

                     try
                     {
                         indexer.addArtifactToIndex(repositoryId, f.toFile(), artifact);
                     }
                     catch (IOException e)
                     {
                         logger.error("Failed to add artifact to index " + artifactPath + "!", e);
                     }
                 });

        }
    }

    public RepositoryIndexManager getRepositoryIndexManager()
    {
        return repositoryIndexManager;
    }

    public void setRepositoryIndexManager(RepositoryIndexManager repositoryIndexManager)
    {
        this.repositoryIndexManager = repositoryIndexManager;
    }

}
