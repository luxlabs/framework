# LuxLabs Vaadin Framework 7.7.17.1

> **Unofficial fork.** LuxLabs Vaadin Framework 7.7.17.1 is an unofficial fork of the tag `7.7.17` of [vaadin/framework](https://github.com/vaadin/framework), released under the Apache License, Version 2.0. It is **not** an official Vaadin release, it is **not** affiliated with Vaadin Ltd, and it is **not** endorsed or supported by Vaadin Ltd. "Vaadin" is a trademark of Vaadin Ltd; the name is used here only to identify the code this fork is derived from.

*Modified by LuxLabs in 2026: the sections from the start of this file to "Compatibility notes" describe the LuxLabs fork 7.7.17.1; "Compatibility notes" keeps the fork notes written with the M05 and M06 fixes. The upstream text, from "Upstream README" on, is unchanged. Original copyright and license notices are retained.*

## Coordinates

| Item | Value |
| --- | --- |
| Group and artifacts | `com.vaadin:vaadin-root`, `vaadin-shared`, `vaadin-server`, `vaadin-push`, `vaadin-client`, `vaadin-client-compiler`, `vaadin-client-compiled`, `vaadin-themes` |
| Version | `7.7.17.1` |
| Published files | jar and POM of every module; `-sources` jar for `vaadin-shared`, `vaadin-server`, `vaadin-client` and `vaadin-client-compiler` (as for 7.7.17 on Maven Central) |
| Repository | an internal Nexus only; the fork is not published to Maven Central |
| OSGi metadata | `Implementation-Version` is `7.7.17.1`; `Bundle-Version` and the versions in `Export-Package` stay `7.7.17` (the build reads only the first three numbers). OSGi is not used by the applications this fork is built for |

## Sources

| Allowed | Not used |
| --- | --- |
| The tag `7.7.17` of vaadin/framework (the last open source Vaadin 7 release, Apache License 2.0) | Any code of Vaadin 7.7.18 and later, including the jars on Maven Central |
| Code of Vaadin Framework 8 up to the tag `8.14.3` included (Apache License 2.0), each piece recorded with tag, commit, file and lines | Vaadin 8 after `8.14.3` |
| Third-party libraries under their own licenses (jsoup, GWT `2.7.0.vaadin7`, Atmosphere runtime `2.2.13.vaadin1` and JavaScript `2.2.13.vaadin5`) | — |
| Public advisories and release notes, read to know what to fix, not how | — |

Fixes without an equivalent in Vaadin 8 up to 8.14.3 are written from scratch. Every modified file carries a modification notice (Apache License 2.0, section 4(b)).

## Fixes

Item codes refer to the LuxLabs remediation matrix (`M..`, vulnerabilities and security fixes) and inventory (`I..`, fixes listed in the public notes of 7.7.18 to 7.7.55).

### Security

| Item | Change |
| --- | --- |
| M01, I18 · CVE-2021-31403 | CSRF token, upload security key and push id are compared in constant time |
| M02, I16 · CVE-2020-36320 | `EmailValidator` uses a regular expression that cannot be blocked by malicious input |
| M03, I06 · CVE-2019-25028 | Grid header captions, and the captions of the column hiding toggles, are sent to the client as plain text |
| M05, I52 · CVE-2025-15022 | `Action` captions are escaped by default (Table and Tree context menus, Calendar actions, `ActionManager`); `Action#setCaptionAsHtml(true)` keeps trusted HTML |
| M06, I48 · CVE-2025-9467 | `interruptUpload()` and the upload size limit hold on the bytes actually received, also without `Content-Length` |
| M08, M09, M10, I27 · CVE-2021-37714, CVE-2022-36033, CVE-2026-75140 | jsoup updated from 1.8.3 to 1.24.1 |
| M14, I30 | directories and resources that cannot be classified as regular files are not served under `/VAADIN/` |
| M15, I54 | no bootstrap page is served for paths under `/VAADIN/` |
| M16, I43 | error responses carry `Cache-Control: no-cache, no-store, must-revalidate` |
| M17, I58 | the size of the body of UIDL requests and push messages is limited (configurable) |
| M04, I05 · Panel caption XSS | client side: **to be completed by F2c** |
| M03 · client part | Grid column hiding toggle in the client: **to be completed by F2c** |

Not affected, declared with OpenVEX: CVE-2026-110737 (M07: with Atmosphere `2.2.13.vaadin1` the push endpoint does not send CORS headers) and the libraries bundled in `gwt-dev` (M13: build-time only).

### Browser compatibility

> **Placeholder for F2c.** The client-side fixes (keyboard navigation with Firefox 65+ in MenuBar, Slider, DateField, Tree and Table, I02; ComboBox autocompletion with Chrome, I03; global variable `u2f` with Firefox 67+, I04; text selection in Table with Chrome, I10; text selection in Grid cells, I46; `ResponsiveConnector` and CSS rules without `selectorText`, I55; and the client parts of M03 and M04) are listed here when F2c is integrated, with what was fixed, what was tested in Chromium and what is still to be tested in Firefox.

### Functional

| Item | Change |
| --- | --- |
| I11 | Grid (`Escalator`): rows shown correctly after a resize when scrolled to the bottom |
| I19 | push: thread local instances cleared when the connection is lost |
| I20 | the `ClientCache` of a detached UI is removed (memory leak) |
| I21 | Valo theme: cell borders no longer grow the row height of Table and TreeTable |
| I23 | the session lock is not left held in `findOrCreateVaadinSession` |
| I29 | Window: resize and drag curtains removed when the window is closed |
| I41 | Upload: no upload is started without a file, and the change listener still works after the first upload |
| I56 | an expired session answers 403 instead of 410, and the client shows the session expired message |

### Not fixed or removed

| Item | Reason |
| --- | --- |
| I09 · push stopping after a resynchronization | not fixed: the defect could not be reproduced on 7.7.17 and the Vaadin 8 change caused regressions with the fork's widgetset; reverted |
| I34 · concurrent disconnect and push | removed: the fix written for the fork caused regressions with the fork's server; reverted |
| I24, I28, I31, I47, I51 · Atmosphere updates (2.2.13.vaadin2 to vaadin4, 2.4) | not included: those Atmosphere builds are not among the allowed inputs |
| I12, I33, I35, I59, I62 · push reconnection and session lock details | no change: not reproduced, no allowed source |
| F2c exclusions | **to be completed by F2c** |

Other notes of 7.7.18 to 7.7.55 (license, license checker, Multiplatform Runtime, Jakarta, portlets, OSGi, build changes) do not apply to this fork.

## Requirements

| Item | Requirement |
| --- | --- |
| Java at runtime | Java 8 or later: jsoup 1.24.1 is compiled for Java 8, so `vaadin-server` declares `Bundle-RequiredExecutionEnvironment: JavaSE-1.8` |
| Widgetsets and themes | compile them with `com.vaadin:vaadin-maven-plugin:7.7.17` (Apache License 2.0); no plugin `7.7.17.1` exists. The root POM pins `vaadin.plugin.version` to `7.7.17` in the default build and in the `snapshot` and `release` profiles |
| Code compiled against 7.7.17 | must be recompiled if it uses jsoup types (see the compatibility notes below) |

## Changed behaviour

| Area | What changes | What to do |
| --- | --- | --- |
| Action captions (M05) | captions of `Action`s are shown as text | call `Action#setCaptionAsHtml(true)` for captions that are trusted HTML |
| Resources under `/VAADIN/` (M14) | directories are refused with 403; only resources verified to be regular files are served: `file:` URLs and `jar:` URLs opened as `JarURLConnection` (the JDK one and the Spring Boot loader one). Resources from containers that use other protocols (`vfs:` of JBoss/WildFly, `war:` without `jar:`, `jndi:` of Tomcat 7, custom class loader protocols) are refused with 403 | for those containers override `VaadinServlet#isAllowedVAADINResourceUrl` or serve the extracted resources |
| Paths under `/VAADIN/` (M15) | no bootstrap page is served for them | — |
| Upload size (M06) | `Upload#setMaxFileSize(long)` limits the bytes actually received; see the compatibility notes for the partial content | discard partial content in the `FailedListener` |
| Request body size (M17) | UIDL requests and push messages over `maxRequestBodySize` characters (default 32 MiB) fail without being read further; uploads are not affected | set the servlet init parameter `maxRequestBodySize` or the system property `vaadin.maxRequestBodySize`; `-1` disables the limit |
| Expired session (I56) | 403 instead of 410 | — |
| Error responses (M16) | not cacheable | — |

## Build

JDK 8 (the GWT compiler of `vaadin-client-compiled` needs it), Maven offline with an explicit reactor: the root POM, `buildhelpers` and the modules above, without `uitest` and TestBench.

```sh
JAVA_HOME=<JDK 8> JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir=<tmp>' \
mvn -B -ntp -o -s <offline settings.xml> \
  -Dmaven.repo.local=<new local repository> -Dmaven.repo.local.tail=<dependency cache>,<~/.m2/repository> \
  -pl buildhelpers,shared,push,server,client,client-compiler,client-compiled,themes -am \
  -Psnapshot -Dvaadin.plugin.version=7.7.17 install
```

The `snapshot` profile also builds the `-sources` jars.

## Compatibility notes

| Item | Note |
| --- | --- |
| jsoup | `vaadin-server` depends on jsoup 1.24.1 instead of 1.8.3. Code compiled against jsoup 1.8.3 that calls `org.jsoup.nodes.Attributes.put` or the removed `DocumentType` and `DataNode` constructors must be recompiled. |
| Java | With jsoup 1.24.1 `vaadin-server` requires Java 8 at runtime: its manifest declares `Bundle-RequiredExecutionEnvironment: JavaSE-1.8`. The Vaadin code itself still uses only the Java 6 API, apart from `java.util.function.Consumer` passed to jsoup. |
| OSGi | `Bundle-Version` and the versions in `Export-Package` stay `7.7.17`: the build reads only the first three numbers of `7.7.17.1`. `Implementation-Version` is `7.7.17.1`. |
| Upload size limit | `Upload#setMaxFileSize(long)` limits the bytes actually received, also without `Content-Length` (chunked requests). If the declared length already exceeds the limit, or a `StartedListener` calls `interruptUpload()`, the `Receiver` is not asked for an output stream, so no output is created. Without a declared length the limit is detected only when it is reached: at most the bytes under the limit reach the receiver's output stream, which is then closed, and a `FailedEvent` with `Upload.FileSizeLimitExceededException` is fired. The application's `FailedListener` must discard that partial content (for example delete the file the receiver created). |
| Action captions | Captions of `Action`s (Table and Tree context menus, Calendar actions, shortcut actions) are sent to the client escaped; `Action#setCaptionAsHtml(true)` sends trusted HTML unchanged. |
| Declarative | Designs are parsed with jsoup 1.24.1; custom tags and `menu` may still be self-closing (`<vaadin-label />`, `<menu separator />`). Rich text read from a design no longer gets the line breaks that the jsoup 1.8.3 pretty printer added. |

## Upstream README

# Vaadin Framework

*[Vaadin Framework](https://vaadin.com/framework) allows you to build modern web apps efficiently in plain Java, without touchking low level web technologies.*

For instructions about _using_ Vaadin to develop applications, please refer to [Vaadin tutorial](https://vaadin.com/docs/-/part/framework/tutorial.html) and other [documentation](https://vaadin.com/docs/).

To contribute, first refer to [Contributing Code](https://vaadin.com/wiki/-/wiki/Main/Contributing+Code) for general instructions and requirements for contributing code to the Vaadin framework.

Instructions on how to set up a working environments for developing the Vaadin Framework follow below.

## Building a package

The distribution files can be built by running the standard Maven goal `mvn install` in the project root.

## Eclipse Quick Setup

1. Run
<code>git clone https://github.com/vaadin/vaadin.git</code>
command or clone the repository your favorite Git tool.
If using Windows, you might want to add these Git settings: `core.autocrlf=false` and `core.fileMode=false`.
1. Run <code>mvn install</code> in the project root.
Note that the first compilation takes a while to finish as maven downloads dependencies used in the projects.
1. Start Eclipse with the workspace you would like to use. It is usually a good idea to use the parent folder of the Git repository as the workspace folder.
1. Import the project into Eclipse as a maven project. Use *File* -> *Import* -> *Maven* -> *Existing Maven Projects*.
1. Select the *vaadin* folder (where you cloned the project)
1. Click “Finish” to complete the import of Vaadin Framework

Now the project should compile without further configuration.

### Compiling the Default Widget Set and Themes

* Compile the default widgetset by running <code>install</code> maven goal in `vaadin-client-compiled` module root.
In Eclipse this is done by right clicking on vaadin-client-compiled project it and choosing *Run As* -> *Maven Build...*.
* Compile the default themes by running <code>install</code> maven goal in `vaadin-themes` module root.
In Eclipse this is done by right clicking on vaadin-themes project it and choosing *Run As* -> *Maven Build...*.

### Set up extra workspace preferences

The following preferences need to be set to keep the project consistent. You need to do this especially to be able to contribute changes to the project.

1. Open *Window* -> *Preferences* (Windows) or *Eclipse* -> *Preferences* (Mac)
1. Go to *General* ->  *Workspace*
 1. Set *Text file encoding* to *UTF-8*
 1. Set *New text file line delimiter* to *Unix*
1. Go to XML -> XML Files -> Editor
 1. Ensure the settings are follows:
 
    Line width: 72
    Format comments: true
    Join lines: true
    Insert whitespace before closing empty end-tags: true
    Indent-using spaces: true
    Indentation size: 4


### Running a UI test

1. In a Project Explorer  right-click *vaadin-uitest*
1. Open *Run As* -> *Maven build...*
1. Type in <code>jetty:run-exploded</code> into *Goals* and click *Run*
1. Open URL *http://localhost:8080/run/<testUI>*

## Setting up IntelliJ IDEA to Develop Vaadin 7

1. Intall and run IDEA. Ultimate Edition is better but Community Edition should also work.
1. Ensure if Git and Maven plugins are installed, properly configured and enabled.
1. Clone the repository, using menu VCS -> Checkout from Version Control -> Git -> Git Repository URL -> https://github.com/vaadin/vaadin.git.
  When the repository is cloned, do **NOT** open it as a project.
1. Open cloned repository as a maven object. Use File -> Open and choose root _pom.xml_ file
1. Have a coffee break while IDEA is loading dependencies and indexing the project
1. Run Maven targets <code>clean</code> and <code>install</code> using *Maven Projects* tool window to compile the whole project

## Running a specific UI test

1. Open *Maven Projects*
1. Open *vaadin-uitest* -> *Plugins* -> *jetty* -> *jetty:run-exploded*
1. Open URL [http://localhost:8888/run/&lt;testUI&gt;](http://localhost:8888/run/<testUI>)

## Running a Development Server

1. Open *Run* menu  and click *Edit Configurations*
1. Click green ***+*** sign at top left corner, select *Maven* from popup
1. In the run configuration page, set any name for the configuration, select *vaadin-uitest* project folder as *Working directory*
1. Type <code>exec:exec@run-development-server</code> into *Command line* and save the configuration
1. Run the configuration and open URL [http://localhost:8888/run/&lt;testUI&gt;](http://localhost:8888/run/<testUI>)

## Running a Development Server in a debug mode

1. Type <code>exec:exec@debug-development-server</code> into *Command line* and save the configuration
1. In the same dialog, create new "Remote" debug configuration, using *localhost* and *Port 5005*
1. Start both configurations and open URL [http://localhost:8888/run/&lt;testUI&gt;](http://localhost:8888/run/<testUI>)
