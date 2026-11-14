# F2b fixtures

Fixtures of package F2b (server-side fixes of the 7.7.17.1 fork), outside the Maven reactor. Run by `IMP/schede/P2/F2b/esegui.sh` (Chromium only) and, without browser and without sockets, by `rem.f2b.fixture.HttpChecks`.

| File | Purpose |
| --- | --- |
| `src/rem/f2b/fixture/ActionsUI.java` | M05: hostile captions in Table and Tree context menus and in a Panel shortcut; `setCaptionAsHtml(true)` by reflection when available |
| `src/rem/f2b/fixture/GridUI.java` | M03: hostile and encoded Grid header captions, hostile hiding toggle caption |
| `src/rem/f2b/fixture/BootUI.java` | bootstrap page comparison (jsoup 1.24.1), M15 |
| `src/rem/f2b/fixture/FixtureServer.java` | Jetty 9.4 on 127.0.0.1, ports 18413-18415 only; `local()` without sockets |
| `src/rem/f2b/fixture/HttpChecks.java` | bootstrap pages, static resources, M14 directories, M15 `/app/VAADIN/...` |
| `browser/test.js` | Playwright checks; every request outside 127.0.0.1:port is aborted and recorded |
| `browser/evaluate.py` | expected red on 7.7.17.1, green on the candidate |
| `classpath.sh` | runtime classpath from a Maven repository |
| `src/rem/f2b/fixture/boot/BootNestedJarCheck.java` | M14 (review R3): `/VAADIN/` files of a Spring Boot 1.5 executable jar (nested jars, `BOOT-INF/classes`) stay allowed, directories are refused; needs the Spring Boot 1.5.22 loader on the classpath, not used by `esegui.sh` |
