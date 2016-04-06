package com.xw.repo.xedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * XEditText
 * Created by woxingxiao on 2015/9/4.
 * Github: https://github.com/woxingxiao/XEditText
 */
public class XEditText extends AppCompatEditText {

    private OnTextChangeListener mTextChangeListener;
    private OnMarkerClickListener mMarkerClickListener;
    private TextWatcher mTextWatcher;
    private int preLength;
    private int currLength;
    private Drawable mRightMarkerDrawable;
    private Drawable mLeftDrawable;
    private boolean hasFocused;
    private int[] pattern; // pattern to separate. e.g.: separator = "-", pattern = [3,4,4] -> xxx-xxxx-xxxx
    private int[] intervals; // indexes of separators.
    private String separator; //separator，default is "".
    /* When you set pattern, it will automatically compute the max length of characters and separators,
     so you don't need to set 'maxLength' attr in your xml any more(it won't work).*/
    private int mMaxLength = Integer.MAX_VALUE;
    private boolean hasNoSeparator; // true, the same as EditText.
    private boolean customizeMarkerEnable; // true, you can customize the Marker's onClick event.
    private ShowMarkerTime mShowMarkerTime; // set when ths Marker shows，after inputted by default.
    private Paint mTextPaint;
    private Rect mRect;
    private Rect mTextRect;
    private Bitmap mBitmap;
    private Paint mBitPaint;
    private boolean iOSStyleEnable; // iOS style，to set this, you should combine 'shape.xml' to set frame.
    private boolean iOSFrameHide;
    private CharSequence mHintCharSeq;
    private boolean disableEmoji; // disable emoji and some special symbol input.

    public XEditText(Context context) {
        this(context, null);
    }

    public XEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle); // Attention here !
    }

    public XEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XEditText, defStyleAttr, 0);

        separator = a.getString(R.styleable.XEditText_x_separator);
        if (separator == null)
            separator = "";
        customizeMarkerEnable = a.getBoolean(R.styleable.XEditText_x_customizeMarkerEnable, false);
        int which = a.getInt(R.styleable.XEditText_x_showMarkerTime, 0);
        switch (which) {
            case 0:
                mShowMarkerTime = ShowMarkerTime.AFTER_INPUT;
                break;
            case 1:
                mShowMarkerTime = ShowMarkerTime.BEFORE_INPUT;
                break;
            case 2:
                mShowMarkerTime = ShowMarkerTime.ALWAYS;
                break;
        }
        iOSStyleEnable = a.getBoolean(R.styleable.XEditText_x_iOSStyleEnable, false);
        disableEmoji = a.getBoolean(R.styleable.XEditText_x_disableEmoji, false);
        a.recycle();

        init();
    }

    private void init() {
        if (getInputType() == InputType.TYPE_CLASS_NUMBER) // if inputType="number", it can't insert separator.
            setInputType(InputType.TYPE_CLASS_PHONE);

        mTextWatcher = new MyTextWatcher();
        this.addTextChangedListener(mTextWatcher);
        mRightMarkerDrawable = getCompoundDrawables()[2];
        if (customizeMarkerEnable && mRightMarkerDrawable != null) {
            setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                    null, getCompoundDrawables()[3]);
            setHasNoSeparator(true);
        }
        if (mRightMarkerDrawable == null) { // didn't customize Marker
            mRightMarkerDrawable = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_clear_mtrl_alpha); // support v4
            DrawableCompat.setTint(mRightMarkerDrawable, getCurrentHintTextColor());
            if (mRightMarkerDrawable != null) {
                mRightMarkerDrawable.setBounds(0, 0, mRightMarkerDrawable.getIntrinsicWidth(),
                        mRightMarkerDrawable.getIntrinsicHeight());
            }
        }

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                hasFocused = hasFocus;
                markerFocusChangeLogic();
                iOSFocusChangeLogic();
            }
        });

        if (iOSStyleEnable)
            initiOSObjects();
        if (disableEmoji)
            setFilters(new InputFilter[]{new EmojiExcludeFilter()});
    }

    private void initiOSObjects() {
        mLeftDrawable = getCompoundDrawables()[0];
        if (mLeftDrawable != null) {
            if (mBitmap == null || mBitPaint == null) {
                BitmapDrawable bd = (BitmapDrawable) mLeftDrawable;
                mBitmap = bd.getBitmap();
                mBitPaint = new Paint();
                mBitPaint.setAntiAlias(true);
            }

            setCompoundDrawables(null, getCompoundDrawables()[1],
                    getCompoundDrawables()[2], getCompoundDrawables()[3]);
        }
        mHintCharSeq = getHint();
        if (mHintCharSeq != null) {
            setHint("");
            if (mRect == null || mTextRect == null || mTextPaint == null) {
                mRect = new Rect(getLeft(), getTop(), getWidth(), getHeight());
                mTextRect = new Rect();
                mTextPaint = new Paint();
                mTextPaint.setAntiAlias(true);
                mTextPaint.setTextSize(getTextSize());
                mTextPaint.setColor(getCurrentHintTextColor());
                mTextPaint.setTextAlign(Paint.Align.CENTER);
                mTextPaint.getTextBounds(mHintCharSeq.toString(), 0, mHintCharSeq.length(), mTextRect);
            }
        }
        iOSFrameHide = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (iOSStyleEnable) {
            if (iOSFrameHide)
                return;

            if (mHintCharSeq != null) {
                Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
                int textCenterY = (mRect.bottom + mRect.top - fontMetrics.bottom - fontMetrics.top) / 2 - 5;
                canvas.drawText(mHintCharSeq.toString(), canvas.getWidth() / 2, canvas.getHeight() / 2 + textCenterY, mTextPaint);
            }
            if (mBitmap != null) {
                canvas.drawBitmap(mBitmap,
                        (canvas.getWidth() - mTextRect.width()) / 2 - mBitmap.getWidth() - getCompoundDrawablePadding(),
                        (canvas.getHeight() - mBitmap.getHeight()) / 2, mBitPaint);
            }
        }
    }

    /**
     * listen Marker's onTouch event
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (hasFocused && mRightMarkerDrawable != null && event.getAction() == MotionEvent.ACTION_UP) {
            Rect rect = mRightMarkerDrawable.getBounds();
            int height = rect.height();
            int rectTopY = (getHeight() - height) / 2;
            boolean isAreaX = event.getX() >= (getWidth() - getTotalPaddingRight()) &&
                    event.getX() <= (getWidth() - getPaddingRight());
            boolean isAreaY = event.getY() >= rectTopY && event.getY() <= (rectTopY + height);
            if (isAreaX && isAreaY) {
                if (customizeMarkerEnable) {
                    hideInputMethod();
                    if (mMarkerClickListener != null)
                        mMarkerClickListener.onMarkerClick(event.getRawX(), event.getRawY());

                    return true;
                } else {
                    setError(null);
                    setText("");
                }
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * set customize separator
     */
    public void setSeparator(String separator) {
        if (separator == null) {
            throw new IllegalArgumentException("separator can't be null !");
        }
        this.separator = separator;
    }

    /**
     * set customize pattern
     *
     * @param pattern e.g. pattern:{4,4,4,4}, separator:"-"  ===>  xxxx-xxxx-xxxx-xxxx
     */
    public void setPattern(int[] pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern can't be null !");
        }
        this.pattern = pattern;

        intervals = new int[pattern.length];
        int count = 0;
        int sum = 0;
        for (int i = 0; i < pattern.length; i++) {
            sum += pattern[i];
            intervals[i] = sum + count;
            if (i < pattern.length - 1)
                count += separator.length();
        }
        mMaxLength = intervals[intervals.length - 1];
    }

    /**
     * set customize Marker drawable on the right
     */
    public void setRightMarkerDrawable(Drawable drawable) {
        mRightMarkerDrawable = drawable;
        if (mRightMarkerDrawable != null) {
            mRightMarkerDrawable.setBounds(0, 0, mRightMarkerDrawable.getIntrinsicWidth(),
                    mRightMarkerDrawable.getIntrinsicHeight());
        }
    }

    /**
     * set customize Marker drawableResId on the right
     */
    public void setRightMarkerDrawableRes(int resId) {
        mRightMarkerDrawable = ContextCompat.getDrawable(getContext(), resId);
        if (mRightMarkerDrawable != null) {
            mRightMarkerDrawable.setBounds(0, 0, mRightMarkerDrawable.getIntrinsicWidth(),
                    mRightMarkerDrawable.getIntrinsicHeight());
        }
    }

    /**
     * set CharSequence to separate
     */
    public void setTextToSeparate(CharSequence c) {
        if (c == null || c.length() == 0)
            return;

        setText("");
        for (int i = 0; i < c.length(); i++) {
            append(c.subSequence(i, i + 1));
        }
    }

    /**
     * get text without separators
     */
    public String getNonSeparatorText() {
        return getText().toString().replaceAll(separator, "");
    }

    /**
     * set customize Marker enable
     */
    public void setCustomizeMarkerEnable(boolean customizeMarkerEnable) {
        this.customizeMarkerEnable = customizeMarkerEnable;
        if (customizeMarkerEnable && mRightMarkerDrawable != null) { // 如果自定义Marker，暂时不显示rightDrawable
            setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                    null, getCompoundDrawables()[3]);
        }
    }

    /**
     * when Marker shows
     *
     * @param showMarkerTime BEFORE_INPUT：if has none contents
     *                       AFTER_INPUT：if has contents
     *                       ALWAYS：shows once having focus
     */
    public void setShowMarkerTime(ShowMarkerTime showMarkerTime) {
        mShowMarkerTime = showMarkerTime;
    }

    /**
     * @return has separator or not
     */
    public boolean hasNoSeparator() {
        return hasNoSeparator;
    }

    /**
     * @param hasNoSeparator true, has no separator, the same as EditText
     */
    public void setHasNoSeparator(boolean hasNoSeparator) {
        this.hasNoSeparator = hasNoSeparator;
        if (hasNoSeparator)
            separator = "";
    }

    /**
     * @param iOSStyleEnable true: enable iOS style;
     *                       false: disable iOS style
     */
    public void setiOSStyleEnable(boolean iOSStyleEnable) {
        this.iOSStyleEnable = iOSStyleEnable;
        if (iOSStyleEnable) {
            initiOSObjects();
        } else {
            setCompoundDrawables(mLeftDrawable, getCompoundDrawables()[1],
                    getCompoundDrawables()[2], getCompoundDrawables()[3]);
            setHint(mHintCharSeq);
            iOSFrameHide = true;
        }
        invalidate();
    }

    /**
     * set true to disable Emoji and special symbol
     *
     * @param disableEmoji true: disable emoji;
     *                     false: enable emoji
     */
    public void setDisableEmoji(boolean disableEmoji) {
        this.disableEmoji = disableEmoji;
        if (disableEmoji)
            setFilters(new InputFilter[]{new EmojiExcludeFilter()});
        else
            setFilters(new InputFilter[0]);
    }

    /**
     * the same as EditText.addOnTextChangeListener(TextWatcher textWatcher)
     */
    public void setOnTextChangeListener(OnTextChangeListener listener) {
        this.mTextChangeListener = listener;
    }

    /**
     * set on Marker click listener
     */
    public void setOnMarkerClickListener(OnMarkerClickListener markerClickListener) {
        mMarkerClickListener = markerClickListener;
    }

    /**
     * set max length of contents
     */
    public void setMaxLength(int maxLength) {
        this.mMaxLength = maxLength;
    }

    // =========================== MyTextWatcher ================================
    private class MyTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            preLength = s.length();
            if (mTextChangeListener != null)
                mTextChangeListener.beforeTextChanged(s, start, count, after);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mTextChangeListener != null)
                mTextChangeListener.onTextChanged(s, start, before, count);
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mTextChangeListener != null)
                mTextChangeListener.afterTextChanged(s);

            currLength = s.length();
            if (hasNoSeparator)
                mMaxLength = currLength;

            markerFocusChangeLogic();

            if (currLength > mMaxLength) {
                getText().delete(currLength - 1, currLength);
                return;
            }
            if (pattern == null)
                return;

            for (int i = 0; i < pattern.length; i++) {
                if (currLength - 1 == intervals[i]) {
                    if (currLength > preLength) { // inputting
                        if (currLength < mMaxLength) {
                            removeTextChangedListener(mTextWatcher);
                            getText().insert(currLength - 1, separator);
                        }
                    } else if (preLength <= mMaxLength) { // deleting
                        removeTextChangedListener(mTextWatcher);
                        getText().delete(currLength - 1, currLength);
                    }

                    addTextChangedListener(mTextWatcher);

                    break;
                }
            }
        }
    }

    private void markerFocusChangeLogic() {
        if (!hasFocused) {
            setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                    null, getCompoundDrawables()[3]);
            return;
        }
        Drawable drawable = null;
        switch (mShowMarkerTime) {
            case ALWAYS:
                drawable = mRightMarkerDrawable;
                break;
            case BEFORE_INPUT:
                if (currLength == 0) drawable = mRightMarkerDrawable;

                break;
            case AFTER_INPUT:
                if (currLength > 0) drawable = mRightMarkerDrawable;

                break;
        }
        setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                drawable, getCompoundDrawables()[3]);
    }

    private void iOSFocusChangeLogic() {
        if (!iOSStyleEnable)
            return;
        if (hasFocused) {
            if (mLeftDrawable != null)
                setCompoundDrawables(mLeftDrawable, getCompoundDrawables()[1],
                        getCompoundDrawables()[2], getCompoundDrawables()[3]);
            if (mHintCharSeq != null)
                setHint(mHintCharSeq);
            iOSFrameHide = true;
            invalidate();
        } else {
            if (currLength == 0) {
                initiOSObjects();
                invalidate();
            }
        }
    }

    public interface OnTextChangeListener {

        void beforeTextChanged(CharSequence s, int start, int count, int after);

        void onTextChanged(CharSequence s, int start, int before, int count);

        void afterTextChanged(Editable s);
    }

    public interface OnMarkerClickListener {

        /**
         * @param x clicked pointer's x coordinate
         * @param y clicked pointer's y coordinate
         */
        void onMarkerClick(float x, float y);
    }

    public enum ShowMarkerTime {
        BEFORE_INPUT,
        AFTER_INPUT,
        ALWAYS
    }

    /**
     * hide the input method
     */
    private void hideInputMethod() {
        InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    /**
     * disable emoji and special symbol input
     */
    private class EmojiExcludeFilter implements InputFilter {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                int type = Character.getType(source.charAt(i));
                if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                    return "";
                }
            }
            return null;
        }
    }
}