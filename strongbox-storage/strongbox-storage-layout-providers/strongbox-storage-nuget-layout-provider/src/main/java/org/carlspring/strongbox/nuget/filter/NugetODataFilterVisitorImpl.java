package org.carlspring.strongbox.nuget.filter;

import java.util.Optional;

import javax.persistence.criteria.Predicate.BooleanOperator;

import org.antlr.v4.runtime.ParserRuleContext;
import org.carlspring.strongbox.artifact.ArtifactTag;
import org.carlspring.strongbox.artifact.criteria.ArtifactEntryCriteria;
import org.carlspring.strongbox.data.criteria.Predicate;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.LogicalOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpFunctionContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpLeftContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpRightContext;

/**
 * @author sbespalov
 *
 */
public class NugetODataFilterVisitorImpl extends NugetODataFilterBaseVisitor<Predicate<ArtifactEntryCriteria>>
{

    private Predicate<ArtifactEntryCriteria> root;

    public NugetODataFilterVisitorImpl(Predicate<ArtifactEntryCriteria> p)
    {
        super();
        this.root = p;
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitFilter(FilterContext ctx)
    {
        trace(ctx);
        return super.visitFilter(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitFilterExp(FilterExpContext ctx)
    {
        if (ctx.tokenExp() != null)
        {
            return visitTokenExp(ctx.tokenExp());
        }
        else if (ctx.vNesteedFilterExp != null)
        {
            return visitFilterExp(ctx.vNesteedFilterExp);
        }

        BooleanOperator booleanOperator = BooleanOperator.valueOf(ctx.vLogicalOp.getText().toUpperCase());

        Predicate<ArtifactEntryCriteria> p1 = visitFilterExp(ctx.vFilterExpLeft);
        Predicate<ArtifactEntryCriteria> p2 = visitFilterExp(ctx.vFilterExpRight);

        if (BooleanOperator.AND.equals(booleanOperator))
        {
            return p1.and(p2);
        }
        else if (BooleanOperator.OR.equals(booleanOperator))
        {
            return p1.or(p2);
        }

        return null;
    }

    private void trace(ParserRuleContext ctx)
    {
        System.out.println();
        System.out.println(ctx.getClass().getSimpleName());
        System.out.println(ctx.getText());
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitTokenExp(TokenExpContext ctx)
    {
        trace(ctx);
        if (ctx.TAG() != null)
        {
            ArtifactEntryCriteria c = new ArtifactEntryCriteria();
            c.getTagSet().add(ArtifactTag.LAST_VERSION);

            Predicate<ArtifactEntryCriteria> p = new Predicate<>();
            p.eq(c);

            return p;
        }

        Predicate<ArtifactEntryCriteria> p = visitTokenExpLeft(ctx.vTokenExpLeft);

        String attributeValue = ctx.vTokenExpRight.getText();
        p.getCriteria().getCoordinates().entrySet().stream().findFirst().map(e -> e.setValue(attributeValue));

        return p;
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitTokenExpRight(TokenExpRightContext ctx)
    {
        trace(ctx);
        return super.visitTokenExpRight(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitTokenExpLeft(TokenExpLeftContext ctx)
    {
        trace(ctx);

        ArtifactEntryCriteria c = new ArtifactEntryCriteria();
        Predicate<ArtifactEntryCriteria> p = new Predicate<ArtifactEntryCriteria>().eq(c);

        String attribute = ctx.ATTRIBUTE().getText();
        Optional.of(attribute).map(a -> c.getCoordinates().put(a, null));

        return p;
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitTokenExpFunction(TokenExpFunctionContext ctx)
    {
        trace(ctx);
        return super.visitTokenExpFunction(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitFilterOp(FilterOpContext ctx)
    {
        trace(ctx);
        return super.visitFilterOp(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitLogicalOp(LogicalOpContext ctx)
    {
        trace(ctx);
        return super.visitLogicalOp(ctx);
    }

}
