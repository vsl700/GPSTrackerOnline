package com.vasciie.gpstrackeronline.services;

import junit.framework.TestCase;

import org.junit.Test;

public class APIConnectorTest extends TestCase {

    @Test
    public void testCallerLogin() {
        // Test Logging in with the test account
        assertTrue(APIConnector.CallerLogin("vsl700", "stBG3541!"));
    }
}