package com.mikescher.xamarinforms.resourcebuilder;

import java.io.File;
import java.util.HashMap;

public interface IConverter {
    File run(File input, HashMap<String, String> parameter) throws Exception;
}
