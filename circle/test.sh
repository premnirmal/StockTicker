#! /bin/bash
code=0
./gradlew testPureflossDebug -PdisablePreDex --no-daemon
if [ $? != 0 ] ; then
  code=1
fi
cp -r app/build/reports/tests/ $CIRCLE_TEST_REPORTS/junit/
cp -r app/build/test-results/testPureflossDebugUnitTest/ $CIRCLE_TEST_REPORTS/junit/
exit $code