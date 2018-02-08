package org.carlspring.strongbox.artifact.criteria;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

public class Predicate
{

    private ArtifactEntryCriteria criteria;

    private BooleanOperator operator = BooleanOperator.AND;

    private List<Predicate> predicate = new ArrayList<>();

    public Predicate()
    {
        super();
    }

    public ArtifactEntryCriteria getCriteria()
    {
        return criteria;
    }

    public BooleanOperator getOperator()
    {
        return operator;
    }

    public List<Predicate> getPredicate()
    {
        return predicate;
    }

    public Predicate eq(ArtifactEntryCriteria c)
    {
        this.criteria = c;
        return this;
    }

    public Predicate or(Predicate p)
    {
        Assert.state(!BooleanOperator.AND.equals(this.operator), "Only disjunction allowed.");

        this.operator = BooleanOperator.OR;
        this.predicate.add(p);

        return this;
    }

    public Predicate and(Predicate p)
    {
        Assert.state(!BooleanOperator.OR.equals(this.operator), "Only conjunction allowed.");

        this.operator = BooleanOperator.AND;
        this.predicate.add(p);

        return this;
    }

    public static enum BooleanOperator
    {
        AND, OR
    }
}
