package com.github.msemys.esjc.ssl;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

/**
 * SSL settings
 */
public class SslSettings {
    private static final SslSettings TRUST_ALL_CERTIFICATES = new SslSettings(true, null, false);
    private static final SslSettings NO_SSL = new SslSettings(false, null, false);

    /**
     * Whether or not the connection is encrypted using SSL.
     */
    public final boolean useSslConnection;

    /**
     * The Common Name (CN) identifies the fully qualified domain name associated with the certificate.
     */
    public final String certificateCommonName;

    /**
     * Whether or not to validate the server SSL certificate.
     */
    public final boolean validateServerCertificate;

    private SslSettings(boolean useSslConnection, String certificateCommonName, boolean validateServerCertificate) {
        this.useSslConnection = useSslConnection;
        this.certificateCommonName = certificateCommonName;
        this.validateServerCertificate = validateServerCertificate;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SslSettings{");
        sb.append("useSslConnection=").append(useSslConnection);
        sb.append(", certificateCommonName='").append(certificateCommonName).append('\'');
        sb.append(", validateServerCertificate=").append(validateServerCertificate);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a new SSL settings that enables connection encryption using SSL and
     * trusts an X.509 server certificate whose Common Name (CN) matches.
     *
     * @param certificateCommonName server certificate common name (CN)
     * @return SSL settings
     */
    public static SslSettings trustCertificateCN(String certificateCommonName) {
        checkArgument(!isNullOrEmpty(certificateCommonName), "certificateCommonName is null or empty");
        return new SslSettings(true, certificateCommonName, true);
    }

    /**
     * Creates a new SSL settings that enables connection encryption using SSL and
     * trusts all X.509 server certificates without any verification.
     *
     * @return SSL settings
     */
    public static SslSettings trustAllCertificates() {
        return TRUST_ALL_CERTIFICATES;
    }

    /**
     * Creates a new SSL settings that do not use connection encryption.
     *
     * @return SSL settings
     */
    public static SslSettings noSsl() {
        return NO_SSL;
    }

}
