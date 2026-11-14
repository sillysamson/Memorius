package com.memorius.app;

import android.app.TimePickerDialog;
import android.content.Context;

// Time picker that takes on the current style (window shape and colours)
final class StyledTimePickerDialog extends TimePickerDialog {
    StyledTimePickerDialog(Context c, OnTimeSetListener l, int h, int m, boolean is24) {
        super(c, Ui.dialogTheme(), l, h, m, is24);
    }

    @Override
    public void show() {
        super.show();
        Ui.styleDialog(this);
    }
}
