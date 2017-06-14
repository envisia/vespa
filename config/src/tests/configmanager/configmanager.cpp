// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <vespa/vespalib/testkit/test_kit.h>
#include <vespa/vespalib/util/noncopyable.hpp>
#include <vespa/config/common/configmanager.h>
#include <vespa/config/common/exceptions.h>
#include <vespa/config/common/timingvalues.h>
#include <vespa/config/subscription/sourcespec.h>
#include <vespa/config/raw/rawsource.h>
#include "config-my.h"

using namespace config;

namespace {

    ConfigValue createValue(const std::string & myField, const std::string & md5)
    {
        std::vector< vespalib::string > lines;
        lines.push_back("myField \"" + myField + "\"");
        return ConfigValue(lines, md5);
    }

    struct TestContext
    {
        int numGetConfig;
        int numUpdate;
        int numClose;
        int64_t generation;
        bool respond;
        TestContext()
            : numGetConfig(0), numUpdate(0), numClose(0), generation(-1), respond(true)
        { }
    };

    class MySource : public Source
    {
    public:
        MySource(TestContext * data, const IConfigHolder::SP & holder) : _holder(holder), _data(data) { }
        void getConfig() override
        {
            _data->numGetConfig++;
            if (_data->respond) {
                _holder->handle(ConfigUpdate::UP(new ConfigUpdate(ConfigValue(), true, _data->generation)));
            }
        }
        void reload(int64_t generation) override
        {
            _data->numUpdate++;
            _data->generation = generation;
        }
        void close() override
        {
            _data->numClose++;
        }
        IConfigHolder::SP _holder;
        TestContext * _data;
    };

    class MySourceFactory : public SourceFactory
    {
    public:
        MySourceFactory(TestContext * d) : data(d) { }
        Source::UP createSource(const IConfigHolder::SP & holder, const ConfigKey & key) const override
        {
            (void) key;
            return Source::UP(new MySource(data, holder));
        }
        TestContext * data;
    };

    class MySpec : public SourceSpec
    {
    public:
        MySpec(TestContext * data)
            : _key("foo"),
              _data(data)
        {
        }
        SourceSpecKey createKey() const { return SourceSpecKey(_key); }
        SourceFactory::UP createSourceFactory(const TimingValues & timingValues) const override {
            (void) timingValues;
            return SourceFactory::UP(new MySourceFactory(_data));
        }
        SourceSpec * clone() const { return new MySpec(*this); }
    private:
        const std::string _key;
        TestContext * _data;
    };

    static TimingValues testTimingValues(
            2000,  // successTimeout
            500,  // errorTimeout
            500,   // initialTimeout
            4000,  // unsubscribeTimeout
            0,     // fixedDelay
            250,   // successDelay
            250,   // unconfiguredDelay
            500,   // configuredErrorDelay
            5,
            1000,
            2000);    // maxDelayMultiplier

    class ManagerTester {
    public:
        ConfigKey key;
        ConfigManager _mgr;
        ConfigSubscription::SP sub;

        ManagerTester(const ConfigKey & k, const MySpec & s);
        ~ManagerTester();

        void subscribe()
        {
            sub = _mgr.subscribe(key, 5000);
        }
    };

    ManagerTester::ManagerTester(const ConfigKey & k, const MySpec & s)
        : key(k),
          _mgr(s.createSourceFactory(testTimingValues), 1)
    { }
    ManagerTester::~ManagerTester() { }

}

TEST("requireThatSubscriptionTimesout") {
    const ConfigKey key(ConfigKey::create<MyConfig>("myid"));
    const ConfigValue testValue(createValue("l33t", "a"));

    { // No valid response
        TestContext data;
        data.respond = false;

        ManagerTester tester(ConfigKey::create<MyConfig>("myid"), MySpec(&data));
        bool thrown = false;
        try {
            tester.subscribe();
        } catch (const ConfigRuntimeException & e) {
            thrown = true;
        }
        ASSERT_TRUE(thrown);
        ASSERT_EQUAL(1, data.numGetConfig);
    }
}
TEST("requireThatSourceIsAskedForRequest") {
    TestContext data;
    const ConfigKey key(ConfigKey::create<MyConfig>("myid"));
    const ConfigValue testValue(createValue("l33t", "a"));
    try {
        ManagerTester tester(key, MySpec(&data));
        tester.subscribe();
        ASSERT_EQUAL(1, data.numGetConfig);
    } catch (ConfigRuntimeException & e) {
        ASSERT_TRUE(false);
    }
    ASSERT_EQUAL(1, data.numClose);
}

TEST("require that new sources are given the correct generation") {
    TestContext data;
    const ConfigKey key(ConfigKey::create<MyConfig>("myid"));
    const ConfigValue testValue(createValue("l33t", "a"));
    try {
        ManagerTester tester(key, MySpec(&data));
        tester._mgr.reload(30);
        tester.subscribe();
        ASSERT_EQUAL(30, data.generation);
    } catch (ConfigRuntimeException & e) {
        ASSERT_TRUE(false);
    }
}

TEST_MAIN() { TEST_RUN_ALL(); }
