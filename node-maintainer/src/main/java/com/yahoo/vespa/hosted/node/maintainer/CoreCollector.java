// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.maintainer;

import com.yahoo.collections.Pair;
import com.yahoo.system.ProcessExecuter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes in a compressed (lz4) or uncompressed core dump and collects relevant metadata.
 *
 * @author freva
 */
public class CoreCollector {
    private static final String GDB_PATH = "/home/y/bin64/gdb";
    private static final Pattern CORE_GENERATOR_PATH_PATTERN = Pattern.compile("^Core was generated by `(?<path>.*?)'.$");
    private static final Pattern EXECFN_PATH_PATTERN = Pattern.compile("^.* execfn: '(?<path>.*?)'");
    private static final Pattern FROM_PATH_PATTERN = Pattern.compile("^.* from '(?<path>.*?)'");
    private static final Pattern TOTAL_MEMORY_PATTERN = Pattern.compile("^MemTotal:\\s*(?<totalMem>\\d+) kB$", Pattern.MULTILINE);

    private static final Logger logger = Logger.getLogger(CoreCollector.class.getName());
    private final ProcessExecuter processExecuter;

    public CoreCollector(ProcessExecuter processExecuter) {
        this.processExecuter = processExecuter;
    }

    List<String> readYinstState(Path yinstStatePath) throws IOException {
        Pair<Integer, String> result = processExecuter.exec(new String[]{"cat", yinstStatePath.toString()});

        if (result.getFirst() != 0) {
            throw new RuntimeException("Failed to read yinst state file at: " + yinstStatePath + ", result: " + result);
        }
        return Arrays.asList(result.getSecond().split("\n"));
    }

    List<String> readRpmPackages() throws IOException {
        Pair<Integer, String> result = processExecuter.exec(new String[]{"rpm", "-qa"});

        if (result.getFirst() != 0) {
            throw new RuntimeException("Failed to read RPM packages " + result);
        }
        return Arrays.asList(result.getSecond().split("\n"));
    }
    
    Path readBinPathFallback(Path coredumpPath) throws IOException, InterruptedException {
        String command = GDB_PATH + " -n -batch -core " + coredumpPath + " | grep \'^Core was generated by\'";
        Pair<Integer, String> result = processExecuter.exec(new String[]{"sh", "-c", command});

        Matcher matcher = CORE_GENERATOR_PATH_PATTERN.matcher(result.getSecond());
        if (! matcher.find()) {
            throw new RuntimeException("Failed to extract binary path from " + result);
        }
        return Paths.get(matcher.group("path").split(" ")[0]);
    }

    Path readBinPath(Path coredumpPath) throws IOException, InterruptedException {
        try {
            Pair<Integer, String> result = processExecuter.exec(new String[]{"file", coredumpPath.toString()});

            Matcher execfnMatcher = EXECFN_PATH_PATTERN.matcher(result.getSecond());
            if (execfnMatcher.find()) {
                return Paths.get(execfnMatcher.group("path").split(" ")[0]);
            }

            Matcher fromMatcher = FROM_PATH_PATTERN.matcher(result.getSecond());
            if (fromMatcher.find()) {
                return Paths.get(fromMatcher.group("path").split(" ")[0]);
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Failed getting bin path, trying fallback instead", e);
        }

        return readBinPathFallback(coredumpPath);
    }

    List<String> readBacktrace(Path coredumpPath, Path binPath, boolean allThreads) throws IOException, InterruptedException {
        String threads = allThreads ? "thread apply all bt" : "bt";
        Pair<Integer, String> result = processExecuter.exec(
                new String[]{GDB_PATH, "-n", "-ex", threads, "-batch", binPath.toString(), coredumpPath.toString()});
        if (result.getFirst() != 0) {
            throw new RuntimeException("Failed to read backtrace " + result);
        }
        return Arrays.asList(result.getSecond().split("\n"));
    }

    Map<String, Object> collect(Path coredumpPath, Optional<Path> yinstStatePath) {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            coredumpPath = compressCoredump(coredumpPath);
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Failed compressing/decompressing core dump", e);
        }

        try {
            Path binPath = readBinPath(coredumpPath);

            data.put("bin_path", binPath.toString());
            data.put("backtrace", readBacktrace(coredumpPath, binPath, false));
            data.put("backtrace_all_threads", readBacktrace(coredumpPath, binPath, true));
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Failed to extract backtrace", e);
        }

        yinstStatePath.ifPresent(yinstState -> {
            try {
                data.put("yinst_state", readYinstState(yinstState));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to read yinst state", e);
            }

            try {
                data.put("rpm_packages", readRpmPackages());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to read RPM packages", e);
            }
        });

        try {
            deleteDecompressedCoredump(coredumpPath);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete decompressed core dump", e);
        }
        return data;
    }


    /**
     * This method will either compress or decompress the core dump if the input path is to a decompressed or
     * compressed core dump, respectively.
     *
     * @return Path to the decompressed core dump
     */
    private Path compressCoredump(Path coredumpPath) throws IOException, InterruptedException {
        if (! coredumpPath.toString().endsWith(".lz4")) {
            processExecuter.exec(
                    new String[]{"/home/y/bin64/lz4", coredumpPath.toString(), coredumpPath.toString() + ".lz4"});
            return coredumpPath;

        } else {
            if (!diskSpaceAvailable(coredumpPath)) {
                throw new RuntimeException("Not decompressing " + coredumpPath + " due to not enough disk space available");
            }

            Path decompressedPath = Paths.get(coredumpPath.toString().replaceFirst("\\.lz4$", ""));
            Pair<Integer, String> result = processExecuter.exec(
                    new String[]{"/home/y/bin64/lz4", "-d", coredumpPath.toString(), decompressedPath.toString()});
            if (result.getFirst() != 0) {
                throw new RuntimeException("Failed to decompress file " + coredumpPath + ": " + result);
            }
            return decompressedPath;
        }
    }

    /**
     * Delete the core dump unless:
     * - The file is compressed
     * - There is no compressed file (i.e. it was not decompressed in the first place)
     */
    void deleteDecompressedCoredump(Path coredumpPath) throws IOException {
        if (! coredumpPath.toString().endsWith(".lz4") && Paths.get(coredumpPath.toString() + ".lz4").toFile().exists()) {
            Files.delete(coredumpPath);
        }
    }

    private boolean diskSpaceAvailable(Path path) throws IOException {
        // TODO: If running inside container, check against container memory size, not for the enitre host
        String memInfo = new String(Files.readAllBytes(Paths.get("/proc/meminfo")));
        return path.toFile().getFreeSpace() > parseTotalMemorySize(memInfo);
    }

    int parseTotalMemorySize(String memInfo) {
        Matcher matcher = TOTAL_MEMORY_PATTERN.matcher(memInfo);
        if (!matcher.find()) throw new RuntimeException("Could not parse meminfo: " + memInfo);
        return Integer.valueOf(matcher.group("totalMem"));
    }
}
