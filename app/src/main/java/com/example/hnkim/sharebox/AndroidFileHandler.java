package com.example.hnkim.sharebox;

/**
 * Created by hnkim on 2016-12-21.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ESRENLL on 2016-12-16.
 */

public class AndroidFileHandler {
    public static boolean copyDir(File srcFile, File dstFile) {
        if(dstFile.getAbsolutePath().contains(srcFile.getAbsolutePath()))
            return false;
        if(!dstFile.exists())
            if(!dstFile.mkdir()) return false;
        for(File csf : srcFile.listFiles()) {
            String cdp = csf.getAbsolutePath();
            cdp = dstFile + cdp.substring(cdp.lastIndexOf('/'));
            copy(csf, new File(cdp));
        }
        return true;
    }
    public static boolean copy(File srcFile, File dstFile) {
        InputStream in = null;
        OutputStream out = null;
        try {
            //create output directory if it doesn't exist
            if(!srcFile.exists() || dstFile.exists())
                return false;

            if(srcFile.isDirectory())
                return copyDir(srcFile, dstFile);
            else {
                in = new FileInputStream(srcFile.getAbsolutePath());
                out = new FileOutputStream(dstFile.getAbsolutePath());

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                in = null;

                // write the output file
                out.flush();
                out.close();
                out = null;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    public static boolean remove(File file) {
        if(!file.exists()) return false;
        if(file.isDirectory()) {
            for(File cf : file.listFiles()) {
                if(!remove(cf))
                    return false;
            }
        }
        return file.delete();
    }
    public static boolean move(File srcFile, File dstFile) {
        if(!srcFile.exists() || dstFile.exists())
            return false;
        return srcFile.renameTo(dstFile);
    }
    public static boolean rename(File file, String name) {
        if(!file.exists())
            return false;
        String path = file.getAbsolutePath();
        int endIdx = path.lastIndexOf('/');
        path = path.substring(0, endIdx) + "/" + name;
        File dstFile = new File(path);
        if(dstFile.exists())
            return false;
        return file.renameTo(dstFile);
    }
}
