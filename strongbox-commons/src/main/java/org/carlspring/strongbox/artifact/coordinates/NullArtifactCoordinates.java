package org.carlspring.strongbox.artifact.coordinates;

import javax.persistence.Entity;

import java.util.Map;
import java.util.Optional;

/**
 * @author carlspring
 */
@Entity
public class NullArtifactCoordinates
        extends AbstractArtifactCoordinates<NullArtifactCoordinates, String>
{

    private static final String PATH = "path";


    public NullArtifactCoordinates()
    {
        resetCoordinates(PATH);
    }

    public NullArtifactCoordinates(String path)
    {
        this();
        setCoordinate(PATH, path);
    }

    @Override
    public String getId()
    {
        return getPath();
    }

    @Override
    public void setId(String id)
    {
        setCoordinate(PATH, id);
    }

    /**
     * WARNING: Unsurprisingly, this is null.
     * @return  null
     */
    @Override
    public String getVersion()
    {
        return null;
    }

    @Override
    public void setVersion(String version)
    {
    }

    @Override
    public String getNativeVersion()
    {
        return null;
    }

    @Override
    public Map<String, String> dropVersion()
    {
        return getCoordinates();
    }
    
    @Override
    public String toPath()
    {
        return Optional.ofNullable(getCoordinate(PATH)).orElse("");
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("NullArtifactCoordinates{");
        sb.append("objectId='").append(objectId).append('\'');
        sb.append(", uuid='").append(uuid).append('\'');
        sb.append(", entityVersion=").append(entityVersion);
        sb.append('}');
        return sb.toString();
    }
}
