package au.org.ala.biocache.util;

import au.org.ala.biocache.dto.SpatialSearchRequestDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.*;

public class DataRoleCacheKeyGeneratorTest {

    private DataRoleCacheKeyGenerator generator;

    @Before
    public void setUp() {
        generator = new DataRoleCacheKeyGenerator();
        ReflectionTestUtils.setField(generator, "rolePrefix", "DATA_");
        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void login(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "x",
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
    }

    private static SpatialSearchRequestDTO request(String q) {
        SpatialSearchRequestDTO dto = new SpatialSearchRequestDTO();
        dto.setQ(q);
        return dto;
    }

    private Object key(Object... params) {
        return generator.generate(null, null, params);
    }

    @Test
    public void equalParamsGiveEqualKeys() {
        // separate instances, as each request builds new ones; arrays compared by content
        assertEquals(key(request("*:*"), "year", new String[]{"1", "2"}, false),
                key(request("*:*"), "year", new String[]{"1", "2"}, false));
    }

    @Test
    public void differentParamsGiveDifferentKeys() {
        assertNotEquals(key(request("*:*"), "year"), key(request("taxon_name:x"), "year"));
        assertNotEquals(key(request("*:*"), "year", null), key(request("*:*"), "year", null, false));
    }

    @Test
    public void anonymousAndRoleHolderGetDifferentKeys() {
        Object anonymous = key(request("*:*"), "year");
        login("DATA_abv");
        assertNotEquals(anonymous, key(request("*:*"), "year"));
    }

    @Test
    public void onlyDataRolesCountAndOrderDoesNot() {
        login("DATA_b", "DATA_a", "ROLE_USER");
        Object first = key(request("*:*"), "year");
        login("ROLE_ADMIN", "DATA_a", "DATA_b");
        assertEquals(first, key(request("*:*"), "year"));
    }
}
