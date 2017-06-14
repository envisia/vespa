// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#include <vespa/vespalib/testkit/testapp.h>
#include <vbench/test/all.h>

using namespace vbench;

IGNORE_TEST("timer") {
    Timer timer;
    EXPECT_APPROX(0.0, timer.sample(), 0.1);
    vespalib::Thread::sleep(1000);
    EXPECT_APPROX(1.0, timer.sample(), 0.1);
    timer.reset();
    EXPECT_APPROX(0.0, timer.sample(), 0.1);
}

TEST_MAIN() { TEST_RUN_ALL(); }
