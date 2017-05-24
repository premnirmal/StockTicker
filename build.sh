#! /bin/bash
./gradlew assemblePureflossDebug -PdisablePreDex
if [ $? != 0 ] ; then
  exit 1
fi
cp -r app/build/outputs $CIRCLE_ARTIFACTS
