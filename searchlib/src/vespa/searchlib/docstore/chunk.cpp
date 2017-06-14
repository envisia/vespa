// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "chunk.h"
#include "chunkformats.h"
#include <vespa/vespalib/stllike/hash_map.hpp>

namespace search {

LidMeta
Chunk::append(uint32_t lid, const void * buffer, size_t len)
{
    vespalib::nbostream & os = getData();
    size_t oldSz(os.size());
    os << lid << static_cast<uint32_t>(len);
    os.write(buffer, len);
    _lids.push_back(Entry(lid, len, oldSz));
    return LidMeta(lid, len);
}

ssize_t
Chunk::read(uint32_t lid, vespalib::DataBuffer & buffer) const
{
    vespalib::ConstBufferRef buf = getLid(lid);
    if (buf.size() != 0) {
        buffer.writeBytes(buf.c_str(), buf.size());
    }
    return buf.size();
}

bool
Chunk::hasRoom(size_t len) const
{
    const size_t HeaderSize(2*sizeof(uint32_t));
    const size_t TrailerSize(sizeof(uint64_t));
    // To avoid read races during compacting These buffers must be preallocated.
    // There is always room for at least one element.
    // There is also room as long as neither _lids[] nor _dataBuf[] require reallocation.
    // Remember to account for Header and Trailer space requirement.
    const vespalib::nbostream & os = getData();
    return _lids.empty()
           || (((HeaderSize + TrailerSize + os.size() + len) <= os.capacity())
               && ((_lids.size() + 1) <= _lids.capacity()));
}

size_t
Chunk::getMaxPackSize(const document::CompressionConfig & compression) const {
    return _format->getMaxPackSize(compression);
}

void
Chunk::pack(uint64_t lastSerial, vespalib::DataBuffer & compressed, const document::CompressionConfig & compression)
{
    _lastSerial = lastSerial;
    _format->pack(_lastSerial, compressed, compression);
}

Chunk::Chunk(uint32_t id, const Config & config) :
    _id(id),
    _nextOffset(0),
    _lastSerial(static_cast<uint64_t>(-1l)),
    _format(new ChunkFormatV2(config.getMaxBytes()))
{
    _lids.reserve(4096/sizeof(Entry));
}

Chunk::Chunk(uint32_t id, const void * buffer, size_t len, bool skipcrc) :
    _id(id),
    _nextOffset(0),
    _lastSerial(static_cast<uint64_t>(-1l)),
    _format(ChunkFormat::deserialize(buffer, len, skipcrc))
{
    vespalib::nbostream &os = getData();
    while (os.size() > sizeof(_lastSerial)) {
        uint32_t sz(0);
        uint32_t lid(0);
        ssize_t oldRp(os.rp());
        os >> lid >> sz;
        os.adjustReadPos(sz);
        _lids.push_back(Entry(lid, sz, oldRp));
    }
    os >> _lastSerial;
}

Chunk::~Chunk() { }

vespalib::ConstBufferRef
Chunk::getLid(uint32_t lid) const
{
    vespalib::ConstBufferRef buf;
    for (LidList::const_iterator it(_lids.begin()), mt(_lids.end()); it != mt; it++) {
        if (it->getLid() == lid) {
#if 1
            uint32_t bLid(0), bLen(0);
            vespalib::nbostream is(getData().c_str()+it->getOffset(), it->size());
            is >> bLid >> bLen;
            assert(bLid == lid);
            assert(bLen == it->netSize());
            assert((bLen + 2*sizeof(uint32_t)) == it->size());
#endif
            buf = vespalib::ConstBufferRef(getData().c_str() + it->getNetOffset(), it->netSize());
        }
    }
    return buf;
}

size_t
Chunk::size() const {
    return getData().size();
}

const vespalib::nbostream &
Chunk::getData() const {
    return _format->getBuffer();
}

vespalib::nbostream &
Chunk::getData() {
    return _format->getBuffer();
}

Chunk::LidList
Chunk::getUniqueLids() const
{
    vespalib::hash_map<uint32_t, Entry> last;
    for (const Entry & e : _lids) {
        last[e.getLid()] = e;
    }
    LidList unique;
    unique.reserve(last.size());
    for (auto it(last.begin()), mt(last.end()); it != mt; it++) {
        unique.push_back(it->second);
    }
    return unique;
}

MemoryUsage
Chunk::getMemoryUsage() const
{
    MemoryUsage result;
    result.incAllocatedBytes(_format->getBuffer().capacity());
    result.incUsedBytes(_format->getBuffer().size());
    result.incAllocatedBytes(sizeof(Entry) * _lids.capacity());
    result.incUsedBytes(sizeof(Entry) * _lids.size());
    return result;
}

vespalib::nbostream &
ChunkMeta::deserialize(vespalib::nbostream & is)
{
    return is >> _offset >> _size >> _lastSerial >> _numEntries;
}

vespalib::nbostream &
ChunkMeta::serialize(vespalib::nbostream & os) const
{
    return os << _offset << _size << _lastSerial << _numEntries;
}

vespalib::nbostream &
LidMeta::deserialize(vespalib::nbostream & is)
{
    return is >> _lid >> _size;
}

vespalib::nbostream &
LidMeta::serialize(vespalib::nbostream & os) const
{
    return os << _lid << _size;
}

} // namespace search
