#! /bin/bash
code=0
./gradlew testPureflossDebug -PdisablePreDex --no-daemon
if [ $? != 0 ] ; then
  code=1
fi
cp -r app/build/reports/tests/ $CIRCLE_ARTIFACTS
exit $code