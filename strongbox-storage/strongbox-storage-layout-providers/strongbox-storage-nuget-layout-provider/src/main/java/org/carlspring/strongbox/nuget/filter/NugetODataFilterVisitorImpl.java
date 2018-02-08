package org.carlspring.strongbox.nuget.filter;

import org.antlr.v4.runtime.ParserRuleContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.LogicalOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpFunctionContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpLeftContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpRightContext;
import org.carlspring.strongbox.providers.repository.RepositorySearchRequest;

import com.mchange.v2.sql.filter.SynchronizedFilterDataSource;

public class NugetODataFilterVisitorImpl extends NugetODataFilterBaseVisitor<RepositorySearchRequest>
{

    private RepositorySearchRequest repositorySearchRequest;

    public NugetODataFilterVisitorImpl(RepositorySearchRequest repositorySearchRequest)
    {
        super();
        this.repositorySearchRequest = repositorySearchRequest;
    }

    @Override
    public RepositorySearchRequest visitFilter(FilterContext ctx)
    {
        trace(ctx);
        return super.visitFilter(ctx);
    }

    @Override
    public RepositorySearchRequest visitFilterExp(FilterExpContext ctx)
    {
        trace(ctx);
        return super.visitFilterExp(ctx);
    }

    private void trace(ParserRuleContext ctx)
    {
        System.out.println();
        System.out.println(ctx.getClass().getSimpleName());
        System.out.println(ctx.getText());
    }

    @Override
    public RepositorySearchRequest visitTokenExp(TokenExpContext ctx)
    {
        trace(ctx);
        return super.visitTokenExp(ctx);
    }

    @Override
    public RepositorySearchRequest visitTokenExpRight(TokenExpRightContext ctx)
    {
        trace(ctx);
        return super.visitTokenExpRight(ctx);
    }

    @Override
    public RepositorySearchRequest visitTokenExpLeft(TokenExpLeftContext ctx)
    {
        trace(ctx);
        return super.visitTokenExpLeft(ctx);
    }

    @Override
    public RepositorySearchRequest visitTokenExpFunction(TokenExpFunctionContext ctx)
    {
        trace(ctx);
        return super.visitTokenExpFunction(ctx);
    }

    @Override
    public RepositorySearchRequest visitFilterOp(FilterOpContext ctx)
    {
        trace(ctx);
        return super.visitFilterOp(ctx);
    }

    @Override
    public RepositorySearchRequest visitLogicalOp(LogicalOpContext ctx)
    {
        trace(ctx);
        return super.visitLogicalOp(ctx);
    }

}
