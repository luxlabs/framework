/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.tests.data.validator;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.data.validator.EmailValidator;

/**
 * CVE-2020-36320 (M02 / I16): the regular expression of
 * {@link EmailValidator} must not backtrack catastrophically.
 */
public class EmailValidatorRedosTest {

    private static final long LIMIT_MS = 2000;

    private final EmailValidator validator = new EmailValidator("Error");

    private static String malicious(int length) {
        StringBuilder sb = new StringBuilder("a@b.");
        for (int i = 0; i < length; i++) {
            sb.append('a');
        }
        // the trailing character forces the matcher to try every split of
        // the top level domain into groups of 2 to 4 characters
        return sb.append('!').toString();
    }

    @Test
    public void maliciousInputIsRejectedQuickly() throws Exception {
        final String input = malicious(80);
        ExecutorService executor = Executors
                .newSingleThreadExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "EmailValidatorRedosTest");
                        t.setDaemon(true);
                        return t;
                    }
                });
        try {
            Future<Boolean> result = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    return validator.isValid(input);
                }
            });
            try {
                Assert.assertFalse(result.get(LIMIT_MS, TimeUnit.MILLISECONDS));
            } catch (TimeoutException e) {
                result.cancel(true);
                Assert.fail("EmailValidator did not reject the malicious input within "
                        + LIMIT_MS + " ms (catastrophic backtracking)");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void validAddresses() {
        String[] valid = { "my.name@email.com", "a@b.co", "first.last+tag@sub.domain.org",
                "x_y-z@host-name.info", "UPPER@EXAMPLE.COM", "a@b.museum" };
        for (String address : valid) {
            Assert.assertTrue(address, validator.isValid(address));
        }
    }

    @Test
    public void invalidAddresses() {
        String[] invalid = { "not.an.email", "a@b", "@b.com", "a@b.c", "a b@c.com",
                "a@b.com!", "a@@b.com" };
        for (String address : invalid) {
            Assert.assertFalse(address, validator.isValid(address));
        }
    }
}
