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
package org.jboss.test.osgi.jbosgi143;

//$Id: OSGI143TestCase.java 87103 2009-04-09 22:18:31Z thomas.diesler@jboss.com $

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jboss.osgi.spi.framework.OSGiBootstrap;
import org.jboss.osgi.spi.framework.OSGiBootstrapProvider;
import org.jboss.osgi.testing.OSGiTest;
import org.jboss.test.osgi.jbosgi143.bundleA.BeanA;
import org.jboss.test.osgi.jbosgi143.bundleX.BeanX;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

/**
 * [JBOSGI-143] Add initial support for DynamicImport-Package
 * 
 * https://jira.jboss.org/jira/browse/JBOSGI-143
 * 
 * A imports X
 * X has DynamicImport-Package: *
 * 
 * Can X load a class from A?
 * 
 * @author thomas.diesler@jboss.com
 * @since 28-Aug-2009
 */
public class OSGi143TestCase extends OSGiTest
{
   @Test
   public void testLoadClass() throws Exception
   {
      OSGiBootstrapProvider bootProvider = OSGiBootstrap.getBootstrapProvider();
      Framework framework = bootProvider.getFramework();
      framework.start();
      
      try
      {
         BundleContext sysContext = framework.getBundleContext();
         Bundle bundleX = sysContext.installBundle(getTestArchiveURL("jbosgi143-bundleX.jar").toExternalForm());
         bundleX.start();
         
         assertBundleLoadClass(bundleX, BeanX.class, true);
         
         Bundle bundleA = sysContext.installBundle(getTestArchiveURL("jbosgi143-bundleA.jar").toExternalForm());
         bundleA.start();
         
         assertBundleLoadClass(bundleA, BeanA.class, true);
         
         assertBundleLoadClass(bundleA, BeanX.class, true);
         assertBundleLoadClass(bundleX, BeanA.class, true);
      }
      finally
      {
         framework.stop();
         framework.waitForStop(1000);
      }
   }

   private void assertBundleLoadClass(Bundle bundle, Class<?> expClazz, boolean success) 
   {
      String message = bundle.getSymbolicName() + " loads " + expClazz.getName();
      
      Class<?> wasClass;
      try
      {
         wasClass = bundle.loadClass(expClazz.getName());
         if (success)
         {
            assertEquals(message, expClazz.getName(), wasClass.getName());
         }
         else
         {
            fail("ClassNotFoundException expected for: " + message);
         }
      }
      catch (ClassNotFoundException ex)
      {
         if (success)
            fail("Unexpected ClassNotFoundException for: " + message);
      }
   }
}