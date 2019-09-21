package com.mikescher.xamarinforms.resourcebuilder.values;

public enum FittingType
{
    STRETCH("stretch"),
    CENTER("center");

    public final String Text;

    FittingType(String t) {
        Text = t;
    }

    public static FittingType get(String txt) throws Exception
    {
        for (FittingType ft : values())
        {
            if (ft.Text.equalsIgnoreCase(txt)) return ft;
        }

        throw new Exception("Unknown Fitting: " + txt);
    }

    public IntRect calcInnerRect(int margin, double inputWidth, double inputHeight, int outputWidth, int outputHeight) throws Exception
    {
        switch (this)
        {
            case STRETCH:
            {
                int x = margin;
                int y = margin;
                int w = outputWidth  - 2 * margin;
                int h = outputHeight - 2 * margin;
                return new IntRect(x, y, w, h);
            }

            case CENTER:
            {
                double relInput  = (inputWidth * 1.0)  / (inputHeight * 1.0);
                double relOutput = (outputWidth - 2.0 * margin) / (outputHeight - 2.0 * margin);
                if (relInput > relOutput)
                {
                    int w = outputWidth - 2 * margin;
                    int h = (int)Math.round(w/relInput);
                    int x = (outputWidth - w) / 2;
                    int y = (outputHeight - h) / 2;
                    return new IntRect(x, y, w, h);
                }
                else
                {
                    int h = outputHeight - 2 * margin;
                    int w = (int)Math.round(relInput*h);
                    int x = (outputWidth - w) / 2;
                    int y = (outputHeight - h) / 2;
                    return new IntRect(x, y, w, h);
                }
            }
        }

        throw new Exception("OutOfEnum");
    }
}
