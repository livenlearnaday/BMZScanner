package io.github.livenlearnaday.bmzscanner.scanning.misc;

import android.text.InputFilter;
import android.text.Spanned;

public class InputFilterPositiveFloat implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            double input = Double.parseDouble(dest.toString() + source.toString());
            if( input > 0 )
                return null;
        } catch (NumberFormatException nfe) { }
        return "1";
    }
}