package org.openintegrationengine.tlsmanager.shared.models;

import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.shared.PersistenceMode;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import static org.openintegrationengine.tlsmanager.shared.TLSPluginConstants.ENV_PERSISTENCE_FS_KEYSTOREPASS;
import static org.openintegrationengine.tlsmanager.shared.TLSPluginConstants.ENV_PERSISTENCE_FS_KEYSTOREPATH;
import static org.openintegrationengine.tlsmanager.shared.TLSPluginConstants.ENV_PERSISTENCE_FS_TRUSTSTOREPASS;
import static org.openintegrationengine.tlsmanager.shared.TLSPluginConstants.ENV_PERSISTENCE_FS_TRUSTSTOREPATH;

@Slf4j
public record TLSPluginConfiguration(
    PersistenceMode persistenceMode,
    String truststorePath,
    String truststorePassword,
    String keystorePath,
    String keystorePassword
) {
    public static TLSPluginConfiguration fromEnv() {
        var conf = new TLSPluginConfiguration(
            getPersistenceMode(),
            readKeyFromEnv(ENV_PERSISTENCE_FS_TRUSTSTOREPATH, false),
            readKeyFromEnv(ENV_PERSISTENCE_FS_TRUSTSTOREPASS, false),
            readKeyFromEnv(ENV_PERSISTENCE_FS_KEYSTOREPATH, false),
            readKeyFromEnv(ENV_PERSISTENCE_FS_KEYSTOREPASS, false)
        );

        return conf;
    }

    private static PersistenceMode getPersistenceMode() {
        var persistenceModeFromEnv = readKeyFromEnv(TLSPluginConstants.ENV_PERSISTENCE_BACKEND, true);

        var persistenceMode = PersistenceMode.valueOf(persistenceModeFromEnv.toUpperCase());

        log.info("Using persistence mode {}", persistenceMode);

        return persistenceMode;
    }

    private static String readKeyFromEnv(String key, boolean isRequired) {
        var keyFromEnv = System.getenv(key);
        if (keyFromEnv == null && isRequired) {
            throw new IllegalStateException("Env key (%s) is not set".formatted(keyFromEnv));
        }

        return keyFromEnv;
    }
}
