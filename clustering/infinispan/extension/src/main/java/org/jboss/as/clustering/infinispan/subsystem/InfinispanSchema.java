/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Locale;

import org.jboss.as.clustering.controller.Schema;

/**
 * Enumeration of the supported subsystem xml schemas.
 * @author Paul Ferraro
 */
public enum InfinispanSchema implements Schema<InfinispanSchema> {
/*  Unsupported schema versions - for reference only
    VERSION_1_0(1, 0), // AS 7.0
    VERSION_1_1(1, 1), // AS 7.1.0
    VERSION_1_2(1, 2), // AS 7.1.1
    VERSION_1_3(1, 3), // AS 7.1.2
    VERSION_1_4(1, 4), // AS 7.2.0
    VERSION_1_5(1, 5), // EAP 6.3
    VERSION_2_0(2, 0), // WildFly 8
    VERSION_3_0(3, 0), // WildFly 9
    VERSION_4_0(4, 0), // WildFly 10/11
    VERSION_5_0(5, 0), // WildFly 12
    VERSION_6_0(6, 0), // WildFly 13
    VERSION_7_0(7, 0), // WildFly 14-15
    VERSION_8_0(8, 0), // WildFly 16
*/
    VERSION_9_0(9, 0), // WildFly 17-19
    VERSION_9_1(9, 1), // EAP 7.3.4
    VERSION_10_0(10, 0), // WildFly 20
    VERSION_11_0(11, 0), // WildFly 21-22
    VERSION_12_0(12, 0), // WildFly 23, EAP 7.4
    VERSION_13_0(13, 0), // WildFly 24-26
    VERSION_14_0(14, 0), // WildFly 27-present
    ;
    static final InfinispanSchema CURRENT = VERSION_14_0;

    private final int major;
    private final int minor;

    InfinispanSchema(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    @Override
    public int major() {
        return this.major;
    }

    @Override
    public int minor() {
        return this.minor;
    }

    @Override
    public String getNamespaceUri() {
        return String.format(Locale.ROOT, "urn:jboss:domain:infinispan:%d.%d", this.major, this.minor);
    }
}
