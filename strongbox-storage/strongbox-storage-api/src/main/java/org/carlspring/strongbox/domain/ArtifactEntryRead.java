package org.carlspring.strongbox.domain;

import java.util.Date;

public interface ArtifactEntryRead
{

    String getObjectId();

    String getStorageId();

    String getRepositoryId();

    Long getSizeInBytes();

    Date getLastUpdated();

    Date getLastUsed();

    Date getCreated();

    Integer getDownloadCount();

    String getArtifactPath();
    
    ArtifactEntry getArtifactEntry();

}