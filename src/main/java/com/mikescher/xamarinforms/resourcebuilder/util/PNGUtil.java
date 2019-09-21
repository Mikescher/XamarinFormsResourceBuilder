package com.mikescher.xamarinforms.resourcebuilder.util;

import javax.imageio.ImageIO;
import java.io.File;

public class PNGUtil {

    public static int getWidthFromPNG(File f) throws Exception
    {
        return ImageIO.read(f).getWidth();
    }

    public static int getHeightFromPNG(File f) throws Exception
    {
        return ImageIO.read(f).getHeight();
    }

    public static int getWidthFromPNG(String path) throws Exception
    {
        return ImageIO.read(new File(path)).getWidth();
    }

    public static int getHeightFromPNG(String path) throws Exception
    {
        return ImageIO.read(new File(path)).getHeight();
    }

    public static int calcAutoWidthFromPNG(String path, int height) throws Exception
    {
        double png_width = getWidthFromPNG(path);
        double png_height = getHeightFromPNG(path);

        return (int)Math.round((png_width/png_height) * height);
    }

    public static int calcAutoHeightFromPNG(String path, int width) throws Exception
    {
        double png_width = getWidthFromPNG(path);
        double png_height = getHeightFromPNG(path);

        return (int)Math.round((png_height/png_width) * width);
    }
}
