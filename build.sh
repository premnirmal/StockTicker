./gradlew assemblePureflossDebug -PdisablePreDex

cp -r app/build/outputs $CIRCLE_ARTIFACTS
