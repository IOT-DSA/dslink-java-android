#!/usr/bin/env bash
set -e
./gradlew dist
adb install -r build/apk/DGMobile.apk
adb shell am start com.dglogik.mobile/com.dglogik.mobile.ui.ControllerActivity
