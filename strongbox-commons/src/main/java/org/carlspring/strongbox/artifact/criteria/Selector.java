package org.carlspring.strongbox.artifact.criteria;

public class Selector
{

    private Projection projection = Projection.ROWS;

    private Predicate predicate;

    public Predicate getPredicate()
    {
        return predicate;
    }

    public Projection getProjection()
    {
        return projection;
    }

    public Selector select(Projection p)
    {
        this.projection = p;
        return this;
    }

    public Selector where(ArtifactEntryCriteria c)
    {
        this.predicate = new Predicate().eq(c);
        return this;
    }

    public Predicate eq(ArtifactEntryCriteria c)
    {
        return new Predicate().eq(c);
    }

    public Predicate and(Predicate p1,
                         Predicate p2)
    {
        return p1.and(p2);
    }

    public Predicate or(Predicate p1,
                        Predicate p2)
    {
        return p1.or(p2);
    }

}
