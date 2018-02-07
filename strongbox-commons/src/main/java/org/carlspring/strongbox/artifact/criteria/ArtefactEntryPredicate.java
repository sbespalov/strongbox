package org.carlspring.strongbox.artifact.criteria;

public class ArtefactEntryPredicate
{

    private ArtefactEntryCriteria criteria;

    private BooleanOperator operator = BooleanOperator.AND;

    private ArtefactEntryPredicate predicate;

    public ArtefactEntryPredicate(ArtefactEntryCriteria criteria)
    {
        super();
        this.criteria = criteria;
    }

    public ArtefactEntryCriteria getCriteria()
    {
        return criteria;
    }

    public BooleanOperator getOperator()
    {
        return operator;
    }

    public ArtefactEntryPredicate getPredicate()
    {
        return predicate;
    }

    public ArtefactEntryPredicate and(ArtefactEntryPredicate p)
    {
        this.operator = BooleanOperator.AND;
        this.predicate = p;
        return p;
    }

    public ArtefactEntryPredicate or(ArtefactEntryPredicate p)
    {
        this.operator = BooleanOperator.OR;
        this.predicate = p;
        return p;
    }

    public static enum BooleanOperator
    {
        AND, OR
    }

}
