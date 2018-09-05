package com.capitalone.dashboard.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.capitalone.dashboard.auth.access.PermitAll;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.Http401AuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.capitalone.dashboard.auth.AuthProperties;
import com.capitalone.dashboard.auth.AuthenticationResultHandler;
import com.capitalone.dashboard.auth.apitoken.ApiTokenAuthenticationProvider;
import com.capitalone.dashboard.auth.apitoken.ApiTokenRequestFilter;
import com.capitalone.dashboard.auth.ldap.CustomUserDetailsContextMapper;
import com.capitalone.dashboard.auth.ldap.LdapLoginRequestFilter;
import com.capitalone.dashboard.auth.sso.SsoAuthenticationFilter;
import com.capitalone.dashboard.auth.standard.StandardLoginRequestFilter;
import com.capitalone.dashboard.auth.token.JwtAuthenticationFilter;
import com.capitalone.dashboard.model.AuthType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;
	
	@Autowired
	private AuthenticationResultHandler authenticationResultHandler;

	@Autowired
	private AuthenticationProvider standardAuthenticationProvider;

	@Autowired
	private ApiTokenAuthenticationProvider apiTokenAuthenticationProvider;
	
	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private ApplicationContext context;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.headers().cacheControl();
		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry interceptUrlRegistry = http.csrf().disable()
			.authorizeRequests().antMatchers("/appinfo").permitAll()
								.antMatchers("/registerUser").permitAll()
								.antMatchers("/findUser").permitAll()
								.antMatchers("/login**").permitAll()
								//TODO: sample call secured with ROLE_API
								//.antMatchers("/ping").hasAuthority("ROLE_API")
								.antMatchers(HttpMethod.GET, "/**").permitAll()
								
								// Temporary solution to allow jenkins plugin to send data to the api
							    //TODO: Secure with API Key
								.antMatchers(HttpMethod.POST, "/build").permitAll()
					            .antMatchers(HttpMethod.POST, "/deploy").permitAll()
								.antMatchers(HttpMethod.POST, "/performance").permitAll()
					            .antMatchers(HttpMethod.POST, "/artifact").permitAll()
					            .antMatchers(HttpMethod.POST, "/quality/test").permitAll()
					            .antMatchers(HttpMethod.POST, "/quality/static-analysis").permitAll()
                                //Temporary solution to allow Github webhook
                                .antMatchers(HttpMethod.POST, "/commit/github/v3").permitAll();

		addPermittedApis(interceptUrlRegistry);

		interceptUrlRegistry
								.anyRequest().authenticated()
									.and()
								.addFilterBefore(standardLoginRequestFilter(), UsernamePasswordAuthenticationFilter.class)
								.addFilterBefore(ssoAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
								.addFilterBefore(ldapLoginRequestFilter(), UsernamePasswordAuthenticationFilter.class)
								.addFilterBefore(apiTokenRequestFilter(), UsernamePasswordAuthenticationFilter.class)
								.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
								.exceptionHandling().authenticationEntryPoint(new Http401AuthenticationEntryPoint("Authorization"));
	}

	/**
	 * Add Permitted Api Endpoints annotated with PermitAll
	 * @param interceptUrlRegistry
	 */
	private void addPermittedApis(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry interceptUrlRegistry) {
		Map<String, RequestMappingHandlerMapping> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context,
			RequestMappingHandlerMapping.class, true, false);

		if (MapUtils.isEmpty(matchingBeans)) {
			return;
		}

		ArrayList<HandlerMapping> handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
		AnnotationAwareOrderComparator.sort(handlerMappings);

		RequestMappingHandlerMapping mappings = matchingBeans.get("requestMappingHandlerMapping");
		Map<RequestMappingInfo, HandlerMethod> handlerMethods = mappings.getHandlerMethods();

		for (RequestMappingInfo requestMappingInfo : handlerMethods.keySet()) {
			HandlerMethod handlerMethod = handlerMethods.get(requestMappingInfo);

			if (!handlerMethod.getMethod().isAnnotationPresent(PermitAll.class)) {
				continue;
			}

			RequestMethodsRequestCondition methods = requestMappingInfo.getMethodsCondition();

			HttpMethod requestMethod = HttpMethod.GET;
			if (methods.getMethods().isEmpty()) {
				requestMethod = HttpMethod.GET;
			} else if (methods.getMethods().contains(RequestMethod.POST)) {
				requestMethod = HttpMethod.POST;
			} else if (methods.getMethods().contains(RequestMethod.PUT)) {
				requestMethod = HttpMethod.PUT;
			}

			interceptUrlRegistry
				.antMatchers(requestMethod, requestMappingInfo.getPatternsCondition().getPatterns().iterator().next())
				.permitAll();

		}
	}
	
    @Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        List<AuthType> authenticationProviders = authProperties.getAuthenticationProviders();
        
        if(authenticationProviders.contains(AuthType.STANDARD)) {
            auth.authenticationProvider(standardAuthenticationProvider);
        }
		
        if(authenticationProviders.contains(AuthType.LDAP)) {
    		configureLdap(auth);
    		configureActiveDirectory(auth);
        }
		
		auth.authenticationProvider(apiTokenAuthenticationProvider);
	}

    private void configureActiveDirectory(AuthenticationManagerBuilder auth) {
        ActiveDirectoryLdapAuthenticationProvider adProvider = activeDirectoryLdapAuthenticationProvider();
        if(adProvider != null) auth.authenticationProvider(adProvider);
    }

    private void configureLdap(AuthenticationManagerBuilder auth) throws Exception {
        String ldapServerUrl = authProperties.getLdapServerUrl();
		String ldapUserDnPattern = authProperties.getLdapUserDnPattern();
		if (StringUtils.isNotBlank(ldapServerUrl) && StringUtils.isNotBlank(ldapUserDnPattern)) {
			auth.ldapAuthentication()
			.userDnPatterns(ldapUserDnPattern)
			.contextSource().url(ldapServerUrl);
		}
    }
	
	@Bean
	protected StandardLoginRequestFilter standardLoginRequestFilter() throws Exception {
		return new StandardLoginRequestFilter("/login", authenticationManager(), authenticationResultHandler);
	}
	
	@Bean
	protected SsoAuthenticationFilter ssoAuthenticationFilter() throws Exception {
		return new SsoAuthenticationFilter("/findUser", authenticationManager(), authenticationResultHandler);
	}
	
	@Bean
	protected LdapLoginRequestFilter ldapLoginRequestFilter() throws Exception {
		return new LdapLoginRequestFilter("/login/ldap", authenticationManager(), authenticationResultHandler);
	}

	@Bean
	protected ApiTokenRequestFilter apiTokenRequestFilter() throws Exception {
		return new ApiTokenRequestFilter("/**", authenticationManager(), authenticationResultHandler);
	}
	
    @Bean
    protected ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider() {
        if(StringUtils.isBlank(authProperties.getAdUrl())) return null;
        
        ActiveDirectoryLdapAuthenticationProvider provider = new ActiveDirectoryLdapAuthenticationProvider(authProperties.getAdDomain(), authProperties.getAdUrl(),
                authProperties.getAdRootDn());
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(true);
        provider.setUserDetailsContextMapper(new CustomUserDetailsContextMapper());
        return provider;
    }
	
}
