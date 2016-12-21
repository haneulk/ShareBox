package com.example.hnkim.sharebox;

/**
 * Created by hnkim on 2016-12-21.
 */


import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.Comparator;

/**
 * Created by ESRENLL on 2016-12-15.
 */

public class ListData {
    public boolean mChecked;
    public Drawable mIcon;
    public String mName;
    public String mDate;
    public String mNote;
    public String mPath;

    public static final Comparator<ListData> NAME_ASC_COMPARATOR = new Comparator<ListData>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(ListData o1, ListData o2) {
            return sCollator.compare(o1.mName, o2.mName);
        }
    };
    public static final Comparator<ListData> NAME_DESC_COMPARATOR = new Comparator<ListData>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(ListData o1, ListData o2) {
            return sCollator.compare(o2.mName, o1.mName);
        }
    };
    public static final Comparator<ListData> DATE_ASC_COMPARATOR = new Comparator<ListData>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(ListData o1, ListData o2) {
            return sCollator.compare(o1.mDate, o2.mDate);
        }
    };
    public static final Comparator<ListData> DATE_DESC_COMPARATOR = new Comparator<ListData>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(ListData o1, ListData o2) {
            return sCollator.compare(o2.mDate, o1.mDate);
        }
    };
}
