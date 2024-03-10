package org.ds.simple.ink.launcher;

import org.ds.simple.ink.launcher.BuildConfig;

import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.io.IOException;

public class CrashHandler implements UncaughtExceptionHandler {
    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
      String backtrace = getBacktrace(e);

      File dir = new File(Environment.getExternalStorageDirectory(),
              "Crashes_" + BuildConfig.APPLICATION_ID);
      if (!dir.exists()) {
          dir.mkdirs();
      }
      SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss");
      Date date = new Date();
      String filename = dateFormat.format(date) + "_" + BuildConfig.APPLICATION_ID + "_stacktrace.txt";

      writeFile(dir, filename, backtrace);
    }

    private String getBacktrace(@NonNull Throwable e) {
        final Writer buffer = new StringWriter();
        final PrintWriter writer = new PrintWriter(buffer);
        e.printStackTrace(writer);
        String backtrace = buffer.toString();
        writer.close();

        return backtrace;
    }

    private void writeFile(File dir, String filename, String content) {
        File report = new File(dir, filename);
        try {
            FileWriter writer = new FileWriter(report);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e("ExceptionHandler", e.getMessage());
        }
    }
}
