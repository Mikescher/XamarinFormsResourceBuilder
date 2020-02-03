package com.mikescher.xamarinforms.resourcebuilder.converter;

import com.mikescher.xamarinforms.resourcebuilder.Main;
import com.mikescher.xamarinforms.resourcebuilder.env.RunEnvironment;
import com.mikescher.xamarinforms.resourcebuilder.util.FileIO;
import com.mikescher.xamarinforms.resourcebuilder.values.Tuple2;
import com.mikescher.xamarinforms.resourcebuilder.values.FittingType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractConverter
{
    public double InputWidth;
    public double InputHeight;

    public String SuppliedOutputWidth;
    public String SuppliedOutputHeight;

    public int OutputWidth;
    public int OutputHeight;

    public String SuppliedInputFile;
    public String SuppliedOutputFile;

    public String InputFile;
    public String OutputFile;

    public void run(RunEnvironment env, String sourcepath, String suppliedSourcePath, Element outputXMLNode) throws Exception
    {
        InputFile         = sourcepath;

        SuppliedInputFile  = suppliedSourcePath;
        SuppliedOutputFile = outputXMLNode.getAttribute("path");

        InputWidth  = getInputWidthDouble(new File(sourcepath));
        InputHeight = getInputHeightDouble(new File(sourcepath));


        if (outputXMLNode.hasAttribute("width"))
            setWidth(outputXMLNode.getAttribute("width"));
        else
            SuppliedOutputWidth = Integer.toString((int)Math.round(InputWidth));

        if (outputXMLNode.hasAttribute("height"))
            setHeight(outputXMLNode.getAttribute("height"));
        else
            SuppliedOutputHeight = Integer.toString((int)Math.round(InputHeight));

        calculateWidthAndHeight();

        if (outputXMLNode.hasAttribute("fitting"))          setFitting(FittingType.get(outputXMLNode.getAttribute("fitting")));
        if (outputXMLNode.hasAttribute("margin"))           setMargin(Integer.parseInt(outputXMLNode.getAttribute("margin")));
        if (outputXMLNode.hasAttribute("background_color")) setBackgroundColor(decodeColor(outputXMLNode.getAttribute("background_color")));

        OutputFile = doPathReplacements(env.getPathInOutputDirectory(SuppliedOutputFile));

        if (!new File(InputFile).exists()) throw new Exception("File '" + InputFile + "' does not exist");

        String display = StringUtils.rightPad(getDisplayString(), 24);

        if (checkLockData(env))
        {
            System.out.println("[ ] " + display + "  --  Not needed");
            updateLockData(env);
            env.Count_NotNeeded++;
            return;
        }

        File intermediateFile = convert(env);

        String xold = new File(OutputFile).exists() ? FileIO.cs(new File(OutputFile)) : "";
        String xnew = FileIO.cs(intermediateFile);

        if (xnew.length() < 8)
        {
            throw new Exception("Conversion resulted in empty file");
        }
        else if (xold.isEmpty())
        {
            if (new File(OutputFile).exists() && !new File(OutputFile).delete()) throw new Exception("File delete failed");
            if (!intermediateFile.renameTo(new File(OutputFile))) throw new Exception("File renameTo failed");
            System.out.println("[#] " + display + "  --  File created");
            updateLockData(env);
            env.Count_Created++;
        }
        else if (xold.equals(xnew))
        {
            if (!intermediateFile.delete()) throw new Exception("File delete failed");
            System.out.println("[/] " + display + "  --  No changes");
            updateLockData(env);
            env.Count_NoChanges++;
        }
        else
        {
            if (!new File(OutputFile).delete()) throw new Exception("File delete failed");
            if (! intermediateFile.renameTo(new File(OutputFile))) throw new Exception("File renameTo failed");
            System.out.println("[#] " + display + "  --  File changed");
            updateLockData(env);
            env.Count_Changed++;
        }
    }

    private Color decodeColor(String str) throws NoSuchFieldException, IllegalAccessException {
        String inv = str;

        if (!str.startsWith("#")) return (Color)Color.class.getField(str.toLowerCase()).get(null);
        str = str.substring(1);

        if (str.length() == 3) {
            str = ""+str.charAt(0)+""+str.charAt(0)+""+str.charAt(1)+""+str.charAt(1)+""+str.charAt(2)+""+str.charAt(2)+"FF";
        } else if (str.length() == 6) {
            str = str+"FF";
        }

        if (str.length() == 8) {

            int r = Integer.parseInt(str.substring(0, 2), 16);
            int g = Integer.parseInt(str.substring(2, 4), 16);
            int b = Integer.parseInt(str.substring(4, 6), 16);
            int a = Integer.parseInt(str.substring(6, 8), 16);

            return new Color(r, g, b, a);

        } else {
            throw new IllegalArgumentException("Value '"+inv+"' is not a valid color");
        }


    }

    protected void setHeight(String value) throws Exception {
        SuppliedOutputHeight = value;
    }

    protected void setWidth(String value) throws Exception {
        SuppliedOutputWidth = value;
    }

    private void updateLockData(RunEnvironment env) throws IOException {
        env.addLockData(SuppliedInputFile, SuppliedOutputFile, calculateLockChecksum());
    }

    private void calculateWidthAndHeight() throws Exception
    {
        if (SuppliedOutputWidth.equalsIgnoreCase("auto") && SuppliedOutputHeight.equalsIgnoreCase("auto")) {
            OutputWidth  = (int)Math.round(InputWidth);
            OutputHeight = (int)Math.round(InputHeight);
        } else if (SuppliedOutputWidth.equalsIgnoreCase("auto")) {
            OutputHeight = Integer.parseInt(SuppliedOutputHeight);
            OutputWidth  = (int)Math.round((InputWidth / InputHeight) * OutputHeight);
        } else if (SuppliedOutputHeight.equalsIgnoreCase("auto")) {
            OutputWidth  = Integer.parseInt(SuppliedOutputWidth);
            OutputHeight = (int)Math.round((InputHeight / InputWidth) * OutputWidth);
        } else {
            OutputWidth  = Integer.parseInt(SuppliedOutputWidth);
            OutputHeight = Integer.parseInt(SuppliedOutputHeight);
        }
    }

    private String doPathReplacements(String outputpath)
    {
        outputpath = outputpath.replace("{filename}",       FilenameUtils.getBaseName(InputFile));
        outputpath = outputpath.replace("{originalwidth}",  "" + (int)Math.round(InputWidth));
        outputpath = outputpath.replace("{originalheight}", "" + (int)Math.round(InputHeight));
        outputpath = outputpath.replace("{width}",          "" + OutputWidth);
        outputpath = outputpath.replace("{height}",         "" + OutputHeight);

        return outputpath;
    }

    private boolean checkLockData(RunEnvironment env) throws Exception
    {
        if (!new File(InputFile).exists())  return false;
        if (!new File(OutputFile).exists()) return false;

        String dat = env.LockDataFile.get(Tuple2.Create(SuppliedInputFile, SuppliedOutputFile));
        if (dat == null) return false;

        String cs = calculateLockChecksum();

        return cs.equals(dat);
    }

    private String calculateLockChecksum() throws IOException
    {
        File f1 = new File(InputFile);
        File f2 = new File(OutputFile);

        if (!f1.exists()) return "";
        if (!f2.exists()) return "";

        String cs1 = FileIO.cs(f1);
        String cs2 = FileIO.cs(f2);

        StringBuilder b = new StringBuilder();

        List<Map.Entry<String, String>> parameter = listParameterForChecksum()
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(Collectors.toList());

        for (Map.Entry<String, String> s : parameter)
        {
            b.append(s.getKey()).append("\t").append(s.getValue()).append("\n");
        }

        String cs3 = FileIO.cs(b.toString());

        return FileIO.cs(cs1 + cs2 + cs3 + Main.LOCK_VERSION); // [ INPUT, OUTPUT, PARAM, BINARY ]
    }

    protected HashMap<String, String> listParameterForChecksum()
    {
        HashMap<String, String> data = new HashMap<>();

        data.put("width", Integer.toString(OutputWidth));
        data.put("height", Integer.toString(OutputHeight));

        return data;
    }

    protected String colorToString(Color col) {
        return MessageFormat.format("[{0}|{1}|{2} - {3}]", col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
    }

    protected abstract double getInputWidthDouble(File file) throws Exception;
    protected abstract double getInputHeightDouble(File file) throws Exception;

    protected abstract void setMargin(int value) throws Exception;
    protected abstract void setFitting(FittingType value) throws Exception;
    protected abstract void setBackgroundColor(Color value) throws Exception;

    protected abstract String getDisplayString();
    protected abstract File convert(RunEnvironment env) throws Exception;
}
