package com.github.msemys.esjc.ssl;

import java.io.File;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

/**
 * SSL settings
 */
public class SslSettings {
    private static final SslSettings TRUST_ALL_CERTIFICATES = new SslSettings(true, SslValidationMode.NONE, null, null);
    private static final SslSettings NO_SSL = new SslSettings(false, SslValidationMode.NONE, null, null);

    /**
     * Whether or not the connection is encrypted using SSL.
     */
    public final boolean useSslConnection;

    /**
     * The Common Name (CN) identifies the fully qualified domain name associated with the certificate.
     */
    public final String certificateCommonName;

    /**
     * The file containing the signing certificate (chain) to validate against.
     */
    public final File certificateFile;

    /**
     * The validation mode.
     */
    public final SslValidationMode validationMode;

    private SslSettings(boolean useSslConnection, SslValidationMode validationMode, String certificateCommonName, File certificateFile) {
        this.useSslConnection = useSslConnection;
        this.certificateCommonName = certificateCommonName;
        this.validationMode = validationMode;
        this.certificateFile = certificateFile;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SslSettings{");
        sb.append("useSslConnection=").append(useSslConnection);
        sb.append(", validationMode=").append(validationMode);
        sb.append(", certificateCommonName='").append(certificateCommonName).append('\'');
        sb.append(", certificateFile=").append(certificateFile);
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
        return new SslSettings(true, SslValidationMode.COMMON_NAME, certificateCommonName, null);
    }

    /**
     * Creates a new SSL settings that enables connection encryption using SSL and
     * trusts an X.509 server certificate which is trusted by the given certificate file.
     *
     * @param certificateFile server certificate file in PEM form.
     * @return SSL settings
     */
    public static SslSettings trustCertificate(File certificateFile) {
        checkArgument(certificateFile != null, "certificateFile is null");
        checkArgument(certificateFile.exists(), "certificateFile '" + certificateFile + "' does not exist");
        return new SslSettings(true, SslValidationMode.CERTIFICATE, null, certificateFile);
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
