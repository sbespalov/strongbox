package org.carlspring.strongbox.nuget.filter;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.carlspring.strongbox.artifact.criteria.ArtifactEntryCriteria;
import org.carlspring.strongbox.data.criteria.Predicate;
import org.carlspring.strongbox.data.criteria.Selector;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.junit.Test;

// @RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration(classes = NugetLayoutProviderTestConfig.class)
public class NugetFilterODataParserTestCase
// extends TestCaseWithNugetPackageGeneration
{

    @Test
    public void testParseFilter()
        throws Exception
    {
        Selector<ArtifactEntryCriteria> selector = new Selector<>(ArtifactEntry.class);
        Predicate<ArtifactEntryCriteria> predicate = selector.where();

        CodePointCharStream is = CharStreams.fromString("tolower(Id) eq 'org.carlspring.strongbox.nuget.test.lastversion' and IsLatestVersion and Version eq '1.0.0'");
        NugetODataFilterLexer lexer = new NugetODataFilterLexer(is);
        CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
        NugetODataFilterParser parser = new NugetODataFilterParser(commonTokenStream);

        NugetODataFilterParser.FilterContext fileContext = parser.filter();
        NugetODataFilterVisitor<Predicate<ArtifactEntryCriteria>> visitor = new NugetODataFilterVisitorImpl(predicate);
        Predicate<ArtifactEntryCriteria> result = visitor.visitFilter(fileContext);
        
        System.out.println(result);
    }

}
