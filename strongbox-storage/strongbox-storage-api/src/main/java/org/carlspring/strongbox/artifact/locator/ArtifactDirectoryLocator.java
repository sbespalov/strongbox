package org.carlspring.strongbox.artifact.locator;

import org.carlspring.strongbox.artifact.locator.handlers.ArtifactDirectoryOperation;
import org.carlspring.strongbox.providers.io.RepositoryPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mtodorov
 */
public class ArtifactDirectoryLocator
{

    private static final Logger logger = LoggerFactory.getLogger(ArtifactDirectoryLocator.class);

    private ArtifactDirectoryOperation operation;

    /**
     * The basedir to start analyzing from. Define this only, if you need to pass null values for operation.storage and
     * operation.repository.
     */
    private RepositoryPath basedir;

    public void locateArtifactDirectories()
        throws IOException
    {
        long startTime = System.currentTimeMillis();

        RepositoryPath startingPath = getStartingPath();

        try (Stream<Path> pathStream = Files.walk(startingPath))
        {
            pathStream.filter(Files::isDirectory)
                      // Skip directories which start with a dot (like, for example: .index)
                      .filter(path -> !path.getFileName().toString().startsWith("."))
                      // Note: Sorting can be expensive:
                      .sorted()
                      .forEach(this::execute);
        }

        long endTime = System.currentTimeMillis();

        logger.debug("Executed (cache: " + -operation.getVisitedRootPaths().size() + ")" +
                     " visits in " + (endTime - startTime) + " ms.");

        getOperation().getVisitedRootPaths().clear();
    }

    public RepositoryPath getStartingPath()
    {
        // The root path
        RepositoryPath rootPath =
                basedir != null ? basedir : getOperation().getBasePath().getFileSystem().getRootDirectory();
        if (getOperation().getBasePath() != null)
        {
            rootPath = rootPath.resolve(getOperation().getBasePath().relativize());
        }
        rootPath = rootPath.normalize();

        logger.debug(String.format("ArtifactDirectoryLocator started in: path-[%s]", rootPath));

        return Files.isDirectory(rootPath) ? rootPath : rootPath.getParent();
    }

    public ArtifactDirectoryOperation getOperation()
    {
        return operation;
    }

    public void setOperation(ArtifactDirectoryOperation operation)
    {
        this.operation = operation;
    }

    public RepositoryPath getBasedir()
    {
        return basedir;
    }

    public void setBasedir(RepositoryPath basedir)
    {
        this.basedir = basedir;
    }

    void execute(Path path)
    {
        try
        {
            operation.execute((RepositoryPath) path);
        }
        catch (IOException e)
        {
            logger.error(String.format("Failed to execute operation [%s]", operation.getClass().getSimpleName()), e);
        }
    }
    
}
