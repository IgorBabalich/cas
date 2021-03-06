package org.apereo.cas.config;

import org.apereo.cas.CipherExecutor;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationMetaDataPopulator;
import org.apereo.cas.authentication.metadata.AuthenticationCredentialTypeMetaDataPopulator;
import org.apereo.cas.authentication.metadata.CacheCredentialsCipherExecutor;
import org.apereo.cas.authentication.metadata.CacheCredentialsMetaDataPopulator;
import org.apereo.cas.authentication.metadata.RememberMeAuthenticationMetaDataPopulator;
import org.apereo.cas.authentication.metadata.SuccessfulHandlerMetaDataPopulator;
import org.apereo.cas.configuration.CasConfigurationProperties;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is {@link CasCoreAuthenticationMetadataConfiguration}.
 *
 * @author Misagh Moayyed
 * @author Dmitriy Kopylenko
 * @since 5.1.0
 */
@Configuration("casCoreAuthenticationMetadataConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CasCoreAuthenticationMetadataConfiguration {
    @Autowired
    private CasConfigurationProperties casProperties;

    @ConditionalOnMissingBean(name = "successfulHandlerMetaDataPopulator")
    @Bean
    public AuthenticationMetaDataPopulator successfulHandlerMetaDataPopulator() {
        return new SuccessfulHandlerMetaDataPopulator();
    }

    @ConditionalOnMissingBean(name = "rememberMeAuthenticationMetaDataPopulator")
    @Bean
    public AuthenticationMetaDataPopulator rememberMeAuthenticationMetaDataPopulator() {
        return new RememberMeAuthenticationMetaDataPopulator();
    }

    @ConditionalOnMissingBean(name = "cacheCredentialsCipherExecutor")
    @Bean
    public CipherExecutor cacheCredentialsCipherExecutor() {
        val cp = casProperties.getClearpass();
        if (cp.isCacheCredential()) {
            val crypto = cp.getCrypto();
            if (crypto.isEnabled()) {
                return new CacheCredentialsCipherExecutor(crypto.getEncryption().getKey(),
                    crypto.getSigning().getKey(),
                    crypto.getAlg(),
                    crypto.getSigning().getKeySize(),
                    crypto.getEncryption().getKeySize());
            }
            LOGGER.warn("CAS is configured to capture and cache credentials via Clearpass yet crypto operations for the cached password are "
                + "turned off. Consider enabling the crypto configuration in CAS settings that allow the system to sign & encrypt the captured credential.");
        }
        return CipherExecutor.noOp();
    }

    @ConditionalOnMissingBean(name = "authenticationCredentialTypeMetaDataPopulator")
    @Bean
    public AuthenticationMetaDataPopulator authenticationCredentialTypeMetaDataPopulator() {
        return new AuthenticationCredentialTypeMetaDataPopulator();
    }

    @ConditionalOnMissingBean(name = "casCoreAuthenticationMetadataAuthenticationEventExecutionPlanConfigurer")
    @Bean
    public AuthenticationEventExecutionPlanConfigurer casCoreAuthenticationMetadataAuthenticationEventExecutionPlanConfigurer() {
        return plan -> {
            plan.registerAuthenticationMetadataPopulator(successfulHandlerMetaDataPopulator());
            plan.registerAuthenticationMetadataPopulator(rememberMeAuthenticationMetaDataPopulator());
            plan.registerAuthenticationMetadataPopulator(authenticationCredentialTypeMetaDataPopulator());

            val cp = casProperties.getClearpass();
            if (cp.isCacheCredential()) {
                LOGGER.warn("CAS is configured to capture and cache credentials via Clearpass. Sharing the user credential with other applications "
                    + "is generally NOT recommended, may lead to security vulnerabilities and MUST only be used as a last resort .");
                plan.registerAuthenticationMetadataPopulator(new CacheCredentialsMetaDataPopulator(cacheCredentialsCipherExecutor()));
            }
        };
    }
}
