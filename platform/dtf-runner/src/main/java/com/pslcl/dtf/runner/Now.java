package com.pslcl.dtf.runner;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Now {

    static public String time() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

}
