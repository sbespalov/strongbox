package org.carlspring.strongbox.domain;

import java.util.Date;

public class ArtifactEntryReadDto implements ArtifactEntryRead
{

    private String storageId;

    private String repositoryId;

    private Long sizeInBytes;

    private Date lastUpdated;

    private Date lastUsed;

    private Date created;

    private Integer downloadCount;

    private transient ArtifactEntry artifactEntry;

    private String artifactPath;

    private String objectId;
    
    ArtifactEntryReadDto()
    {
    }

    public ArtifactEntryReadDto(String path, ArtifactEntry artifactEntry)
    {
        this.artifactEntry = artifactEntry;

        storageId = artifactEntry.getStorageId();
        repositoryId = artifactEntry.getRepositoryId();
        sizeInBytes = artifactEntry.getSizeInBytes();
        lastUpdated = artifactEntry.getLastUpdated();
        lastUpdated = artifactEntry.getLastUpdated();
        created = artifactEntry.getCreated();
        downloadCount = artifactEntry.getDownloadCount();
        objectId = artifactEntry.getObjectId();

        artifactPath = artifactEntry.getArtifactPath();
    }

    @Override
    public String getObjectId()
    {
        return objectId;
    }

    @Override
    public String getStorageId()
    {
        return storageId;
    }

    @Override
    public String getRepositoryId()
    {
        return repositoryId;
    }

    @Override
    public Long getSizeInBytes()
    {
        return sizeInBytes;
    }

    @Override
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    @Override
    public Date getLastUsed()
    {
        return lastUsed;
    }

    @Override
    public Date getCreated()
    {
        return created;
    }

    @Override
    public Integer getDownloadCount()
    {
        return downloadCount;
    }

    @Override
    public String getArtifactPath()
    {
        return artifactPath;
    }

    public ArtifactEntry getArtifactEntry()
    {
        return artifactEntry;
    }

}
