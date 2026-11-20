package au.org.ala.biocache.util;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

/**
 * Cache key for results that depend on the RBAC filter: the default Spring key for the method
 * parameters (equals/hashCode, arrays compared by content) plus the user's data roles.
 */
@Component("dataRoleCacheKeyGenerator")
public class DataRoleCacheKeyGenerator implements KeyGenerator {

    @Value("${rbac.rolePrefix:}")
    private String rolePrefix;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var roles = "";
        if (auth != null) {
            roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(role -> role.startsWith(rolePrefix))
                    .map(role -> role.substring(rolePrefix.length()))
                    .sorted()
                    .collect(Collectors.joining(" "));
        }

        return new SimpleKey(SimpleKeyGenerator.generateKey(params), roles);
    }
}
