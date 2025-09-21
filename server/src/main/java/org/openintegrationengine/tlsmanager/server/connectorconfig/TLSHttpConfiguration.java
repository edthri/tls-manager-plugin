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
import com.mirth.connect.util.MirthSSLUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.DetectorConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openintegrationengine.tlsmanager.server.CertificateService;
import org.openintegrationengine.tlsmanager.server.SocketFactoryService;
import org.openintegrationengine.tlsmanager.server.TLSServicePlugin;
import org.openintegrationengine.tlsmanager.shared.properties.HttpConnectorProperties;

@Slf4j
public class TLSHttpConfiguration extends DefaultHttpConfiguration {

    private final CertificateService certificateService;
    private final SocketFactoryService socketFactoryService;
    private final ConfigurationController configurationController;

    public TLSHttpConfiguration() {
        this.configurationController = ControllerFactory.getFactory().createConfigurationController();

        var tlsServicePlugin = TLSServicePlugin.getPluginInstance();
        this.certificateService = tlsServicePlugin.getCertificateService();
        this.socketFactoryService = tlsServicePlugin.getSocketFactoryService();
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
    public void configureReceiver(HttpReceiver connector) {
        String[] enabledProtocols = MirthSSLUtil.getEnabledHttpsProtocols(configurationController.getHttpsServerProtocols());
        String[] cipherSuites = MirthSSLUtil.getEnabledHttpsProtocols(configurationController.getHttpsCipherSuites());

        var httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(true);
        httpConfig.setSendXPoweredBy(true);

        var ssl = new SslContextFactory.Server();
        ssl.setIncludeProtocols(enabledProtocols);
        ssl.setIncludeCipherSuites(cipherSuites);

        ssl.setKeyStore(certificateService.getTruststore());
        ssl.setKeyStorePassword("changeit");
        ssl.setKeyManagerPassword("changeit");

        var http11 = new HttpConnectionFactory(httpConfig);
        var tls = new SslConnectionFactory(ssl, HttpVersion.HTTP_1_1.asString());

        var detectorConnectionFactory = new DetectorConnectionFactory(tls);

        var listener = new ServerConnector(connector.getServer(), detectorConnectionFactory, http11);

        listener.setHost(connector.getHost());
        listener.setPort(connector.getPort());
        listener.setIdleTimeout(connector.getTimeout());
        connector.getServer().addConnector(listener);
    }

    private void configureSocketFactory(HttpDispatcher connector) {
        var oTlsPluginProperties = connector.getConnectorProperties().getPluginProperties()
            .stream()
            .filter(HttpConnectorProperties.class::isInstance)
            .findFirst();

        // TODO Fix repetition
        if (oTlsPluginProperties.isEmpty()) {
            try {
                super.configureSocketFactoryRegistry(null, connector.getSocketFactoryRegistry());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }

        var properties = (HttpConnectorProperties) oTlsPluginProperties.get();
        if (!properties.isTlsManagerEnabled()) {
            try {
                super.configureSocketFactoryRegistry(null, connector.getSocketFactoryRegistry());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }

        var sslSocketFactory = socketFactoryService.getChannelSocketFactory("kala", properties);
        connector.getSocketFactoryRegistry().register("https", sslSocketFactory);
    }
}
