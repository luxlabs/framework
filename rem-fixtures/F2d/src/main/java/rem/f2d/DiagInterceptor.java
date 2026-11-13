/*
 * Part of the 7.7.17.1 maintenance fork of Vaadin Framework (REM F2d).
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
package rem.f2d;

import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AtmosphereInterceptorAdapter;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;

/**
 * Fixture-only Atmosphere interceptor of the I09 diagnostics: records every
 * push request and every event of its resource (uuid and time) in
 * {@link Diag}. Configured by init parameter, the framework is not changed.
 */
public class DiagInterceptor extends AtmosphereInterceptorAdapter {

    @Override
    public Action inspect(AtmosphereResource r) {
        String method = r.getRequest().getMethod();
        Diag.event("atmo request " + method + " uuid=" + r.uuid() + "@"
                + Integer.toHexString(System.identityHashCode(r)) + " transport=" + r.transport());
        if ("GET".equals(method)) {
            r.addEventListener(new AtmosphereResourceEventListenerAdapter() {
                @Override
                public void onSuspend(AtmosphereResourceEvent e) {
                    log("suspend", e);
                }

                @Override
                public void onResume(AtmosphereResourceEvent e) {
                    log("resume", e);
                }

                @Override
                public void onDisconnect(AtmosphereResourceEvent e) {
                    log("disconnect", e);
                }

                @Override
                public void onClose(AtmosphereResourceEvent e) {
                    log("close", e);
                }

                @Override
                public void onBroadcast(AtmosphereResourceEvent e) {
                    Object m = e.getMessage();
                    String s = m == null ? "" : m.toString();
                    int i = s.indexOf("\"syncId\": ");
                    String sync = i < 0 ? "?" : s.substring(i + 10, Math.min(s.length(), i + 16)).replaceAll("[^0-9].*", "");
                    log("broadcast syncId=" + sync, e);
                }

                @Override
                public void onThrowable(AtmosphereResourceEvent e) {
                    log("throwable " + e.throwable(), e);
                }
            });
        }
        return Action.CONTINUE;
    }

    private static void log(String what, AtmosphereResourceEvent e) {
        AtmosphereResource r = e.getResource();
        Diag.event("atmo " + what + " uuid=" + (r == null ? "null"
                : r.uuid() + "@" + Integer.toHexString(System.identityHashCode(r)))
                + " cancelled=" + e.isCancelled() + " resumedOnTimeout=" + e.isResumedOnTimeout()
                + " closedByClient=" + e.isClosedByClient() + " closedByApplication=" + e.isClosedByApplication()
                + " thread=" + Thread.currentThread().getName());
    }
}
