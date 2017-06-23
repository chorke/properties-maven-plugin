package org.codehaus.mojo.properties;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import org.junit.Test;
import org.apache.maven.plugin.MojoFailureException;
import java.util.Properties;

/**
 * Tests the support class that produces concrete values from a set of properties.
 */
public class PropertyResolverTest
{
    private final PropertyResolver resolver = new PropertyResolver();

    @Test
    public void validPlaceholderIsResolved()
        throws MojoFailureException
    {
        Properties properties = new Properties();
        properties.setProperty( "p1", "${p2}" );
        properties.setProperty( "p2", "value" );

        String value1 = resolver.getPropertyValue( "p1", properties, new Properties() );
        String value2 = resolver.getPropertyValue( "p2", properties, new Properties() );

        assertEquals( "value", value1 );
        assertEquals( "value", value2 );
    }

    @Test
    public void unknownPlaceholderIsLeftAsIs()
        throws MojoFailureException
    {
        Properties properties = new Properties();
        properties.setProperty( "p1", "${p2}" );
        properties.setProperty( "p2", "value" );
        properties.setProperty( "p3", "${unknown}" );

        String value1 = resolver.getPropertyValue( "p1", properties, new Properties() );
        String value2 = resolver.getPropertyValue( "p2", properties, new Properties() );
        String value3 = resolver.getPropertyValue( "p3", properties, new Properties() );

        assertEquals( "value", value1 );
        assertEquals( "value", value2 );
        assertEquals( "${unknown}", value3 );
    }

    @Test
    public void multipleValuesAreResolved()
        throws MojoFailureException
    {
        Properties properties = new Properties();
        properties.setProperty( "hostname", "localhost" );
        properties.setProperty( "port", "8080" );
        properties.setProperty( "base.url", "http://${hostname}:${port}/" );

        String value = resolver.getPropertyValue( "base.url", properties, new Properties() );

        assertEquals( "http://localhost:8080/", value );
    }

    @Test
    public void malformedPlaceholderIsLeftAsIs()
        throws MojoFailureException
    {
        Properties properties = new Properties();
        properties.setProperty( "p1", "${p2}" );
        properties.setProperty( "p2", "value" );
        properties.setProperty( "p4", "${malformed" );

        String value1 = resolver.getPropertyValue( "p1", properties, new Properties() );
        String value2 = resolver.getPropertyValue( "p2", properties, new Properties() );
        String value4 = resolver.getPropertyValue( "p4", properties, new Properties() );

        assertEquals( "value", value1 );
        assertEquals( "value", value2 );
        assertEquals( "${malformed", value4 );
    }

    @Test
    public void propertyDefinedAsItselfIsIllegal()
        throws MojoFailureException
    {
        Properties properties = new Properties();
        properties.setProperty( "p1", "${p2}" );
        properties.setProperty( "p2", "value" );
        properties.setProperty( "p5", "${p5}" );
        properties.setProperty( "p6", "${p7}" );
        properties.setProperty( "p7", "${p6}" );

        String value1 = resolver.getPropertyValue( "p1", properties, new Properties() );
        String value2 = resolver.getPropertyValue( "p2", properties, new Properties() );
        String value5 = null;
        try
        {
            value5 = resolver.getPropertyValue( "p5", properties, new Properties() );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "p5" ) );
        }
        String value6 = null;
        try
        {
            value6 = resolver.getPropertyValue( "p6", properties, new Properties() );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "p7" ) );
        }

        assertEquals( "value", value1 );
        assertEquals( "value", value2 );
        assertNull( value5 );
        assertNull( value6 );
    }

    @Test
    public void valueIsObtainedFromSystemProperty()
        throws MojoFailureException
    {
        Properties saved = System.getProperties();
        System.setProperty( "system.property", "system.value" );

        Properties properties = new Properties();
        properties.setProperty( "p1", "${system.property}" );

        String value = resolver.getPropertyValue( "p1", properties, new Properties() );

        try
        {
            assertEquals( "system.value", value );
        }
        finally
        {
            System.setProperties( saved );
        }
    }

    @Test
    public void valueIsObtainedFromEnvironmentProperty()
        throws MojoFailureException
    {
        Properties environment = new Properties();
        environment.setProperty( "PROPERTY", "env.value" );

        Properties properties = new Properties();
        properties.setProperty( "p1", "${env.PROPERTY}" );

        String value = resolver.getPropertyValue( "p1", properties, environment );

        assertEquals( "env.value", value );
    }

    @Test
    public void missingPropertyIsTolerated()
    {
        assertEquals( "", resolver.getPropertyValue( "non-existent", new Properties(), null ) );
    }

    @Test
    public void oneKeyTwice()
        throws MojoFailureException
    {
        Properties properties = new Properties();
        properties.setProperty( "p1", "${p3}${p3}" );
        properties.setProperty( "p2", "${p3}b${p3}" );
        properties.setProperty( "p3", "a" );
        properties.setProperty( "p4", "${p5}${p6}" );
        properties.setProperty( "p5", "${p6}" );
        properties.setProperty( "p6", "${p3}" );
        properties.setProperty( "p7", "a${p3}" );
        properties.setProperty( "p8", "${p7}" );

        String value1 = resolver.getPropertyValue( "p1", properties, new Properties() );
        String value2 = resolver.getPropertyValue( "p2", properties, new Properties() );
        String value3 = resolver.getPropertyValue( "p3", properties, new Properties() );
        String value4 = resolver.getPropertyValue( "p4", properties, new Properties() );
        String value5 = resolver.getPropertyValue( "p5", properties, new Properties() );
        String value6 = resolver.getPropertyValue( "p6", properties, new Properties() );
        String value7 = resolver.getPropertyValue( "p7", properties, new Properties() );
        String value8 = resolver.getPropertyValue( "p8", properties, new Properties() );

        assertEquals( "aa", value1 );
        assertEquals( "aba", value2 );
        assertEquals( "a", value3 );
        assertEquals( "aa", value4 );
        assertEquals( "a", value5 );
        assertEquals( "a", value6 );
        assertEquals( "aa", value7 );
        assertEquals( "aa", value8 );
    }
}
