/*
 * Copyright 2025 Kaur Palang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintegrationengine.tlsmanager.server.connectorconfig;

import com.mirth.connect.connectors.http.DefaultHttpConfiguration;
import com.mirth.connect.connectors.http.HttpDispatcher;
import com.mirth.connect.connectors.http.HttpDispatcherProperties;
import com.mirth.connect.connectors.http.HttpReceiver;
import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import com.mirth.connect.donkey.server.channel.Connector;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openintegrationengine.tlsmanager.server.CertificateService;
import org.openintegrationengine.tlsmanager.server.SocketFactoryService;
import org.openintegrationengine.tlsmanager.server.TLSServicePlugin;
import org.openintegrationengine.tlsmanager.shared.models.ClientAuthMode;
import org.openintegrationengine.tlsmanager.shared.properties.TLSListenerProperties;
import org.openintegrationengine.tlsmanager.shared.properties.TLSSenderProperties;

import static org.openintegrationengine.tlsmanager.shared.TLSPluginConstants.PKCS12;

@Slf4j
public class TLSHttpConfiguration extends DefaultHttpConfiguration {

    private final CertificateService certificateService;
    private final SocketFactoryService socketFactoryService;
    private final ConfigurationController configurationController;

    public TLSHttpConfiguration() {
        // This looks ugly, I know
        this(
            ControllerFactory.getFactory().createConfigurationController(),
            TLSServicePlugin.getPluginInstance().getCertificateService(),
            TLSServicePlugin.getPluginInstance().getSocketFactoryService()
        );
    }

    public TLSHttpConfiguration(
        ConfigurationController configurationController,
        CertificateService certificateService,
        SocketFactoryService socketFactoryService
    ) {
        this.configurationController = configurationController;
        this.certificateService = certificateService;
        this.socketFactoryService = socketFactoryService;
    }

    @Override
    public void configureConnectorDeploy(Connector connector) throws Exception {
        if (connector instanceof HttpDispatcher httpDispatcher) {
            configureSocketFactory(httpDispatcher);
        }
    }

    @Override
    public void configureDispatcher(HttpDispatcher connector, HttpDispatcherProperties connectorProperties) {}

    @Override
    public void configureSocketFactoryRegistry(ConnectorPluginProperties properties, RegistryBuilder<ConnectionSocketFactory> registry) {}

    @Override
    public void configureReceiver(HttpReceiver connector) throws Exception {
        var tlsConnectorProperties = getSenderProperties(TLSListenerProperties.class, connector);

        // If TLS manager is not enabled, delegate to OIE default
        if (tlsConnectorProperties == null || !tlsConnectorProperties.isTlsManagerEnabled()) {
            super.configureReceiver(connector);

        } else {
            var tlsContext = socketFactoryService.generateTLSContext(connector, tlsConnectorProperties);

            var httpConfig = new HttpConfiguration();
            httpConfig.addCustomizer(new SecureRequestCustomizer());
            httpConfig.setSendServerVersion(false);
            httpConfig.setSendXPoweredBy(false);

            var ssl = new SslContextFactory.Server();
            ssl.setIncludeProtocols(tlsContext.protocols());
            ssl.setIncludeCipherSuites(tlsContext.ciphers());

            ssl.setWantClientAuth(ClientAuthMode.REQUIRED == tlsConnectorProperties.getClientAuthMode());
            ssl.setNeedClientAuth(ClientAuthMode.REQUIRED == tlsConnectorProperties.getClientAuthMode());

            ssl.setKeyStore(tlsContext.keyStore());
            ssl.setKeyStoreType(PKCS12);
            ssl.setCertAlias(tlsConnectorProperties.getServerCertificateAlias());
            ssl.setKeyStorePassword("");

            var http11 = new HttpConnectionFactory(httpConfig);
            var tls = new SslConnectionFactory(ssl, HttpVersion.HTTP_1_1.asString());

            var listener = new ServerConnector(connector.getServer(), tls, http11);

            listener.setHost(connector.getHost());
            listener.setPort(connector.getPort());
            listener.setIdleTimeout(connector.getTimeout());

            connector.getServer().setConnectors(new org.eclipse.jetty.server.Connector[] { listener });
        }
    }

    private void configureSocketFactory(HttpDispatcher connector) {
        var tlsConnectorProperties = getSenderProperties(TLSSenderProperties.class, connector);

        if (tlsConnectorProperties != null && tlsConnectorProperties.isTlsManagerEnabled()) {
            var sslSocketFactory = socketFactoryService.getConnectorSocketFactory(connector, tlsConnectorProperties);
            if (sslSocketFactory != null) {
                // FIXME
                connector.getSocketFactoryRegistry().register("https", sslSocketFactory);
            }
        } else {
            try {
                super.configureSocketFactoryRegistry(null, connector.getSocketFactoryRegistry());
            } catch (Exception e) {
                log.error("Error creating non-TLS socket factory", e);
                throw new RuntimeException(e);
            }
        }
    }

    private <T> T getSenderProperties(Class<T> propertiesClass, Connector connector) {
        return connector.getConnectorProperties().getPluginProperties()
            .stream()
            .filter(propertiesClass::isInstance)
            .findFirst()
            .map(propertiesClass::cast)
            .orElse(null);
    }
}
