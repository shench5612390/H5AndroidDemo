package org.shench.h5androiddemo.utils;

import android.util.Log;

import org.shench.h5androiddemo.BuildConfig;


/**
 * 日志工具类
 * Created by chenjh on 2015/9/20.
 */
public class LogUtil {
    public static final boolean DEBUG = BuildConfig.DEBUG;

    private static long curTime;

    private LogUtil() {
    }

    /**
     * 输出 INFO 日志信息.
     *
     * @param tag
     * @param msg
     */
    public static void i(String tag, String msg) {
        if (DEBUG) {
            Log.i(tag, msg);
        }
    }

    /**
     * 输出 DEBUG 日志信息.
     *
     * @param tag
     * @param msg
     */
    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    /**
     * 输出 ERROR 日志信息.
     *
     * @param tag
     * @param msg
     */
    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    /**
     * 输出 ERROR 日志信息.
     *
     * @param tag
     * @param msg
     */
    public static void e(String tag, String msg, Throwable throwable) {
        Log.d(tag, msg, throwable);
    }

    /**
     * 输出 VERBOSE 日志信息.
     *
     * @param tag
     * @param msg
     */
    public static void v(String tag, String msg) {
        if (DEBUG) {
            Log.v(tag, msg);
        }
    }

    /**
     * 输出 WARN 日志信息.
     *
     * @param tag
     * @param msg
     */
    public static void w(String tag, String msg) {
        // 这个日志要输出，要不然有错误，获取不到日志。
        if (DEBUG) {
            Log.w(tag, msg);
        }
    }

    /**
     * 输出 时间 日志信息.
     *
     * @param tag
     * @param msg
     */
    public static void time(String tag, String msg) {
        if (DEBUG) {
            long t = System.currentTimeMillis();
            Log.i(tag, msg + " --- time elapse " + (t - curTime));
            curTime = System.currentTimeMillis();
        }
    }

    /**
     * 统计时间归零
     */
    public static void resetTime() {
        if (DEBUG) {
            curTime = System.currentTimeMillis();
        }
    }
}
