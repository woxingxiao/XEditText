package com.xw.repo;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * XEditText
 * Github: https://github.com/woxingxiao/XEditText
 * <><p/>
 * Created by woxingxiao on 2017-03-22.
 */
public class XEditText extends AppCompatEditText {

    private String mSeparator; //mSeparator，default is "".
    private boolean disableClear; // disable clear drawable.
    private Drawable mClearDrawable;
    private Drawable mTogglePwdDrawable;
    private boolean disableEmoji; // disable emoji and some special symbol input.

    private int mShowPwdResId;
    private int mHidePwdResId;
    private OnXTextChangeListener mXTextChangeListener;
    private TextWatcher mTextWatcher;
    private int mPreLength;
    private boolean hasFocused;
    private int[] pattern; // pattern to separate. e.g.: mSeparator = "-", pattern = [3,4,4] -> xxx-xxxx-xxxx
    private int[] intervals; // indexes of separators.
    /* When you set pattern, it will automatically compute the max length of characters and separators,
     so you don't need to set 'maxLength' attr in your xml any more(it won't work).*/
    private int mMaxLength = Integer.MAX_VALUE;
    private boolean hasNoSeparator; // true, the same as EditText.
    private boolean isPwdInputType;
    private boolean isPwdShow;
    private Bitmap mBitmap;
    private Paint mPaint;

    public XEditText(Context context) {
        this(context, null);
    }

    public XEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle); // Attention here !
    }

    public XEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initAttrs(context, attrs, defStyleAttr);

        int inputType = getInputType();
        if (inputType == 2 || inputType == 8194 || inputType == 4098) { // if inputType is number, it can't insert mSeparator.
            setInputType(InputType.TYPE_CLASS_PHONE);
        }
        if (disableEmoji) {
            setFilters(new InputFilter[]{new EmojiExcludeFilter()});
        }

        mTextWatcher = new MyTextWatcher();
        this.addTextChangedListener(mTextWatcher);

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                hasFocused = hasFocus;
                markerFocusChangeLogic();
            }
        });
    }

    private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XEditText, defStyleAttr, 0);

        mSeparator = a.getString(R.styleable.XEditText_x_separator);
        if (mSeparator == null) {
            mSeparator = "";
        }

        disableClear = a.getBoolean(R.styleable.XEditText_x_disableClear, false);

        if (!disableClear) {
            int cdId = a.getResourceId(R.styleable.XEditText_x_clearDrawable, -1);
            if (cdId == -1)
                cdId = R.drawable.x_et_svg_ic_clear_24dp;
            mClearDrawable = ContextCompat.getDrawable(context, cdId);
            mClearDrawable.setBounds(0, 0, mClearDrawable.getIntrinsicWidth(),
                    mClearDrawable.getIntrinsicHeight());
            if (cdId == R.drawable.x_et_svg_ic_clear_24dp)
                DrawableCompat.setTint(mClearDrawable, getCurrentHintTextColor());
        }

        int inputType = getInputType();
        if (inputType == 129 || inputType == 145 || inputType == 18 || inputType == 225) {
            isPwdInputType = true;
            isPwdShow = inputType == 145;
            setMaxLength(20);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);

            mShowPwdResId = a.getResourceId(R.styleable.XEditText_x_showPwdDrawable, -1);
            mHidePwdResId = a.getResourceId(R.styleable.XEditText_x_hidePwdDrawable, -1);
            if (mShowPwdResId == -1)
                mShowPwdResId = R.drawable.x_et_svg_ic_show_password_24dp;
            if (mHidePwdResId == -1)
                mHidePwdResId = R.drawable.x_et_svg_ic_hide_password_24dp;

            int tId = isPwdShow ? mShowPwdResId : mHidePwdResId;
            mTogglePwdDrawable = ContextCompat.getDrawable(context, tId);
            if (mShowPwdResId == R.drawable.x_et_svg_ic_show_password_24dp ||
                    mHidePwdResId == R.drawable.x_et_svg_ic_hide_password_24dp) {
                DrawableCompat.setTint(mTogglePwdDrawable, getCurrentHintTextColor());
            }
            mTogglePwdDrawable.setBounds(0, 0, mTogglePwdDrawable.getIntrinsicWidth(),
                    mTogglePwdDrawable.getIntrinsicHeight());

            int cdId = a.getResourceId(R.styleable.XEditText_x_clearDrawable, -1);
            if (cdId == -1)
                cdId = R.drawable.x_et_svg_ic_clear_24dp;
            mBitmap = getBitmapFromVectorDrawable(context, cdId,
                    cdId == R.drawable.x_et_svg_ic_clear_24dp); // clearDrawable
        }

        disableEmoji = a.getBoolean(R.styleable.XEditText_x_disableEmoji, false);

        a.recycle();
    }

    private Bitmap getBitmapFromVectorDrawable(Context context, int drawableId, boolean tint) {
        Drawable drawable = AppCompatDrawableManager.get().getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }
        if (tint)
            DrawableCompat.setTint(drawable, getCurrentHintTextColor());

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (hasFocused && isPwdInputType && !isTextEmpty()) {
            int left = getMeasuredWidth() - getPaddingRight() - getCompoundDrawablePadding() -
                    mTogglePwdDrawable.getIntrinsicWidth() - mBitmap.getWidth() - dp2px(4);
            int top = (getMeasuredHeight() - mBitmap.getHeight()) >> 1;
            canvas.drawBitmap(mBitmap, left, top, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (hasFocused && isPwdInputType && event.getAction() == MotionEvent.ACTION_UP) {
            int w = mTogglePwdDrawable.getIntrinsicWidth();
            int h = mTogglePwdDrawable.getIntrinsicHeight();
            int top = (getMeasuredHeight() - h) >> 1;
            int right = getMeasuredWidth() - getPaddingRight() - getCompoundDrawablePadding();
            boolean isAreaX = event.getX() <= right && event.getX() >= right - w;
            boolean isAreaY = event.getY() >= top && event.getY() <= top + h;
            if (isAreaX && isAreaY) {
                isPwdShow = !isPwdShow;
                if (isPwdShow) {
                    setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                setSelection(getSelectionStart(), getSelectionEnd());
                mTogglePwdDrawable = ContextCompat.getDrawable(getContext(), isPwdShow ?
                        mShowPwdResId : mHidePwdResId);
                if (mShowPwdResId == R.drawable.x_et_svg_ic_show_password_24dp ||
                        mHidePwdResId == R.drawable.x_et_svg_ic_hide_password_24dp) {
                    DrawableCompat.setTint(mTogglePwdDrawable, getCurrentHintTextColor());
                }
                mTogglePwdDrawable.setBounds(0, 0, mTogglePwdDrawable.getIntrinsicWidth(),
                        mTogglePwdDrawable.getIntrinsicHeight());

                setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                        mTogglePwdDrawable, getCompoundDrawables()[3]);

                invalidate();
            }

            right -= w + dp2px(4);
            isAreaX = event.getX() <= right && event.getX() >= right - mBitmap.getWidth();
            if (isAreaX && isAreaY) {
                setError(null);
                setText("");
            }
        }

        if (hasFocused && !disableClear && !isPwdInputType && event.getAction() == MotionEvent.ACTION_UP) {
            Rect rect = mClearDrawable.getBounds();
            int rectW = rect.width();
            int rectH = rect.height();
            int top = (getMeasuredHeight() - rectH) >> 1;
            int right = getMeasuredWidth() - getPaddingRight() - getCompoundDrawablePadding();
            boolean isAreaX = event.getX() <= right && event.getX() >= right - rectW;
            boolean isAreaY = event.getY() >= top && event.getY() <= (top + rectH);
            if (isAreaX && isAreaY) {
                setError(null);
                setText("");
            }
        }

        return super.onTouchEvent(event);
    }

    // =========================== MyTextWatcher ================================
    private class MyTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mPreLength = s.length();
            if (mXTextChangeListener != null) {
                mXTextChangeListener.beforeTextChanged(s, start, count, after);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mXTextChangeListener != null) {
                mXTextChangeListener.onTextChanged(s, start, before, count);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mXTextChangeListener != null) {
                mXTextChangeListener.afterTextChanged(s);
            }

            int currLength = s.length();
            if (hasNoSeparator) {
                mMaxLength = currLength;
            }

            markerFocusChangeLogic();

            if (currLength > mMaxLength) {
                getText().delete(currLength - 1, currLength);
                return;
            }
            if (pattern == null) {
                return;
            }

            for (int i = 0; i < pattern.length; i++) {
                if (currLength - 1 == intervals[i]) {
                    if (currLength > mPreLength) { // inputting
                        if (currLength < mMaxLength) {
                            removeTextChangedListener(mTextWatcher);
                            getText().insert(currLength - 1, mSeparator);
                        }
                    } else if (mPreLength <= mMaxLength) { // deleting
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
        if (!hasFocused || (isTextEmpty() && !isPwdInputType)) {
            setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                    null, getCompoundDrawables()[3]);

            if (!isTextEmpty() && isPwdInputType) {
                invalidate();
            }
        } else {
            if (isPwdInputType) {
                if (mShowPwdResId == R.drawable.x_et_svg_ic_show_password_24dp ||
                        mHidePwdResId == R.drawable.x_et_svg_ic_hide_password_24dp) {
                    DrawableCompat.setTint(mTogglePwdDrawable, getCurrentHintTextColor());
                }
                setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                        mTogglePwdDrawable, getCompoundDrawables()[3]);
            } else if (!isTextEmpty() && !disableClear) {
                setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                        mClearDrawable, getCompoundDrawables()[3]);
            }
        }
    }

    private boolean isTextEmpty() {
        return getText().toString().trim().length() == 0;
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                Resources.getSystem().getDisplayMetrics());
    }

    /**
     * set customize mSeparator
     */
    public void setSeparator(String separator) {
        if (separator == null) {
            throw new IllegalArgumentException("mSeparator can't be null !");
        }
        this.mSeparator = separator;
    }

    /**
     * set customize pattern
     *
     * @param pattern e.g. pattern:{4,4,4,4}, mSeparator:"-" to xxxx-xxxx-xxxx-xxxx
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
            if (i < pattern.length - 1) {
                count += mSeparator.length();
            }
        }
        mMaxLength = intervals[intervals.length - 1];
    }

    /**
     * set CharSequence to separate
     */
    public void setTextToSeparate(CharSequence c) {
        if (c == null || c.length() == 0) {
            return;
        }

        setText("");
        for (int i = 0; i < c.length(); i++) {
            append(c.subSequence(i, i + 1));
        }
    }

    /**
     * get text without separators
     */
    public String getNonSeparatorText() {
        return getText().toString().replaceAll(mSeparator, "");
    }

    /**
     * @return has mSeparator or not
     */
    public boolean hasNoSeparator() {
        return hasNoSeparator;
    }

    /**
     * @param hasNoSeparator true, has no mSeparator, the same as EditText
     */
    public void setHasNoSeparator(boolean hasNoSeparator) {
        this.hasNoSeparator = hasNoSeparator;
        if (hasNoSeparator) {
            mSeparator = "";
        }
    }

    /**
     * set true to disable Emoji and special symbol
     *
     * @param disableEmoji true: disable emoji;
     *                     false: enable emoji
     */
    public void setDisableEmoji(boolean disableEmoji) {
        this.disableEmoji = disableEmoji;
        if (disableEmoji) {
            setFilters(new InputFilter[]{new EmojiExcludeFilter()});
        } else {
            setFilters(new InputFilter[0]);
        }
    }

    /**
     * the same as EditText.addOnTextChangeListener(TextWatcher textWatcher)
     */
    public void setOnXTextChangeListener(OnXTextChangeListener listener) {
        this.mXTextChangeListener = listener;
    }

    /**
     * set max length of contents
     */
    public void setMaxLength(int maxLength) {
        this.mMaxLength = maxLength;
    }

    public interface OnXTextChangeListener {

        void beforeTextChanged(CharSequence s, int start, int count, int after);

        void onTextChanged(CharSequence s, int start, int before, int count);

        void afterTextChanged(Editable s);
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