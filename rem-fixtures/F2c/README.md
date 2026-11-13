# REM F2c · browser fixture

Not part of the Vaadin reactor. Chromium checks of the F2c client side items, run by `esegui.sh` in the F2c card folder (outside the sandbox, by L0), against the base (7.7.17.1 at f54ba34f) and the candidate.

| File | Content |
| --- | --- |
| `pom.xml`, `src/main/java/rem/f2c/` | Jetty 9.4.19 server bound to 127.0.0.1, one context per scenario (`/panel`, `/keyboard`, `/combo`, `/table`, `/grid`, `/responsive`) |
| `e2e/f2c-e2e.cjs` | Playwright script, Chromium only, network limited to the fixture host |

| Scenario | Item | Expectation |
| --- | --- | --- |
| `i05-panel`, `m03-grid-toggles` | I05/M04, M03 (client side) | red on base, green on candidate |
| `i02-keyboard` | I02 (Chromium) | green on both: key down navigation unchanged on Chromium |
| `i03-autocomplete`, `i14-fast-tab`, `i40-popup-positions` | I03, I14, I40 | red on base, green on candidate |
| `i10-table-select`, `i15-grid-add-column`, `i55-responsive`, `i46-grid-select-*` | I10, I15, I55, I46 | red on base, green on candidate |
| `i01-grid-sort`, `i44-popup-left`, `i42-popup-scroll` | I01, I44, I42 | green on both: defect not reproduced on 7.7.17, no correction (non regression check) |

The GWTTestCase tests without a browser are in `client/src/test-gwt/java` and run with `prove-gwt.sh` in the card folder.
