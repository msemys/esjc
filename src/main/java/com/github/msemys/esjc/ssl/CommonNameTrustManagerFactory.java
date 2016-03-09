package com.github.msemys.esjc.ssl;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import io.netty.util.internal.EmptyArrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

public class CommonNameTrustManagerFactory extends SimpleTrustManagerFactory {
    private static final Logger logger = LoggerFactory.getLogger(CommonNameTrustManagerFactory.class);

    private final TrustManager tm = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            checkTrusted("client", chain);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            checkTrusted("server", chain);
        }

        private void checkTrusted(String type, X509Certificate[] chain) throws CertificateException {
            if (chain == null || chain.length == 0) {
                throw new CertificateException(type + " certificate not available");
            }

            final X509Certificate certificate = chain[0];

            logger.debug("Accepting a {} certificate: {}", type, certificate.getSubjectX500Principal().getName());

            try {
                certificate.checkValidity();
            } catch (Exception e) {
                throw new CertificateException(type + " certificate has expired");
            }

            try {
                LdapName names = new LdapName(certificate.getSubjectX500Principal().getName());

                for (Rdn name : names.getRdns()) {
                    if (name.getType().equalsIgnoreCase("CN")) {
                        if (!commonName.equals(name.getValue())) {
                            throw new CertificateException(type + " certificate common name (CN) mismatch");
                        } else {
                            break;
                        }
                    }
                }
            } catch (InvalidNameException e) {
                throw new CertificateException(type + " certificate not trusted", e);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return EmptyArrays.EMPTY_X509_CERTIFICATES;
        }
    };

    private final String commonName;

    public CommonNameTrustManagerFactory(String commonName) {
        checkArgument(!isNullOrEmpty(commonName), "commonName is null or empty");
        this.commonName = commonName;
    }

    @Override
    protected void engineInit(KeyStore keyStore) throws Exception {

    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {

    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[]{tm};
    }
}
