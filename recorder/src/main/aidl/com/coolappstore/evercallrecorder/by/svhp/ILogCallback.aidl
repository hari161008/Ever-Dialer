package com.coolappstore.evercallrecorder.by.svhp;

interface ILogCallback {
    void onLogEvent(String level, String tag, String message, String throwableStackTrace);
}