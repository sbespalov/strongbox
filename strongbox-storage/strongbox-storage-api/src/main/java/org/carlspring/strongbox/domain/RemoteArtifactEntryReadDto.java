package org.carlspring.strongbox.domain;

public class RemoteArtifactEntryReadDto extends ArtifactEntryReadDto implements RemoteArtifactEntryRead
{

    private Boolean cached;

    RemoteArtifactEntryReadDto()
    {
    }

    public RemoteArtifactEntryReadDto(String path, RemoteArtifactEntry artifactEntry)
    {
        super(path, artifactEntry);

        this.cached = artifactEntry.getIsCached();
    }

    @Override
    public Boolean getIsCached()
    {
        return cached;
    }

}
