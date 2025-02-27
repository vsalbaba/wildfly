/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum XMLElement {
    // must be first
    UNKNOWN(""),

    AUTH_PROTOCOL("auth-protocol"),
    CIPHER_TOKEN("cipher-token"),
    CHANNEL(ChannelResourceDefinition.WILDCARD_PATH),
    CHANNELS("channels"),
    DEFAULT_THREAD_POOL("default-thread-pool"),
    DIGEST_TOKEN("digest-token"),
    ENCRYPT_PROTOCOL("encrypt-protocol"),
    FORK(ForkResourceDefinition.WILDCARD_PATH),
    JDBC_PROTOCOL("jdbc-protocol"),
    KEY_CREDENTIAL_REFERENCE(EncryptProtocolResourceDefinition.Attribute.KEY_CREDENTIAL),
    PLAIN_TOKEN("plain-token"),
    PROPERTY(ModelDescriptionConstants.PROPERTY),
    PROTOCOL(ProtocolResourceDefinition.WILDCARD_PATH),
    RELAY(RelayResourceDefinition.WILDCARD_PATH),
    REMOTE_SITE(RemoteSiteResourceDefinition.WILDCARD_PATH),
    SHARED_SECRET_CREDENTIAL_REFERENCE(AuthTokenResourceDefinition.Attribute.SHARED_SECRET),
    SOCKET_PROTOCOL("socket-protocol"),
    SOCKET_DISCOVERY_PROTOCOL("socket-discovery-protocol"),
    STACK(StackResourceDefinition.WILDCARD_PATH),
    STACKS("stacks"),
    TRANSPORT(TransportResourceDefinition.WILDCARD_PATH),
    ;

    private final String name;

    XMLElement(PathElement path) {
        this.name = path.isWildcard() ? path.getKey() : path.getValue();
    }

    XMLElement(Attribute attribute) {
        this.name = attribute.getName();
    }

    XMLElement(String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    private enum XMLElementFunction implements Function<ModelNode, XMLElement> {
        PROTOCOL(XMLElement.PROTOCOL),
        SOCKET_PROTOCOL(XMLElement.SOCKET_PROTOCOL),
        JDBC_PROTOCOL(XMLElement.JDBC_PROTOCOL),
        ENCRYPT_PROTOCOL(XMLElement.ENCRYPT_PROTOCOL),
        SOCKET_DISCOVERY_PROTOCOL(XMLElement.SOCKET_DISCOVERY_PROTOCOL),
        AUTH_PROTOCOL(XMLElement.AUTH_PROTOCOL),
        ;
        private final XMLElement element;

        XMLElementFunction(XMLElement element) {
            this.element = element;
        }

        @Override
        public XMLElement apply(ModelNode ignored) {
            return this.element;
        }
    }

    private static final Map<String, XMLElement> elements = new HashMap<>();
    private static final Map<String, Function<ModelNode, XMLElement>> protocols = new HashMap<>();
    private static final Map<String, XMLElement> tokens = new HashMap<>();

    static {
        for (XMLElement element : values()) {
            String name = element.getLocalName();
            if (name != null) {
                elements.put(name, element);
            }
        }

        Function<ModelNode, XMLElement> function = new Function<ModelNode, XMLElement>() {
            @Override
            public XMLElement apply(ModelNode model) {
                // Use socket-protocol element only if optional socket-binding was defined
                return model.hasDefined(SocketProtocolResourceDefinition.Attribute.SOCKET_BINDING.getName()) ? XMLElement.SOCKET_PROTOCOL : XMLElement.PROTOCOL;
            }
        };
        for (ProtocolRegistration.SocketProtocol protocol : EnumSet.allOf(ProtocolRegistration.SocketProtocol.class)) {
            protocols.put(protocol.name(), function);
        }
        for (ProtocolRegistration.MulticastProtocol protocol : EnumSet.allOf(ProtocolRegistration.MulticastProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.SOCKET_PROTOCOL);
        }
        for (ProtocolRegistration.JdbcProtocol protocol : EnumSet.allOf(ProtocolRegistration.JdbcProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.JDBC_PROTOCOL);
        }
        for (ProtocolRegistration.EncryptProtocol protocol : EnumSet.allOf(ProtocolRegistration.EncryptProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.ENCRYPT_PROTOCOL);
        }
        for (ProtocolRegistration.InitialHostsProtocol protocol : EnumSet.allOf(ProtocolRegistration.InitialHostsProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.SOCKET_DISCOVERY_PROTOCOL);
        }
        for (ProtocolRegistration.AuthProtocol protocol : EnumSet.allOf(ProtocolRegistration.AuthProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.AUTH_PROTOCOL);
        }

        tokens.put(PlainAuthTokenResourceDefinition.PATH.getValue(), XMLElement.PLAIN_TOKEN);
        tokens.put(DigestAuthTokenResourceDefinition.PATH.getValue(), XMLElement.DIGEST_TOKEN);
        tokens.put(CipherAuthTokenResourceDefinition.PATH.getValue(), XMLElement.CIPHER_TOKEN);
    }

    public static XMLElement forName(String localName) {
        return elements.getOrDefault(localName, UNKNOWN);
    }

    public static XMLElement forProtocolName(Property protocol) {
        return protocols.getOrDefault(protocol.getName(), XMLElementFunction.PROTOCOL).apply(protocol.getValue());
    }

    public static XMLElement forAuthTokenName(String token) {
        XMLElement element = tokens.get(token);
        if (element == null) throw new IllegalArgumentException(token);
        return element;
    }
}
