package com.mikescher.xamarinforms.resourcebuilder.converter;

import com.mikescher.xamarinforms.resourcebuilder.env.RunEnvironment;
import com.mikescher.xamarinforms.resourcebuilder.util.FileIO;
import com.mikescher.xamarinforms.resourcebuilder.util.ProcessHelper;
import com.mikescher.xamarinforms.resourcebuilder.util.SVGUtil;
import com.mikescher.xamarinforms.resourcebuilder.util.Tuple3;
import com.mikescher.xamarinforms.resourcebuilder.values.FittingType;

import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

public class VectorToAndroidConverter extends AbstractConverter {

    @Override
    protected double getInputWidthDouble(File file) throws Exception {
        return SVGUtil.getFloatWidthFromSVG(file);
    }

    @Override
    protected double getInputHeightDouble(File file) throws Exception {
        return SVGUtil.getFloatHeightFromSVG(file);
    }

    @Override
    protected void setMargin(int value) throws Exception {
        throw new Exception("This attribute is not allowed here");
    }

    @Override
    protected void setFitting(FittingType value) throws Exception {
        throw new Exception("This attribute is not allowed here");
    }

    @Override
    protected void setBackgroundColor(Color value) throws Exception {
        throw new Exception("This attribute is not allowed here");
    }

    @Override
    protected String getDisplayString() {
        return "XML @ " + OutputWidth + "x" + OutputHeight;
    }

    @Override
    protected File convert(RunEnvironment env) throws Exception {
        File f_tmp_dir = Paths.get(System.getProperty("java.io.tmpdir"), "xfrb_" + UUID.randomUUID()).toFile();
        if (!f_tmp_dir.mkdirs()) throw new Exception("Could not mkdir");
        f_tmp_dir.deleteOnExit();
        File f_tmp_file = Paths.get(f_tmp_dir.getAbsolutePath(), new File(InputFile).getName().replace(".svg", ".xml")).toFile();
        f_tmp_file.deleteOnExit();

        Tuple3<Integer, String, String> r;
        if (OutputWidth == InputWidth && OutputHeight == InputHeight) {
            r = ProcessHelper.procExec(env.VDTPath,
                    "-in", new File(InputFile).getAbsolutePath(),
                    "-out", f_tmp_file.getParent(),
                    "-c");
        } else {
            r = ProcessHelper.procExec(env.VDTPath,
                    "-in", new File(InputFile).getAbsolutePath(),
                    "-out", f_tmp_file.getParent(),
                    "-c",
                    "-widthDp", Integer.toString(OutputWidth),
                    "-heightDp", Integer.toString(OutputHeight));
        }

        if (r.Item1 != 0)  throw new Exception("vd-tool failed: \n\n" + r.Item1 + "\n\n" + r.Item2 + "\n\n" + r.Item3);

        String xnew = FileIO.readUTF8TextFile(f_tmp_file);
        if (!xnew.contains("<vector")) throw new Exception("vd-tool resulted in invalid output:\n" + xnew);

        return f_tmp_file;
    }
}
