# REM F2d · browser fixture

Not part of the Vaadin reactor. Chromium checks of I09, I11, I29, I41 and of push in long polling (criterion 4 of the F2d card), plus the static check of I21 on the compiled Valo theme.

| File | Content |
| --- | --- |
| `pom.xml`, `src/main/java/rem/f2d/` | Jetty 9.4.19 server bound to 127.0.0.1, one context per scenario |
| `e2e/f2d-e2e.cjs` | Playwright script, Chromium only, network limited to the fixture host |
| `check-i21-css.py` | static check of the compiled Valo CSS for I21 |

The fixture is built twice, against the base (7.7.17.1 at f54ba34f) and against the candidate, and run by `esegui.sh` in the F2d card folder.
