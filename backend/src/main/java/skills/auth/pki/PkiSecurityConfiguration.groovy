package skills.auth.pki

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.context.NullSecurityContextRepository
import org.springframework.stereotype.Component
import skills.auth.SecurityConfiguration

@Slf4j
@Conditional(SecurityConfiguration.PkiAuth)
@Component
@Configuration
class PkiSecurityConfiguration extends WebSecurityConfigurerAdapter {

//    @Autowired
//    UserDetailsService userDetailsService

    @Bean
    @Conditional(SecurityConfiguration.PkiAuth)
    UserDetailsService pkiUserDetailsService() {
        new PkiUserDetailsService()
    }

    @Bean
    @Conditional(SecurityConfiguration.PkiAuth)
    PkiUserLookup pkiUserLookup() {
        new PkiUserLookup()
    }

    @Autowired
    SecurityConfiguration.PortalWebSecurityHelper portalWebSecurityHelper

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("Configuring PKI authorization mode")

        // Portal endpoints config
        portalWebSecurityHelper.configureHttpSecurity(http)
        http
                .x509()
                .subjectPrincipalRegex(/(.*)/)
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }

    @Override
    @Bean(name = 'defaultAuthManager')
    @Primary
    AuthenticationManager authenticationManagerBean() throws Exception {
        // provides the default AuthenticationManager as a Bean
        return super.authenticationManagerBean()
    }

    @Bean
    NullSecurityContextRepository httpSessionSecurityContextRepository() {
        return new NullSecurityContextRepository()
    }
}