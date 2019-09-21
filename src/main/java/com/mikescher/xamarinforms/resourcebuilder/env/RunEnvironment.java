package com.mikescher.xamarinforms.resourcebuilder.env;

import com.mikescher.xamarinforms.resourcebuilder.util.FileIO;
import com.mikescher.xamarinforms.resourcebuilder.util.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class RunEnvironment
{
    public File LockFile;
    public HashMap<Tuple2<String, String>, String> LockDataFile = new HashMap<>();
    public HashMap<Tuple2<String, String>, String> LockDataNew  = new HashMap<>();

    public String RunDirectory;
    public String RelativeOutputRoot;

    public String VDTPath;
    public String SVGRasterizer;

    public long StartTime;

    public int Count_NotNeeded = 0;
    public int Count_Changed   = 0;
    public int Count_Created   = 0;
    public int Count_NoChanges = 0;

    public void Start()
    {
        StartTime = System.currentTimeMillis();
    }

    public void Init(String parent, Element root) throws IOException
    {
        RunDirectory       = parent;
        RelativeOutputRoot = root.getAttribute("outputroot");

        VDTPath = System.getProperty("os.name").toLowerCase().contains("win")
                ? getPathInRunDirectory(root.getAttribute("vd-tool-win"))
                : getPathInRunDirectory(root.getAttribute("vd-tool-nix"));

        SVGRasterizer = root.hasAttribute("svg_rasterizer")
                ? root.getAttribute("svg_rasterizer")
                : "Unset";

        if (root.hasAttribute("lockfile"))
        {
            LockFile = Paths.get(RunDirectory, root.getAttribute("lockfile")).toFile();
            loadLockData();
        }
    }

    private void loadLockData() throws IOException
    {
        LockDataFile = new HashMap<>();
        if (LockFile.exists())
        {
            for (String s : FileIO.readUTF8TextFileLines(LockFile.getAbsolutePath()))
            {
                String[] arr = s.split("\t");
                LockDataFile.put(Tuple2.Create(arr[0].trim(), arr[1].trim()), arr[2].trim());
            }
        }
    }

    public String getPathInRunDirectory(String path)
    {
        return Paths.get(RunDirectory, path).toAbsolutePath().toString();
    }

    public String getPathInOutputDirectory(String path)
    {
        return Paths.get(RunDirectory, RelativeOutputRoot, path).toAbsolutePath().toString() ;
    }

    public void writeLockFile() throws IOException
    {
        if (LockFile == null) return;

        System.out.println("[WRITING LOCK]");
        {
            int p1 = 3 + LockDataNew.keySet().stream().map(s -> s.Item1.length()).max(Integer::compareTo).orElse(0);
            int p2 = 3 + LockDataNew.keySet().stream().map(s -> s.Item2.length()).max(Integer::compareTo).orElse(0);

            @SuppressWarnings("unchecked")
            List<Map.Entry<Tuple2<String, String>, String>> ld_sorted = LockDataNew
                    .entrySet()
                    .stream()
                    .sorted(
                            Comparator
                                    .comparing(e -> ((Map.Entry<Tuple2<String, String>, String>)e).getKey().Item1)
                                    .thenComparing(e -> ((Map.Entry<Tuple2<String, String>, String>)e).getKey().Item2))
                    .collect(Collectors.toList());

            StringBuilder lockdatabuilder = new StringBuilder();
            for (Map.Entry<Tuple2<String, String>, String> e : ld_sorted)
                lockdatabuilder
                        .append(StringUtils.rightPad(e.getKey().Item1, p1))
                        .append("\t")
                        .append(StringUtils.rightPad(e.getKey().Item2, p2))
                        .append("\t")
                        .append(e.getValue()).append("\n");

            FileIO.writeTextFile(LockFile, lockdatabuilder.toString());
        }
    }

    public void writeResult() {

        System.out.println();
        System.out.println("[RESULT]");
        System.out.println();

        System.out.println("Update not needed (by lockfile): " + Count_NotNeeded);
        System.out.println("File changed:                    " + Count_Changed);
        System.out.println("New file created (first run):    " + Count_Created);
        System.out.println("File unchanged (checksum match): " + Count_NoChanges);
        System.out.println();
        System.out.println("Duration: " + ((System.currentTimeMillis() - StartTime) / 1000) + " sec");
    }

    public void addLockData(String rawinput, String rawoutput, String cs) {
        LockDataNew.put(Tuple2.Create(rawinput, rawoutput), cs);
    }
}
