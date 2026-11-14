package com.memorius.app;

import android.app.DatePickerDialog;
import android.content.Context;

// Date picker that takes on the current style (window shape and colours)
final class StyledDatePickerDialog extends DatePickerDialog {
    StyledDatePickerDialog(Context c, OnDateSetListener l, int y, int m, int d) {
        super(c, Ui.dialogTheme(), l, y, m, d);
    }

    @Override
    public void show() {
        super.show();
        Ui.styleDialog(this);
    }
}
