#!/bin/bash
# Prints the runtime classpath of the F2b fixtures.
# usage: classpath.sh <maven repository with vaadin 7.7.17.1 jars> <jsoup jar>
# Environment: M2_REPO, a local Maven repository with the third party jars
# (sass compiler, sac, flute, servlet API, Jetty util/http/io); DEPS_REPO, a
# Maven repository with jetty-server, jetty-servlet and jetty-security.
set -euo pipefail
M2=$1
JSOUP=$2
: "${M2_REPO:?set M2_REPO to a local Maven repository}"
: "${DEPS_REPO:?set DEPS_REPO to the Maven repository with the Jetty server jars}"
HOME_M2=$M2_REPO
DF=$DEPS_REPO
V=7.7.17.1
J=9.4.19.v20190610
cp=""
add() { [ -f "$1" ] || { echo "missing $1" >&2; exit 2; }; cp="$cp${cp:+:}$1"; }
for m in server shared client-compiled themes; do add "$M2/com/vaadin/vaadin-$m/$V/vaadin-$m-$V.jar"; done
add "$JSOUP"
add "$HOME_M2/com/vaadin/vaadin-sass-compiler/0.9.13/vaadin-sass-compiler-0.9.13.jar"
add "$HOME_M2/org/w3c/css/sac/1.3/sac-1.3.jar"
add "$HOME_M2/com/vaadin/external/flute/flute/1.3.0.gg2/flute-1.3.0.gg2.jar"
add "$HOME_M2/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar"
for a in jetty-util jetty-http jetty-io; do add "$HOME_M2/org/eclipse/jetty/$a/$J/$a-$J.jar"; done
for a in jetty-server jetty-servlet jetty-security; do add "$DF/org/eclipse/jetty/$a/$J/$a-$J.jar"; done
echo "$cp"
