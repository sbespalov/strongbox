package org.carlspring.strongbox.nuget.filter;

import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.LogicalOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpFunctionContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpLeftContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpRightContext;
import org.carlspring.strongbox.providers.repository.RepositorySearchRequest;

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

        return super.visitFilter(ctx);
    }

    @Override
    public RepositorySearchRequest visitFilterExp(FilterExpContext ctx)
    {

        return super.visitFilterExp(ctx);
    }

    @Override
    public RepositorySearchRequest visitTokenExp(TokenExpContext ctx)
    {

        return super.visitTokenExp(ctx);
    }

    @Override
    public RepositorySearchRequest visitTokenExpRight(TokenExpRightContext ctx)
    {

        return super.visitTokenExpRight(ctx);
    }

    @Override
    public RepositorySearchRequest visitTokenExpLeft(TokenExpLeftContext ctx)
    {

        return super.visitTokenExpLeft(ctx);
    }

    @Override
    public RepositorySearchRequest visitTokenExpFunction(TokenExpFunctionContext ctx)
    {

        return super.visitTokenExpFunction(ctx);
    }

    @Override
    public RepositorySearchRequest visitFilterOp(FilterOpContext ctx)
    {

        return super.visitFilterOp(ctx);
    }

    @Override
    public RepositorySearchRequest visitLogicalOp(LogicalOpContext ctx)
    {

        return super.visitLogicalOp(ctx);
    }

}
