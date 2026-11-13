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
package com.vaadin.server.communication;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.asm.ClassReader;
import org.mockito.asm.tree.AbstractInsnNode;
import org.mockito.asm.tree.ClassNode;
import org.mockito.asm.tree.MethodInsnNode;
import org.mockito.asm.tree.MethodNode;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;

/**
 * CVE-2021-31403 (M01 / I18): the CSRF token, the upload security key and the
 * push id must be compared with a time-constant method.
 * <p>
 * The check is static: it reads the compiled byte code of the methods that
 * compare the tokens and verifies that they call
 * {@link java.security.MessageDigest#isEqual(byte[], byte[])} and never
 * {@link String#equals(Object)}. It does not measure time.
 */
public class TimeConstantTokenComparisonTest {

    private static final String MESSAGE_DIGEST = "java/security/MessageDigest";
    private static final String STRING = "java/lang/String";

    private static List<MethodInsnNode> calls(Class<?> type, String method)
            throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        InputStream in = type.getResourceAsStream(resource);
        Assert.assertNotNull("byte code of " + type, in);
        ClassNode node = new ClassNode();
        try {
            new ClassReader(in).accept(node, 0);
        } finally {
            in.close();
        }
        List<MethodInsnNode> result = new ArrayList<MethodInsnNode>();
        boolean found = false;
        for (Object m : node.methods) {
            MethodNode methodNode = (MethodNode) m;
            if (!methodNode.name.equals(method)) {
                continue;
            }
            found = true;
            for (AbstractInsnNode insn = methodNode.instructions
                    .getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode) {
                    result.add((MethodInsnNode) insn);
                }
            }
        }
        Assert.assertTrue("method " + method + " not found in " + type, found);
        return result;
    }

    private static void assertTimeConstant(Class<?> type, String method)
            throws IOException {
        boolean isEqual = false;
        for (MethodInsnNode call : calls(type, method)) {
            if (STRING.equals(call.owner) && "equals".equals(call.name)) {
                Assert.fail(type.getSimpleName() + "." + method
                        + " compares a token with String.equals, which is not time-constant");
            }
            if (MESSAGE_DIGEST.equals(call.owner)
                    && "isEqual".equals(call.name)) {
                isEqual = true;
            }
        }
        Assert.assertTrue(type.getSimpleName() + "." + method
                + " does not use MessageDigest.isEqual", isEqual);
    }

    @Test
    public void csrfTokenComparedInConstantTime() throws IOException {
        assertTimeConstant(VaadinService.class, "isCsrfTokenValid");
    }

    @Test
    public void pushIdComparedInConstantTime() throws IOException {
        assertTimeConstant(PushHandler.class, "isPushIdValid");
    }

    @Test
    public void uploadSecurityKeyComparedInConstantTime() throws IOException {
        assertTimeConstant(FileUploadHandler.class, "handleRequest");
    }

    private static VaadinSession sessionWithToken(String token) {
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration conf = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(session.getService()).thenReturn(service);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(conf);
        Mockito.when(conf.isXsrfProtectionEnabled()).thenReturn(true);
        Mockito.when(session.getCsrfToken()).thenReturn(token);
        return session;
    }

    @Test
    public void csrfTokenValidation() {
        String token = "0c3f1a4e-7b5d-4c1e-9a7d-2a6b0e8f9d11";
        VaadinSession session = sessionWithToken(token);
        Assert.assertTrue(
                VaadinService.isCsrfTokenValid(session, new String(token)));
        Assert.assertFalse(VaadinService.isCsrfTokenValid(session,
                "0c3f1a4e-7b5d-4c1e-9a7d-2a6b0e8f9d12"));
        Assert.assertFalse(VaadinService.isCsrfTokenValid(session, ""));
        Assert.assertFalse(VaadinService.isCsrfTokenValid(session, null));
        Assert.assertFalse(VaadinService
                .isCsrfTokenValid(sessionWithToken(null), token));
    }
}
