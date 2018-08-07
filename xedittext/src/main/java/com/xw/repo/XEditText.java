package com.xw.repo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * XEditText
 * <p>
 * GitHub: https://github.com/woxingxiao/XEditText
 * <p>
 * Created by woxingxiao on 2017-03-22.
 */
public class XEditText extends AppCompatEditText {

    private String mSeparator; //mSeparator，default is "".
    private boolean disableClear; // disable clear drawable.
    private int mClearResId;
    private boolean togglePwdDrawableEnable;
    private boolean disableEmoji; // disable emoji and some special symbol input.
    private int mShowPwdResId;
    private int mHidePwdResId;

    private Drawable mClearDrawable;
    private Drawable mTogglePwdDrawable;
    private OnXTextChangeListener mXTextChangeListener;
    private TextWatcher mTextWatcher;
    private int mOldLength;
    private int mNowLength;
    private int mSelectionPos;
    private boolean hasFocused;
    private int[] pattern; // pattern to separate. e.g.: mSeparator = "-", pattern = [3,4,4] -> xxx-xxxx-xxxx
    private int[] intervals; // indexes of separators.
    private boolean hasNoSeparator; // true, the same as EditText.
    private boolean isPwdInputType;
    private boolean isPwdShow;
    private Bitmap mBitmap;
    private int mLeft, mTop;

    public XEditText(Context context) {
        this(context, null);
    }

    public XEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle); // Attention here !
    }

    public XEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initAttrs(context, attrs, defStyleAttr);

        if (disableEmoji) {
            setFilters(new InputFilter[]{new EmojiExcludeFilter()});
        }

        mTextWatcher = new MyTextWatcher();
        this.addTextChangedListener(mTextWatcher);

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                hasFocused = hasFocus;
                logicOfCompoundDrawables();
            }
        });
    }

    private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XEditText, defStyleAttr, 0);

        mSeparator = a.getString(R.styleable.XEditText_x_separator);
        disableClear = a.getBoolean(R.styleable.XEditText_x_disableClear, false);
        mClearResId = a.getResourceId(R.styleable.XEditText_x_clearDrawable, -1);
        togglePwdDrawableEnable = a.getBoolean(R.styleable.XEditText_x_togglePwdDrawableEnable, true);
        mShowPwdResId = a.getResourceId(R.styleable.XEditText_x_showPwdDrawable, -1);
        mHidePwdResId = a.getResourceId(R.styleable.XEditText_x_hidePwdDrawable, -1);
        disableEmoji = a.getBoolean(R.styleable.XEditText_x_disableEmoji, false);
        String pattern = a.getString(R.styleable.XEditText_x_pattern);
        a.recycle();

        if (mSeparator == null) {
            mSeparator = "";
        }
        hasNoSeparator = TextUtils.isEmpty(mSeparator);
        if (mSeparator.length() > 0) {
            int inputType = getInputType();
            if (inputType == 2 || inputType == 8194 || inputType == 4098) { // if inputType is number, it can't insert mSeparator.
                setInputType(InputType.TYPE_CLASS_PHONE);
            }
        }

        if (!disableClear) {
            if (mClearResId == -1)
                mClearResId = R.drawable.x_et_svg_ic_clear_24dp;
            mClearDrawable = AppCompatResources.getDrawable(context, mClearResId);
            if (mClearDrawable != null) {
                mClearDrawable.setBounds(0, 0, mClearDrawable.getIntrinsicWidth(),
                        mClearDrawable.getIntrinsicHeight());
                if (mClearResId == R.drawable.x_et_svg_ic_clear_24dp)
                    DrawableCompat.setTint(mClearDrawable, getCurrentHintTextColor());
            }
        }

        dealWithInputTypes(true);

        if (!mSeparator.isEmpty() && !isPwdInputType && pattern != null && !pattern.isEmpty()) {
            boolean ok = true;
            if (pattern.contains(",")) {
                String[] split = pattern.split(",");

                int[] array = new int[split.length];
                for (int i = 0; i < array.length; i++) {
                    try {
                        array[i] = Integer.parseInt(split[i]);
                    } catch (Exception e) {
                        ok = false;
                        break;
                    }
                }

                if (ok) {
                    setPattern(array, mSeparator);
                }
            } else {
                try {
                    int i = Integer.parseInt(pattern);
                    setPattern(new int[]{i}, mSeparator);
                } catch (Exception e) {
                    ok = false;
                }
            }

            if (!ok) {
                Log.e("XEditText", "the Pattern format is incorrect!");
            }
        }
    }

    private void dealWithInputTypes(boolean fromXml) {
        int inputType = getInputType();
        if (!fromXml) {
            inputType++;
            if (inputType == 17)
                inputType++;
        }

        isPwdInputType = togglePwdDrawableEnable && (inputType == 129 || inputType == 18 || inputType == 145 || inputType == 225);
        if (isPwdInputType) {
            isPwdShow = inputType == 145; // textVisiblePassword
            if (isPwdShow) {
                setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                setTransformationMethod(PasswordTransformationMethod.getInstance());
            }

            if (mShowPwdResId == -1)
                mShowPwdResId = R.drawable.x_et_svg_ic_show_password_24dp;
            if (mHidePwdResId == -1)
                mHidePwdResId = R.drawable.x_et_svg_ic_hide_password_24dp;
            int tId = isPwdShow ? mShowPwdResId : mHidePwdResId;
            mTogglePwdDrawable = ContextCompat.getDrawable(getContext(), tId);
            if (mShowPwdResId == R.drawable.x_et_svg_ic_show_password_24dp ||
                    mHidePwdResId == R.drawable.x_et_svg_ic_hide_password_24dp) {
                DrawableCompat.setTint(mTogglePwdDrawable, getCurrentHintTextColor());
            }
            mTogglePwdDrawable.setBounds(0, 0, mTogglePwdDrawable.getIntrinsicWidth(),
                    mTogglePwdDrawable.getIntrinsicHeight());

            if (mClearResId == -1)
                mClearResId = R.drawable.x_et_svg_ic_clear_24dp;
            if (!disableClear) {
                mBitmap = getBitmapFromVectorDrawable(getContext(), mClearResId,
                        mClearResId == R.drawable.x_et_svg_ic_clear_24dp); // clearDrawable
            }
        }

        if (!fromXml) {
            setTextEx(getTextEx());
            logicOfCompoundDrawables();
        }
    }

    private Bitmap getBitmapFromVectorDrawable(Context context, int drawableId, boolean tint) {
        Drawable drawable = AppCompatResources.getDrawable(context, drawableId);
        if (drawable == null)
            return null;

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
    public void setInputType(int type) {
        super.setInputType(type);

        dealWithInputTypes(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (hasFocused && mBitmap != null && isPwdInputType && !isTextEmpty()) {
            if (mLeft * mTop == 0) {
                mLeft = getMeasuredWidth() - getPaddingRight() -
                        mTogglePwdDrawable.getIntrinsicWidth() - mBitmap.getWidth() - dp2px(4);
                mTop = (getMeasuredHeight() - mBitmap.getHeight()) >> 1;
            }
            canvas.drawBitmap(mBitmap, mLeft, mTop, null);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
        }

        if (hasFocused && isPwdInputType && event.getAction() == MotionEvent.ACTION_UP) {
            int w = mTogglePwdDrawable.getIntrinsicWidth();
            int h = mTogglePwdDrawable.getIntrinsicHeight();
            int top = (getMeasuredHeight() - h) >> 1;
            int right = getMeasuredWidth() - getPaddingRight();
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

            if (!disableClear) {
                right -= w + dp2px(4);
                isAreaX = event.getX() <= right && event.getX() >= right - mBitmap.getWidth();
                if (isAreaX && isAreaY) {
                    setError(null);
                    setText("");
                }
            }
        }

        if (hasFocused && !disableClear && !isPwdInputType && event.getAction() == MotionEvent.ACTION_UP) {
            Rect rect = mClearDrawable.getBounds();
            int rectW = rect.width();
            int rectH = rect.height();
            int top = (getMeasuredHeight() - rectH) >> 1;
            int right = getMeasuredWidth() - getPaddingRight();
            boolean isAreaX = event.getX() <= right && event.getX() >= right - rectW;
            boolean isAreaY = event.getY() >= top && event.getY() <= (top + rectH);
            if (isAreaX && isAreaY) {
                setError(null);
                setText("");
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        ClipboardManager clipboardManager = (ClipboardManager) getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            if (id == 16908320 || id == 16908321) { // catch CUT or COPY ops
                super.onTextContextMenuItem(id);

                ClipData clip = clipboardManager.getPrimaryClip();
                ClipData.Item item = clip.getItemAt(0);
                if (item != null && item.getText() != null) {
                    String s = item.getText().toString().replace(mSeparator, "");
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, s));

                    return true;
                }
            } else if (id == 16908322) { // catch PASTE ops
                ClipData clip = clipboardManager.getPrimaryClip();
                ClipData.Item item = clip.getItemAt(0);
                if (item != null && item.getText() != null) {
                    String content = item.getText().toString().replace(mSeparator, "");
                    setTextEx(getText().toString() + content);
                    return true;
                }
            }
        }

        return super.onTextContextMenuItem(id);
    }

    // =========================== MyTextWatcher ================================
    private class MyTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mOldLength = s.length();
            if (mXTextChangeListener != null) {
                mXTextChangeListener.beforeTextChanged(s, start, count, after);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mNowLength = s.length();
            mSelectionPos = getSelectionStart();
            if (mXTextChangeListener != null) {
                mXTextChangeListener.onTextChanged(s, start, before, count);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            logicOfCompoundDrawables();

            if (mSeparator.isEmpty()) {
                if (mXTextChangeListener != null) {
                    mXTextChangeListener.afterTextChanged(s);
                }

                return;
            }

            removeTextChangedListener(mTextWatcher);

            String trimmedText;
            if (hasNoSeparator) {
                trimmedText = s.toString().trim();
            } else {
                trimmedText = s.toString().replaceAll(mSeparator, "").trim();
            }
            setTextToSeparate(trimmedText, false);

            if (mXTextChangeListener != null) {
                s.clear();
                s.append(trimmedText);
                mXTextChangeListener.afterTextChanged(s);
            }

            addTextChangedListener(mTextWatcher);
        }
    }

    private void logicOfCompoundDrawables() {
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
     * set customize separator
     */
    public void setSeparator(@NonNull String separator) {
        this.mSeparator = separator;

        hasNoSeparator = TextUtils.isEmpty(mSeparator);
        if (mSeparator.length() > 0) {
            int inputType = getInputType();
            if (inputType == 2 || inputType == 8194 || inputType == 4098) { // if inputType is number, it can't insert mSeparator.
                setInputType(InputType.TYPE_CLASS_PHONE);
            }
        }
    }

    /**
     * set customize pattern
     *
     * @param pattern   e.g. pattern:{4,4,4,4}, separator:"-" to xxxx-xxxx-xxxx-xxxx
     * @param separator separator
     */
    public void setPattern(@NonNull int[] pattern, @NonNull String separator) {
        setSeparator(separator);
        setPattern(pattern);
    }

    /**
     * set customize pattern
     *
     * @param pattern e.g. pattern:{4,4,4,4}, separator:"-" to xxxx-xxxx-xxxx-xxxx
     */
    public void setPattern(@NonNull int[] pattern) {
        this.pattern = pattern;

        intervals = new int[pattern.length];
        int sum = 0;
        for (int i = 0; i < pattern.length; i++) {
            sum += pattern[i];
            intervals[i] = sum;
        }
        /* When you set pattern, it will automatically compute the max length of characters and separators,
           so you don't need to set 'maxLength' attr in your xml any more(it won't work).*/
        int maxLength = intervals[intervals.length - 1] + pattern.length - 1;

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(maxLength);
        setFilters(filters);
    }

    /**
     * set CharSequence to separate
     *
     * @deprecated Call {@link #setTextEx(CharSequence)} instead.
     */
    @Deprecated
    public void setTextToSeparate(@NonNull CharSequence c) {
        setTextToSeparate(c, true);
    }

    /**
     * Call {@link #setText(CharSequence)} or set text to separate by the pattern had been set.
     * <br/>
     * It's especially convenient to call {@link #setText(CharSequence)} in Kotlin.
     */
    public void setTextEx(CharSequence text) {
        if (TextUtils.isEmpty(text) || hasNoSeparator) {
            setText(text);
            if (text != null && text.length() > 0) {
                setSelection(text.length());
            }
        } else {
            setTextToSeparate(text, true);
        }
    }

    private void setTextToSeparate(@NonNull CharSequence c, boolean fromUser) {
        if (c.length() == 0 || intervals == null) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0, length1 = c.length(); i < length1; i++) {
            builder.append(c.subSequence(i, i + 1));
            for (int j = 0, length2 = intervals.length; j < length2; j++) {
                if (i == intervals[j] && j < length2 - 1) {
                    builder.insert(builder.length() - 1, mSeparator);

                    if (mSelectionPos == builder.length() - 1 && mSelectionPos > intervals[j]) {
                        if (mNowLength > mOldLength) { // inputted
                            mSelectionPos += mSeparator.length();
                        } else { // deleted
                            mSelectionPos -= mSeparator.length();
                        }
                    }
                }
            }
        }

        String text = builder.toString();
        setText(text);

        if (fromUser) {
            setSelection(text.length());
        } else {
            if (mSelectionPos > text.length()) {
                mSelectionPos = text.length();
            }
            if (mSelectionPos < 0) {
                mSelectionPos = 0;
            }
            setSelection(mSelectionPos);
        }
    }

    /**
     * Get text string had been trimmed.
     */
    public String getTextTrimmed() {
        return getTextEx().trim();
    }

    /**
     * Get text string.
     */
    public String getTextEx() {
        if (hasNoSeparator) {
            return getText().toString();
        } else {
            return getText().toString().replaceAll(mSeparator, "");
        }
    }

    /**
     * Get text String had been trimmed.
     *
     * @deprecated Call {@link #getTextTrimmed()} instead.
     */
    @Deprecated
    public String getTrimmedString() {
        if (hasNoSeparator) {
            return getText().toString().trim();
        } else {
            return getText().toString().replaceAll(mSeparator, "").trim();
        }
    }

    /**
     * @return has separator or not
     */
    public boolean hasNoSeparator() {
        return hasNoSeparator;
    }

    /**
     * Set no separator, the same as EditText
     */
    public void setNoSeparator() {
        hasNoSeparator = true;
        mSeparator = "";
        intervals = null;
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

    public interface OnXTextChangeListener {

        void beforeTextChanged(CharSequence s, int start, int count, int after);

        void onTextChanged(CharSequence s, int start, int before, int count);

        void afterTextChanged(Editable s);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("save_instance", super.onSaveInstanceState());
        bundle.putString("separator", mSeparator);
        bundle.putIntArray("pattern", pattern);

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mSeparator = bundle.getString("separator");
            pattern = bundle.getIntArray("pattern");

            hasNoSeparator = TextUtils.isEmpty(mSeparator);
            if (pattern != null) {
                setPattern(pattern);
            }
            super.onRestoreInstanceState(bundle.getParcelable("save_instance"));

            return;
        }

        super.onRestoreInstanceState(state);
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