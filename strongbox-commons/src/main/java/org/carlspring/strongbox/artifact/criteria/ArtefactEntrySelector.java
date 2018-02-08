package org.carlspring.strongbox.artifact.criteria;

import org.carlspring.strongbox.artifact.criteria.ArtefactEntryPredicate.BooleanOperator;

public class ArtefactEntrySelector
{

    private Projection projection = Projection.ROWS;

    private ArtefactEntryPredicate predicate;

    public ArtefactEntryPredicate getPredicate()
    {
        return predicate;
    }

    public Projection getProjection()
    {
        return projection;
    }

    public void where(ArtefactEntryPredicate predicate)
    {
        this.predicate = predicate;
    }

    public ArtefactEntryPredicate eq(ArtefactEntryCriteria c)
    {
        ArtefactEntryPredicate result = new ArtefactEntryPredicate();
        result.setCriteria(c);
        return result;
    }

    public ArtefactEntryPredicate and(ArtefactEntryPredicate p1,
                                      ArtefactEntryPredicate p2)
    {
        return append(p1, p2, BooleanOperator.AND);
    }

    public ArtefactEntryPredicate or(ArtefactEntryPredicate p1,
                                     ArtefactEntryPredicate p2)
    {
        return append(p1, p2, BooleanOperator.OR);
    }

    private ArtefactEntryPredicate append(ArtefactEntryPredicate p1,
                                          ArtefactEntryPredicate p2,
                                          BooleanOperator o)
    {
        ArtefactEntryPredicate result = new ArtefactEntryPredicate();
        result.getPredicate().add(p1);
        result.getPredicate().add(p2);
        result.setOperator(o);
        return result;
    }
}
