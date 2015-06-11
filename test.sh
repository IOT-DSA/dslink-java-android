#!/usr/bin/env bash
set -e
./gradlew dist
adb -s 05fa0543 install -r build/apk/DSAndroid.apk
[ ! -z ${ANDROID_WEAR} ] && adb -s localhost:4444 install -r wear/build/outputs/apk/wear-release.apk
adb -s 05fa0543 shell am start com.dglogik.mobile/com.dglogik.mobile.ui.ControllerActivity
[ ! -z ${ANDROID_WEAR} ] && adb -s localhost:4444 shell am start com.dglogik.mobile/com.dglogik.wear.ControllerActivity
