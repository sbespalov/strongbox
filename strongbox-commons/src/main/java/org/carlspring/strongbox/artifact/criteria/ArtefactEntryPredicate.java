package org.carlspring.strongbox.artifact.criteria;

import java.util.List;

public class ArtefactEntryPredicate
{

    private ArtefactEntryCriteria criteria;

    private BooleanOperator operator = BooleanOperator.AND;

    private List<ArtefactEntryPredicate> predicate;

    public ArtefactEntryPredicate()
    {
        super();
    }

    public ArtefactEntryCriteria getCriteria()
    {
        return criteria;
    }

    public void setCriteria(ArtefactEntryCriteria criteria)
    {
        this.criteria = criteria;
    }

    public BooleanOperator getOperator()
    {
        return operator;
    }

    public void setOperator(BooleanOperator operator)
    {
        this.operator = operator;
    }

    public List<ArtefactEntryPredicate> getPredicate()
    {
        return predicate;
    }

    public void setPredicate(List<ArtefactEntryPredicate> predicate)
    {
        this.predicate = predicate;
    }

    public static enum BooleanOperator
    {
        AND, OR
    }

}
