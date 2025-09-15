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

package org.openintegrationengine.tlsmanager.shared;

import com.mirth.connect.model.converters.ObjectXMLSerializer;
import org.openintegrationengine.tlsmanager.shared.properties.HttpConnectorProperties;

import java.util.List;

public class SerializationController {

    private static final List<String> types = List.of(
        HttpConnectorProperties.class.getCanonicalName()
    );

    private static final List<String> wildcardTypes = List.of();
    private static final List<String> typeHierarchies = List.of();

    // Register our property classes with XStream to prevent ForbiddenClassException
    public static void registerSerializableClasses() {
        ObjectXMLSerializer.getInstance().allowTypes(types, wildcardTypes, typeHierarchies);
    }

    private SerializationController() {}
}
