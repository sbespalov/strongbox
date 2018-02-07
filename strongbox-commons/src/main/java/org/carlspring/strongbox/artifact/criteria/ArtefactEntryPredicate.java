package org.carlspring.strongbox.artifact.criteria;

public class ArtefactEntryPredicate
{

    private ArtefactEntrySelector selector;

    private BooleanOperator operator;

    private ArtefactEntryPredicate predicate;

    public ArtefactEntrySelector getSelector()
    {
        return selector;
    }

    public void setSelector(ArtefactEntrySelector selector)
    {
        this.selector = selector;
    }

    public BooleanOperator getOperator()
    {
        return operator;
    }

    public void setOperator(BooleanOperator operator)
    {
        this.operator = operator;
    }

    public ArtefactEntryPredicate getPredicate()
    {
        return predicate;
    }

    public void setPredicate(ArtefactEntryPredicate predicate)
    {
        this.predicate = predicate;
    }

    public static enum BooleanOperator
    {
        AND, OR
    }

}
