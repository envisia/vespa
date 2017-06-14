// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vespa/vespalib/io/fileutil.h>
#include <vespa/vespalib/stllike/string.h>

namespace search {
namespace test {

class DirectoryHandler
{
private:
    vespalib::string _mkdir;
    vespalib::string _rmdir;
    bool             _cleanup;

public:
    DirectoryHandler(const vespalib::string &mkdir)
        : _mkdir(mkdir),
          _rmdir(mkdir),
          _cleanup(true)
    {
        vespalib::mkdir(_mkdir);
    }
    DirectoryHandler(const vespalib::string &mkdir,
                     const vespalib::string &rmdir)
        : _mkdir(mkdir),
          _rmdir(rmdir),
          _cleanup(true)
    {
        vespalib::mkdir(_mkdir);
    }
    ~DirectoryHandler() {
        if (_cleanup) {
            vespalib::rmdir(_rmdir, true);
        }
    }
    void cleanup(bool v) { _cleanup = v; }
};

}
}

