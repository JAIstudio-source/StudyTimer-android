#!/bin/bash
# Recompile the real WeeklyCardView.kt against the AWT-backed android.graphics
# shims and re-render the card previews to out/*.png.
set -e
cd "$(dirname "$0")"
JDK="${JDK:-/storage/jdk-21.0.6+7}"
KOTLINC="${KOTLINC:-/tmp/kotlinc/bin/kotlinc}"
STDLIB="${STDLIB:-/tmp/kotlinc/lib/kotlin-stdlib.jar}"

rm -rf classes && mkdir -p classes
"$JDK/bin/javac" -d classes $(find src/shim -name "*.java")
"$KOTLINC" -cp classes -d classes ../app/src/main/java/com/madeby/JAI/WeeklyCardView.kt
"$KOTLINC" -cp classes -d classes src/com/madeby/JAI/cardtest/Main.kt
"$JDK/bin/java" -Djava.awt.headless=true -cp "classes:$STDLIB" com.madeby.JAI.cardtest.MainKt
