package org.carlspring.strongbox.providers.io;

import java.nio.file.Path;

import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.domain.ArtifactEntryRead;

/**
 * @author sbespalov
 *
 */
public class RootRepositoryPath extends RepositoryPath
{

    public RootRepositoryPath(Path target,
                              RepositoryFileSystem fileSystem)
    {
        super(target, fileSystem);
    }

    public RepositoryPath resolve(ArtifactEntryRead artifactEntry)
    {
        RepositoryPath result = super.resolve(artifactEntry.getArtifactPath());
        result.artifactEntry = artifactEntry;
        return result;
    }

}
