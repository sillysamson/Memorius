package com.memorius.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) { Notifier.fire(c); }
}
