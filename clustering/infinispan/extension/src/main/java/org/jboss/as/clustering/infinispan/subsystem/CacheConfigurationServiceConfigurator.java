/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Attribute.STATISTICS_ENABLED;
import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Capability.CONFIGURATION;

import java.util.function.Consumer;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.distribution.ch.impl.AffinityPartitioner;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.service.ConfigurationServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Builds a cache configuration from its components.
 * @author Paul Ferraro
 */
public class CacheConfigurationServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Consumer<ConfigurationBuilder>, Dependency {

    private final ConfigurationServiceConfigurator configurator;
    private final SupplierDependency<MemoryConfiguration> memory;
    private final SupplierDependency<ExpirationConfiguration> expiration;
    private final SupplierDependency<LockingConfiguration> locking;
    private final SupplierDependency<PersistenceConfiguration> persistence;
    private final SupplierDependency<TransactionConfiguration> transaction;
    private final CacheMode mode;

    private volatile boolean statisticsEnabled;

    CacheConfigurationServiceConfigurator(PathAddress address, CacheMode mode) {
        super(CONFIGURATION, address);
        this.mode = mode;
        this.memory = new ServiceSupplierDependency<>(CacheComponent.MEMORY.getServiceName(address));
        this.expiration = new ServiceSupplierDependency<>(CacheComponent.EXPIRATION.getServiceName(address));
        this.locking = new ServiceSupplierDependency<>(CacheComponent.LOCKING.getServiceName(address));
        this.persistence = new ServiceSupplierDependency<>(CacheComponent.PERSISTENCE.getServiceName(address));
        this.transaction = new ServiceSupplierDependency<>(CacheComponent.TRANSACTION.getServiceName(address));

        String containerName = address.getParent().getLastElement().getValue();
        String cacheName = address.getLastElement().getValue();
        this.configurator = new ConfigurationServiceConfigurator(this.getServiceName(), containerName, cacheName, this).require(this);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        return this.configurator.build(target);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return new CompositeDependency(this.memory, this.expiration, this.locking, this.persistence, this.transaction).register(builder);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        this.configurator.configure(context);
        return this;
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        TransactionConfiguration tx = this.transaction.get();

        builder.clustering().cacheMode(this.mode).hash().keyPartitioner(new AffinityPartitioner());
        builder.memory().read(this.memory.get());
        builder.expiration().read(this.expiration.get());
        builder.locking().read(this.locking.get());
        builder.persistence().read(this.persistence.get());
        builder.transaction().read(tx);
        builder.statistics().enabled(this.statisticsEnabled);

        // See WFLY-14356
        if (this.memory.get().storage().canStoreReferences()) {
            builder.encoding().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
        }
        try {
            // Configure invocation batching based on transaction configuration
            builder.invocationBatching().enable(tx.transactionMode().isTransactional() && (tx.transactionManagerLookup().getTransactionManager() == EmbeddedTransactionManager.getInstance()));
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }
}
