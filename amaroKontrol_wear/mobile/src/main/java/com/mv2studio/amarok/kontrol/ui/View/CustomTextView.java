package com.mv2studio.amarok.kontrol.ui.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.mv2studio.amarok.kontrol.R;

import java.util.HashMap;
import java.util.Map;


public class CustomTextView extends TextView {

	public enum TextFont {
        BOLD(0, "bold"), BOLD_C(1, "cbold"), LIGHT_C(2, "clight"), REG_C(3, "creg"),
        BLACK(4, "ebold"), LIGHT(5, "light"), MED(6, "med"), REG(7, "reg"), THIN(8, "thin");

        private static final Map<Integer, TextFont> lookup = new HashMap<Integer, TextFont>();
        static {
            for(TextFont font : TextFont.values())
                lookup.put(font.value, font);
        }

        public static TextFont get(int value) {
            return lookup.get(value);
        }

        private int value;
        private String name;
        TextFont(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int value() {
            return value;
        }
    }
	
	public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public CustomTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);

	}

	public CustomTextView(Context context) {
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CustomTextView);
			int fontEnumVal = TextFont.REG.value;

			for (int i = 0; i < a.getIndexCount(); i++) {
				if (a.getIndex(i) == R.styleable.CustomTextView_font) {
					fontEnumVal = a.getInt(R.styleable.CustomTextView_font, TextFont.REG.value);
				}
			}

            String fontName = "fonts/" + TextFont.get(fontEnumVal).getName() + ".ttf";

			if (!isInEditMode()) {
				Typeface myTypeface = Typeface.createFromAsset(getContext().getAssets(), fontName);
				setTypeface(myTypeface);
			}

			a.recycle();
		}
	}

	public void setTypeface(TextFont typeFace) {
        String fontName = "reg";
        if (typeFace != null) {
            fontName = typeFace.getName();
        }

        Typeface myTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontName + ".ttf");
        setTypeface(myTypeface);
    }

}
