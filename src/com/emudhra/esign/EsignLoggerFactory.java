/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

/**
 *
 * @author 20476
 */
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class EsignLoggerFactory {

    private static Logger logger;

    private EsignLoggerFactory() {
    }

    public static Logger getLogger(Class<?> caller) {
        return getLogger(caller, null, eSignSettings.LogType.AllLog);
    }

    public static Logger getLogger(Class<?> caller, String logFolder, eSignSettings.LogType logType) {
        try {

            if (logger != null) {

                return logger;
            }
            if (logType == null) {
                logType = eSignSettings.LogType.AllLog;
            }
            if (logFolder == null) {
                logFolder = System.getProperty("user.dir") + File.separator;
            }
            if (logType == null) {
                throw new NullPointerException();
            }
            Level level = Level.ALL;
            switch (logType) {
                case AllLog:
                    break;
                case NoDebugLog:
                    level = Level.WARNING;
                    break;
                case NoLog:
                    level = Level.OFF;
                    break;
                default:
                    break;
            }
            String dirName = logFolder + "logs";
            File directory = new File(dirName);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String fileName = dirName + File.separator + "eSign.log";
            FileHandler fileHandler = new FileHandler(fileName, 10 * 1024 * 1024, 100, true);
            fileHandler.setLevel(Level.INFO);
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("IST"));
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    String time = dateFormat.format(new Date(record.getMillis()));
                    String log = "" + time + "" + "\t"
                            + "[" + record.getLevel() + "]" + "\t"
                            + "[" + record.getSourceClassName() + "]" + "\t"
                            //                            + "[" + record.getSourceMethodName() + "]" + "  "
                            + "\t" + record.getMessage() + "\n";
                    return log;
                }
            });

            logger = Logger.getLogger("esign");
            logger.addHandler(fileHandler);
            logger.setLevel(level);
            logger.setUseParentHandlers(false);
            return logger;

        } catch (IOException | NullPointerException | SecurityException e) {
            return Logger.getLogger(caller.getName());
        }
    }
}
