/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logger factory for the eSign SDK.
 *
 * <p>Strategy:
 * <ol>
 *   <li>On first call a ConsoleHandler is attached immediately — this works on
 *       every platform including Android (output appears in logcat).</li>
 *   <li>When {@link #initFileHandler(String)} is called (triggered by
 *       {@code getGatewayParameter} once a writable {@code tempFolder} is
 *       known), a FileHandler is added and the ConsoleHandler is removed.
 *       The log file is written to {@code <logFolder>/eSign.log}.</li>
 *   <li>If the FileHandler cannot be created (permission denied, etc.) the
 *       ConsoleHandler remains active so logs are never silently lost.</li>
 * </ol>
 */
public class EsignLoggerFactory {

    private static Logger logger;
    private static boolean fileHandlerAttached = false;
    private static Level currentLevel = Level.ALL;

    private EsignLoggerFactory() {
    }

    public static Logger getLogger(Class<?> caller) {
        return getLogger(caller, null, eSignSettings.LogType.AllLog);
    }

    /**
     * Returns (or creates) the SDK-wide logger.
     *
     * @param caller    calling class (used only when logger creation fails)
     * @param logFolder optional folder for the log file; pass {@code null} to
     *                  defer file logging until {@link #initFileHandler} is called
     * @param logType   desired log level
     */
    public static Logger getLogger(Class<?> caller, String logFolder, eSignSettings.LogType logType) {
        if (logType == null) {
            logType = eSignSettings.LogType.AllLog;
        }
        currentLevel = toLevel(logType);

        if (logger == null) {
            logger = Logger.getLogger("esign");
            logger.setLevel(currentLevel);
            logger.setUseParentHandlers(false);
            // Start with a ConsoleHandler so logs are always visible (logcat on Android).
            addConsoleHandler(currentLevel);
        } else {
            // Level may have changed — update the existing logger and its handlers.
            logger.setLevel(currentLevel);
            Level handlerLevel = effectiveHandlerLevel(currentLevel);
            for (Handler h : logger.getHandlers()) {
                h.setLevel(handlerLevel);
            }
        }

        // If a folder was supplied at construction time, try to attach a file handler now.
        if (!fileHandlerAttached && logFolder != null) {
            tryAttachFileHandler(logFolder, currentLevel);
        }

        return logger;
    }

    /**
     * Attaches a {@link FileHandler} writing to {@code <logFolder>/eSign.log}.
     *
     * <p>Call this once a writable directory is available, e.g. when
     * {@code tempFolder} is known inside {@code getGatewayParameter}.  The
     * method is a no-op if a file handler has already been attached, logging
     * is disabled, or {@code logFolder} is {@code null}.
     *
     * <p>On success the ConsoleHandler is removed (file takes over).  On
     * failure (e.g. permission denied on Android if the path is wrong) the
     * ConsoleHandler stays active.
     *
     * @param logFolder directory where {@code eSign.log} should be created
     */
    public static void initFileHandler(String logFolder) {
        if (fileHandlerAttached || logFolder == null || logger == null) {
            return;
        }
        if (currentLevel == Level.OFF) {
            return; // logging is disabled; nothing to do
        }
        tryAttachFileHandler(logFolder, currentLevel);
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    private static Level toLevel(eSignSettings.LogType logType) {
        switch (logType) {
            case NoLog:
                return Level.OFF;
            case NoDebugLog:
                return Level.WARNING;
            default:
                return Level.ALL;
        }
    }

    /** FileHandler / ConsoleHandler level: INFO when ALL is requested, else the chosen level. */
    private static Level effectiveHandlerLevel(Level loggerLevel) {
        return (loggerLevel == Level.ALL) ? Level.INFO : loggerLevel;
    }

    private static void addConsoleHandler(Level loggerLevel) {
        if (loggerLevel == Level.OFF) {
            return;
        }
        java.util.logging.ConsoleHandler ch = new java.util.logging.ConsoleHandler();
        ch.setLevel(effectiveHandlerLevel(loggerLevel));
        logger.addHandler(ch);
    }

    private static void tryAttachFileHandler(String logFolder, Level loggerLevel) {
        try {
            File directory = new File(logFolder);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String fileName = logFolder + File.separator + "eSign.log";
            FileHandler fileHandler = new FileHandler(fileName, 10 * 1024 * 1024, 100, true);
            fileHandler.setLevel(effectiveHandlerLevel(loggerLevel));

            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("IST"));
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    String time = dateFormat.format(new Date(record.getMillis()));
                    StringBuilder sb = new StringBuilder();
                    sb.append(time).append("\t")
                      .append("[").append(record.getLevel()).append("]\t")
                      .append("[").append(record.getSourceClassName()).append("]\t\t")
                      .append(record.getMessage());
                    Throwable thrown = record.getThrown();
                    if (thrown != null) {
                        sb.append(" | ").append(thrown);
                        Throwable cause = thrown.getCause();
                        if (cause != null) {
                            sb.append(" caused by: ").append(cause);
                        }
                    }
                    sb.append("\n");
                    return sb.toString();
                }
            });

            // File handler is ready — remove console handler (no duplicate output).
            for (Handler h : logger.getHandlers()) {
                if (h instanceof java.util.logging.ConsoleHandler) {
                    logger.removeHandler(h);
                }
            }
            logger.addHandler(fileHandler);
            fileHandlerAttached = true;

        } catch (IOException | SecurityException e) {
            // Could not create the file (e.g. wrong path on Android).
            // Console handler stays active so logs are not silently lost.
        }
    }
}
