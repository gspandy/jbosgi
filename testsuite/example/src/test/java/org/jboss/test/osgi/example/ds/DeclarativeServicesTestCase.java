/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.test.osgi.example.ds;

import java.io.InputStream;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.DeclarativeServicesSupport;
import org.jboss.test.osgi.RepositorySupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Example for Declarative Services
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Jul-2011
 */
@RunWith(Arquillian.class)
public class DeclarativeServicesTestCase {

    @Inject
    public BundleContext context;

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-ds");
        archive.addClasses(SampleComparator.class, DeclarativeServicesSupport.class, RepositorySupport.class);
        archive.addAsResource("ds/OSGI-INF/sample.xml", "OSGI-INF/sample.xml");
        archive.addAsManifestResource(RepositorySupport.BUNDLE_VERSIONS_FILE);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addManifestHeader("Service-Component", "OSGI-INF/sample.xml");
                builder.addImportPackages(ServiceTracker.class);
                builder.addImportPackages(XRequirementBuilder.class, Repository.class, Resource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testImmediateService() throws Exception {

        // Start the test bundle
        bundle.start();

        // Provide Declarative Services support
        DeclarativeServicesSupport.provideDeclarativeServices(context, bundle);

        // Track the service provided by the test bundle
        final CountDownLatch latch = new CountDownLatch(1);
        ServiceTracker tracker = new ServiceTracker(context, Comparator.class.getName(), null) {
            public Object addingService(ServiceReference reference) {
                Comparator<?> service = (Comparator<?>) super.addingService(reference);
                latch.countDown();
                return service;
            }
        };
        tracker.open();

        // Wait for the service to become available
        if (latch.await(2, TimeUnit.SECONDS) == false)
            throw new TimeoutException("Timeout tracking Comparator service");
    }
}