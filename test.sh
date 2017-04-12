./gradlew --stop

./gradlew testPureflossDebug -PdisablePreDex --no-daemon

cp -r app/build/reports/tests/ $CIRCLE_ARTIFACTS
