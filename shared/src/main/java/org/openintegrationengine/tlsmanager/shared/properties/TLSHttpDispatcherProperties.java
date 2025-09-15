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

package org.openintegrationengine.tlsmanager.shared.properties;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;

public class TLSHttpDispatcherProperties extends ConnectorProperties implements DestinationConnectorPropertiesInterface {

    private DestinationConnectorProperties destinationConnectorProperties;

    public TLSHttpDispatcherProperties() {
        destinationConnectorProperties = new DestinationConnectorProperties(true);
    }

    public TLSHttpDispatcherProperties(TLSHttpDispatcherProperties properties) {
        super(properties);
        destinationConnectorProperties = new DestinationConnectorProperties(true);
    }

    @Override
    public String getProtocol() {
        return "HTTP";
    }

    @Override
    public String getName() {
        return "HTTP Sender";
    }

    @Override
    public String toFormattedString() {
        return "";
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public DestinationConnectorProperties getDestinationConnectorProperties() {
        return destinationConnectorProperties;
    }

    @Override
    public boolean canValidateResponse() {
        return false;
    }

    @Override
    public ConnectorProperties clone() {
        return new TLSHttpDispatcherProperties(this);
    }

    @Override
    public void migrate3_0_1(DonkeyElement donkeyElement) { }

    @Override
    public void migrate3_0_2(DonkeyElement donkeyElement) { }
}
