package au.org.ala.biocache.dao;

import au.org.ala.biocache.util.solr.FieldMappingUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.*;

/**
 * The RBAC filter must reach every Solr path, including streaming facet() expressions, which
 * carry their own q/fq.
 */
public class SolrIndexDAOImplRbacTest {

    private static final String ANON_FILTER = "(*:* NOT dynamicProperties_rbac:*) OR dynamicProperties_rbac:false";

    private SolrIndexDAOImpl dao;

    @Before
    public void setUp() {
        dao = new SolrIndexDAOImpl();
        ReflectionTestUtils.setField(dao, "fieldMappingUtil", new FieldMappingUtil());
        ReflectionTestUtils.setField(dao, "solrCollection", "biocache");
        ReflectionTestUtils.setField(dao, "rolePrefix", "DATA_");
        ReflectionTestUtils.setField(dao, "rbacEnabled", true);
        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String facetExpr(SolrQuery query) {
        ModifiableSolrParams params = ReflectionTestUtils.invokeMethod(dao, "buildFacetExpr", query, "species");
        return params.get("expr");
    }

    private String endemicExpr(SolrQuery subset, SolrQuery superset) {
        ModifiableSolrParams params = ReflectionTestUtils.invokeMethod(dao, "buildEndemicExpr", subset, superset);
        return params.get("expr");
    }

    private static SolrQuery facetQuery(String q) {
        SolrQuery query = new SolrQuery(q);
        query.addFacetField("species");
        query.setFacetLimit(10);
        return query;
    }

    @Test
    public void anonymousFacetExprIsFiltered() {
        String expr = facetExpr(facetQuery("*:*"));
        assertTrue(expr, expr.contains("fq=\"" + ANON_FILTER + "\""));
    }

    @Test
    public void roleHolderFacetExprAllowsTheirRoles() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "x",
                List.of(new SimpleGrantedAuthority("DATA_abv"), new SimpleGrantedAuthority("ROLE_USER"))));

        String expr = facetExpr(facetQuery("*:*"));
        assertTrue(expr, expr.contains("dynamicProperties_rbac_allowed:(abv)"));
        assertFalse(expr, expr.contains("ROLE_USER"));
    }

    @Test
    public void endemicExprFiltersSubsetAndSuperset() {
        String expr = endemicExpr(facetQuery("state:Vlaanderen"), new SolrQuery("*:*"));
        int first = expr.indexOf(ANON_FILTER);
        assertTrue(expr, first >= 0);
        assertTrue(expr, expr.indexOf(ANON_FILTER, first + 1) > first);
    }

    @Test
    public void noFilterWhenRbacDisabled() {
        ReflectionTestUtils.setField(dao, "rbacEnabled", false);
        assertFalse(facetExpr(facetQuery("*:*")).contains("dynamicProperties_rbac"));
        assertFalse(endemicExpr(facetQuery("*:*"), new SolrQuery("*:*")).contains("dynamicProperties_rbac"));
    }
}
