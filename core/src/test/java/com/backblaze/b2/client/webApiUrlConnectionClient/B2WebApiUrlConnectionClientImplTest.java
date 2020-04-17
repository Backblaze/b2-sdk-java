/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiUrlConnectionClient;

import com.backblaze.b2.client.webApiUrlConnectionClient.B2WebApiUrlConnectionClientImpl;
import org.junit.Test;

public class B2WebApiUrlConnectionClientImplTest {
    @Test
    public void testForCoverageOnly() {
        // just be sure the class isn't unused.
        // that's a pretty weak test!
        new B2WebApiUrlConnectionClientImpl();
    }
}