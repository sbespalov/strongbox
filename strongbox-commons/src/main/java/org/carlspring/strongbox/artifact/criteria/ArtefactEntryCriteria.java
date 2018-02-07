package org.carlspring.strongbox.artifact.criteria;

import java.util.Map;
import java.util.Set;

import org.carlspring.strongbox.artifact.ArtifactTag;

public class ArtefactEntryCriteria
{

    private String storageId;
    private String repositoryId;
    private Map<String, String> coordinates;
    private Set<ArtifactTag> tagSet;

    public String getStorageId()
    {
        return storageId;
    }

    public void setStorageId(String storageId)
    {
        this.storageId = storageId;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public Map<String, String> getCoordinates()
    {
        return coordinates;
    }

    public void setCoordinates(Map<String, String> coordinates)
    {
        this.coordinates = coordinates;
    }

    public Set<ArtifactTag> getTagSet()
    {
        return tagSet;
    }

    public void setTagSet(Set<ArtifactTag> tagSet)
    {
        this.tagSet = tagSet;
    }

}
