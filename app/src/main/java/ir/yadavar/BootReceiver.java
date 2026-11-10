package ir.yadavar;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (!Store.all(c).isEmpty()) ReminderService.poke(c);
    }
}
