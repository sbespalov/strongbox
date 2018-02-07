package org.carlspring.strongbox.artifact.criteria;

import java.util.Optional;

public class ArtefactEntrySelector
{

    private Projection projection;

    private ArtefactEntryPredicate predicate;

    public ArtefactEntryPredicate getPredicate()
    {
        return predicate;
    }

    public void setPredicate(ArtefactEntryPredicate predicate)
    {
        this.predicate = predicate;
    }

    public ArtefactEntrySelector eq(ArtefactEntryCriteria c)
    {
        ArtefactEntryPredicate predicate = new ArtefactEntryPredicate(c);
        this.predicate = Optional.ofNullable(this.predicate).map(p -> p.and(p)).orElse(predicate);
        return this;
    }
}
