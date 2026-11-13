# REM F2c · browser fixture

Not part of the Vaadin reactor. Chromium checks of the F2c client side items, run by `esegui.sh` in the F2c card folder (outside the sandbox, by L0), against the base (7.7.17.1 at f54ba34f) and the candidate.

| File | Content |
| --- | --- |
| `pom.xml`, `src/main/java/rem/f2c/` | Jetty 9.4.19 server bound to 127.0.0.1, one context per scenario (`/panel`, `/keyboard`, `/combo`, `/table`, `/grid`, `/responsive`) |
| `e2e/f2c-e2e.cjs` | Playwright script, Chromium only, network limited to the fixture host |

| Scenario | Item | Expectation |
| --- | --- | --- |
| `i05-panel` | I05/M04 | red on base, green on candidate |
| `i02-keyboard` | I02 (Chromium) | green on both: key down navigation unchanged on Chromium |
| `i03-autocomplete` | I03 | red on base, green on candidate |
| `i44-popup-left`, `i40-popup-positions`, `i01-grid-sort` | I44, I40, I01 | red on base, green on candidate; corrections kept only if the base is red |
| `i10-table-select`, `i15-grid-add-column`, `i55-responsive` | I10, I15, I55 | red on base, green on candidate |
| `i14-fast-tab`, `i42-popup-scroll`, `i46-grid-select-*` | I14, I42, I46 | reproduction on the base only, no correction in the candidate yet |

The GWTTestCase tests without a browser are in `client/src/test-gwt/java` and run with `prove-gwt.sh` in the card folder.
