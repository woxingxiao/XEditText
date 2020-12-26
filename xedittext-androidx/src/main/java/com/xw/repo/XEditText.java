package com.xw.repo;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * XEditText
 * <p>
 * GitHub: https://github.com/woxingxiao/XEditText
 * <p>
 * Created by woxingxiao on 2017-03-22.
 */
public class XEditText extends AppCompatEditText {

    private static final int DEFAULT_PADDING = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 4, Resources.getSystem().getDisplayMetrics());

    private String mSeparator; // the separator，default is "".
    private int mClearResId;
    private int mShowPwdResId;
    private int mHidePwdResId;
    private ColorStateList mClearDrawableTint;
    private ColorStateList mTogglePwdDrawableTint;
    private int mInteractionPadding; // padding of drawables' interactive rect area.
    private boolean disableClear; // disable the clear drawable.
    private boolean togglePwdDrawableEnable; // be able to use togglePwdDrawables.
    private boolean disableEmoji; // disable emoji and some special symbol input.

    private Drawable mClearDrawable;
    private Drawable mTogglePwdDrawable;
    private OnXTextChangeListener mXTextChangeListener;
    private OnXFocusChangeListener mXFocusChangeListener;
    private OnClearListener mOnClearListener;
    private final TextWatcher mTextWatcher;
    private int mOldLength;
    private int mNowLength;
    private int mSelectionPos;
    private boolean hasFocused;
    private int[] pattern; // pattern to separate. e.g.: mSeparator = "-", pattern = [3,4,4] -> xxx-xxxx-xxxx
    private int[] intervals; // indexes of separators.
    private boolean hasNoSeparator; // if is true, the same as EditText.
    private boolean isPwdInputType;
    private boolean isPwdShow;
    private Bitmap mBitmap;
    private int mStart, mTop;
    private int mHalfPadding;

    public XEditText(Context context) {
        this(context, null);
    }

    public XEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle); // Attention here !
    }

    public XEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initAttrs(context, attrs, defStyleAttr);

        mTextWatcher = new MyTextWatcher();
        addTextChangedListener(mTextWatcher);

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                hasFocused = hasFocus;
                logicOfCompoundDrawables();

                if (mXFocusChangeListener != null) {
                    mXFocusChangeListener.onFocusChange(v, hasFocus);
                }
            }
        });
    }

    private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XEditText, defStyleAttr, 0);

        mSeparator = a.getString(R.styleable.XEditText_x_separator);
        disableClear = a.getBoolean(R.styleable.XEditText_x_disableClear, false);
        mClearResId = a.getResourceId(R.styleable.XEditText_x_clearDrawable, R.drawable.x_et_svg_ic_clear_24dp);
        togglePwdDrawableEnable = a.getBoolean(R.styleable.XEditText_x_togglePwdDrawableEnable, true);
        mShowPwdResId = a.getResourceId(R.styleable.XEditText_x_showPwdDrawable, R.drawable.x_et_svg_ic_show_password_24dp);
        mHidePwdResId = a.getResourceId(R.styleable.XEditText_x_hidePwdDrawable, R.drawable.x_et_svg_ic_hide_password_24dp);
        if (a.hasValue(R.styleable.XEditText_x_clearDrawableTint)) {
            mClearDrawableTint = a.getColorStateList(R.styleable.XEditText_x_clearDrawableTint);
        } else {
            mClearDrawableTint = ColorStateList.valueOf(getCurrentHintTextColor());
        }
        if (mShowPwdResId == R.drawable.x_et_svg_ic_show_password_24dp &&
                mHidePwdResId == R.drawable.x_et_svg_ic_hide_password_24dp) {
            // didn't customize toggle pwd drawables
            if (a.hasValue(R.styleable.XEditText_x_togglePwdDrawableTint)) {
                mTogglePwdDrawableTint = a.getColorStateList(R.styleable.XEditText_x_togglePwdDrawableTint);
            } else {
                mTogglePwdDrawableTint = ColorStateList.valueOf(getCurrentHintTextColor());
            }
        } else {
            if (a.hasValue(R.styleable.XEditText_x_togglePwdDrawableTint)) {
                mTogglePwdDrawableTint = a.getColorStateList(R.styleable.XEditText_x_togglePwdDrawableTint);
            }
        }
        disableEmoji = a.getBoolean(R.styleable.XEditText_x_disableEmoji, false);
        String pattern = a.getString(R.styleable.XEditText_x_pattern);
        mInteractionPadding = a.getDimensionPixelSize(R.styleable.XEditText_x_interactionPadding, DEFAULT_PADDING);
        a.recycle();

        if (mSeparator == null) {
            mSeparator = "";
        }
        hasNoSeparator = TextUtils.isEmpty(mSeparator);
        if (mSeparator.length() > 0) {
            int inputType = getInputType();
            if (inputType == 2 || inputType == 8194 || inputType == 4098) {
                // If the inputType is number, the separator can't be inserted, so change to phone type.
                setInputType(InputType.TYPE_CLASS_PHONE);
            }
        }

        if (mInteractionPadding < 0)
            mInteractionPadding = 0;
        mHalfPadding = mInteractionPadding >> 1;

        if (!disableClear) {
            Drawable d = AppCompatResources.getDrawable(context, mClearResId);
            if (d != null) {
                mClearDrawable = DrawableCompat.wrap(d);
                mClearDrawable.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                DrawableCompat.setTintList(mClearDrawable.mutate(), mClearDrawableTint);
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

        if (disableEmoji) {
            InputFilter[] oldFilters = getFilters();
            InputFilter[] newFilters = new InputFilter[oldFilters.length + 1];
            newFilters[oldFilters.length] = new EmojiExcludeFilter();
            System.arraycopy(oldFilters, 0, newFilters, 0, oldFilters.length);
            setFilters(newFilters);
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

            Drawable d = AppCompatResources.getDrawable(getContext(), isPwdShow ? mShowPwdResId : mHidePwdResId);
            if (d != null) {
                mTogglePwdDrawable = DrawableCompat.wrap(d);
                mTogglePwdDrawable.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                if (mTogglePwdDrawableTint != null) {
                    DrawableCompat.setTintList(mTogglePwdDrawable.mutate(), mTogglePwdDrawableTint);
                }
            }

            if (!disableClear && mClearDrawable != null) {
                mBitmap = Bitmap.createBitmap(mClearDrawable.getIntrinsicWidth(),
                        mClearDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mBitmap);
                mClearDrawable.draw(canvas);

                setCompoundDrawablePadding(getCompoundDrawablePadding() + mBitmap.getWidth() + (mInteractionPadding << 1));
            }
        }

        if (!fromXml) {
            setTextEx(getTextEx());
            logicOfCompoundDrawables();
        }
    }

    @Override
    public void setInputType(int type) {
        super.setInputType(type);

        dealWithInputTypes(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        logicOfCompoundDrawables();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (hasFocused && !disableClear && mBitmap != null && isPwdInputType && !isTextEmpty()) {
            if (isRtl()) {
                if (mStart * mTop == 0) {
                    mStart = ViewCompat.getPaddingEnd(this) + mTogglePwdDrawable.getIntrinsicWidth()
                            + mInteractionPadding;
                    mTop = (getHeight() - mBitmap.getHeight()) >> 1;
                }
            } else {
                if (mStart * mTop == 0) {
                    mStart = getWidth() - ViewCompat.getPaddingEnd(this) - mTogglePwdDrawable.getIntrinsicWidth()
                            - mBitmap.getWidth() - mInteractionPadding;
                    mTop = (getHeight() - mBitmap.getHeight()) >> 1;
                }
            }
            // When the inputted content is too long, getScrollX() can fix the offset.
            canvas.drawBitmap(mBitmap, mStart + getScrollX(), mTop, null);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.onTouchEvent(event);
        }

        if (hasFocused && isPwdInputType && event.getAction() == MotionEvent.ACTION_UP) {
            int dw = mTogglePwdDrawable.getIntrinsicWidth();
            int dh = mTogglePwdDrawable.getIntrinsicHeight();
            int top = (getHeight() - dh) >> 1;
            int end;
            boolean inAreaX;
            boolean inAreaY = event.getY() >= top - mInteractionPadding && event.getY() <= top + dh + mInteractionPadding;
            float eventX = event.getX();
            if (isRtl()) {
                end = ViewCompat.getPaddingEnd(this);
                inAreaX = eventX >= end - mHalfPadding && eventX <= end + dw + mHalfPadding;
            } else {
                end = getWidth() - ViewCompat.getPaddingEnd(this);
                inAreaX = eventX <= end + mHalfPadding && eventX >= end - dw - mHalfPadding;
            }
            if (inAreaX && inAreaY) {
                isPwdShow = !isPwdShow;
                if (isPwdShow) {
                    setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                setSelection(getSelectionStart(), getSelectionEnd());

                Drawable d = AppCompatResources.getDrawable(getContext(), isPwdShow ? mShowPwdResId : mHidePwdResId);
                if (d != null) {
                    mTogglePwdDrawable = DrawableCompat.wrap(d);
                    mTogglePwdDrawable.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    if (mTogglePwdDrawableTint != null) {
                        DrawableCompat.setTintList(mTogglePwdDrawable.mutate(), mTogglePwdDrawableTint);
                    }
                    setCompoundDrawablesCompat(mTogglePwdDrawable);
                }
            }

            if (!disableClear) {
                if (isRtl()) {
                    end += dw + mInteractionPadding;
                    inAreaX = eventX >= end - mHalfPadding && eventX <= end + mBitmap.getWidth() + mHalfPadding;
                } else {
                    end -= dw + mInteractionPadding;
                    inAreaX = eventX <= end + mHalfPadding && eventX >= end - mBitmap.getWidth() - mHalfPadding;
                }
                if (inAreaX && inAreaY) {
                    setError(null);
                    Editable editable = getText();
                    if (editable != null) editable.clear();
                    if (mOnClearListener != null) {
                        mOnClearListener.onClear();
                    }
                }
            }
        }

        if (hasFocused && !disableClear && !isPwdInputType && event.getAction() == MotionEvent.ACTION_UP) {
            int dw = mClearDrawable.getIntrinsicWidth();
            int dh = mClearDrawable.getIntrinsicHeight();
            int top = (getHeight() - dh) >> 1;
            int end;
            boolean inAreaX;
            boolean inAreaY = event.getY() >= top - mInteractionPadding && event.getY() <= top + dh + mInteractionPadding;
            float eventX = event.getX();
            if (isRtl()) {
                end = ViewCompat.getPaddingEnd(this);
                inAreaX = eventX >= end - mInteractionPadding && eventX <= end + dw + mInteractionPadding;
            } else {
                end = getWidth() - ViewCompat.getPaddingEnd(this);
                inAreaX = eventX <= end + mInteractionPadding && eventX >= end - dw - mInteractionPadding;
            }
            if (inAreaX && inAreaY) {
                setError(null);
                Editable editable = getText();
                if (editable != null) editable.clear();
                if (mOnClearListener != null) {
                    mOnClearListener.onClear();
                }
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
                if (clip != null) {
                    ClipData.Item item = clip.getItemAt(0);
                    if (item != null && item.getText() != null) {
                        String s = item.getText().toString().replace(mSeparator, "");
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, s));

                        return true;
                    }
                }
            } else if (id == 16908322) { // catch PASTE ops
                ClipData clip = clipboardManager.getPrimaryClip();
                if (clip != null) {
                    ClipData.Item item = clip.getItemAt(0);
                    if (item != null && item.getText() != null) {
                        String content = item.getText().toString().replace(mSeparator, "");
                        String existedTxt = getTextNoneNull();

                        String txt;
                        int start = getSelectionStart();
                        int end = getSelectionEnd();
                        if (start * end >= 0) {
                            String startHalfEx = existedTxt.substring(0, start).replace(mSeparator, "");
                            txt = startHalfEx + content;
                            String endHalfEx = existedTxt.substring(end).replace(mSeparator, "");
                            txt += endHalfEx;
                        } else {
                            txt = existedTxt.replace(mSeparator, "") + content;
                        }
                        setTextEx(txt);

                        return true;
                    }
                }
            }
        }

        return super.onTextContextMenuItem(id);
    }

    private boolean isRtl() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
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
        if (!isEnabled() || !hasFocused || (isTextEmpty() && !isPwdInputType)) {
            setCompoundDrawablesCompat(null);

            if (!isTextEmpty() && isPwdInputType) {
                invalidate();
            }
        } else {
            if (isPwdInputType) {
                if (mTogglePwdDrawableTint != null)
                    DrawableCompat.setTintList(mTogglePwdDrawable.mutate(), mTogglePwdDrawableTint);
                setCompoundDrawablesCompat(mTogglePwdDrawable);
            } else if (!isTextEmpty() && !disableClear) {
                setCompoundDrawablesCompat(mClearDrawable);
            }
        }
    }

    private void setCompoundDrawablesCompat(Drawable drawableEnd) {
        Drawable[] drawables = TextViewCompat.getCompoundDrawablesRelative(this);
        TextViewCompat.setCompoundDrawablesRelative(this, drawables[0], drawables[1], drawableEnd, drawables[3]);
    }

    private boolean isTextEmpty() {
        return getTextNoneNull().trim().length() == 0;
    }


    // =================================== APIs begin ========================================
    public String getSeparator() {
        return mSeparator;
    }

    /**
     * Set custom separator.
     */
    public void setSeparator(@NonNull String separator) {
        if (mSeparator.equals(separator))
            return;

        mSeparator = separator;
        hasNoSeparator = TextUtils.isEmpty(mSeparator);
        if (mSeparator.length() > 0) {
            int inputType = getInputType();
            if (inputType == 2 || inputType == 8194 || inputType == 4098) {
                // If the inputType is number, the separator can't be inserted, so change to phone type.
                setInputType(InputType.TYPE_CLASS_PHONE);
            }
        }
    }

    /**
     * Set custom pattern.
     *
     * @param pattern   e.g. pattern:{4,4,4,4}, separator:"-" to xxxx-xxxx-xxxx-xxxx
     * @param separator separator
     */
    public void setPattern(@NonNull int[] pattern, @NonNull String separator) {
        setSeparator(separator);
        setPattern(pattern);
    }

    /**
     * Set custom pattern.
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

        InputFilter[] oldFilters = getFilters();
        List<InputFilter> list = new ArrayList<>();
        for (InputFilter filter : oldFilters) {
            if (!(filter instanceof InputFilter.LengthFilter))
                list.add(filter);
        }
        list.add(new InputFilter.LengthFilter(maxLength));

        InputFilter[] newFilters = new InputFilter[list.size()];
        setFilters(list.toArray(newFilters));
    }

    /**
     * Set CharSequence to separate.
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
            setSelection(getTextNoneNull().length());
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
            int maxLength = intervals[intervals.length - 1] + pattern.length - 1;
            int index = Math.min(maxLength, text.length());
            try {
                setSelection(index);
            } catch (IndexOutOfBoundsException e) {
                // Last resort (￣▽￣)
                String message = e.getMessage();
                if (!TextUtils.isEmpty(message) && message.contains(" ")) {
                    int last = message.lastIndexOf(" ");
                    String lenStr = message.substring(last + 1);
                    if (TextUtils.isDigitsOnly(lenStr)) {
                        setSelection(Integer.parseInt(lenStr));
                    }
                }
            }
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
    @NonNull
    public String getTextTrimmed() {
        return getTextEx().trim();
    }

    /**
     * Get text string without separator.
     */
    @NonNull
    public String getTextEx() {
        if (hasNoSeparator) {
            return getTextNoneNull();
        } else {
            return getTextNoneNull().replaceAll(mSeparator, "");
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
            return getTextNoneNull().trim();
        } else {
            return getTextNoneNull().replaceAll(mSeparator, "").trim();
        }
    }

    private String getTextNoneNull() {
        Editable editable = getText();
        return editable == null ? "" : editable.toString();
    }

    public boolean hasNoSeparator() {
        return hasNoSeparator;
    }

    /**
     * Set no separator, just like a @{@link android.widget.EditText}.
     */
    public void setNoSeparator() {
        hasNoSeparator = true;
        mSeparator = "";
        intervals = null;
    }

    public void setClearDrawable(@DrawableRes int resId) {
        mClearResId = resId;
        setClearDrawable(AppCompatResources.getDrawable(getContext(), resId));
    }

    public void setClearDrawable(@Nullable Drawable drawable) {
        if (!disableClear && drawable != null) {
            mClearDrawable = DrawableCompat.wrap(drawable);
            mClearDrawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            if (mClearDrawableTint != null)
                DrawableCompat.setTintList(mClearDrawable.mutate(), mClearDrawableTint);
        }
    }

    public void setTogglePwdDrawables(@DrawableRes int showResId, @DrawableRes int hideResId) {
        mShowPwdResId = showResId;
        mHidePwdResId = hideResId;
        setTogglePwdDrawables(AppCompatResources.getDrawable(getContext(), showResId),
                AppCompatResources.getDrawable(getContext(), showResId));
    }

    public void setTogglePwdDrawables(@Nullable Drawable showDrawable, @Nullable Drawable hideDrawable) {
        if (isPwdShow && showDrawable != null) {
            mTogglePwdDrawable = DrawableCompat.wrap(showDrawable);
            mTogglePwdDrawable.setBounds(0, 0, showDrawable.getIntrinsicWidth(), showDrawable.getIntrinsicHeight());
            if (mTogglePwdDrawableTint != null)
                DrawableCompat.setTintList(mTogglePwdDrawable.mutate(), mTogglePwdDrawableTint);
        }
        if (!isPwdShow && hideDrawable != null) {
            mTogglePwdDrawable = DrawableCompat.wrap(hideDrawable);
            mTogglePwdDrawable.setBounds(0, 0, hideDrawable.getIntrinsicWidth(), hideDrawable.getIntrinsicHeight());
            if (mTogglePwdDrawableTint != null)
                DrawableCompat.setTintList(mTogglePwdDrawable.mutate(), mTogglePwdDrawableTint);
        }
    }

    public void setClearDrawableTint(@NonNull ColorStateList colorStateList) {
        mClearDrawableTint = colorStateList;
        if (mClearDrawable != null)
            DrawableCompat.setTintList(mClearDrawable.mutate(), colorStateList);
    }

    public void setTogglePwdDrawablesTint(@NonNull ColorStateList colorStateList) {
        mTogglePwdDrawableTint = colorStateList;
        if (mTogglePwdDrawable != null)
            DrawableCompat.setTintList(mTogglePwdDrawable.mutate(), colorStateList);
    }

    public void setInteractionPadding(int paddingInDp) {
        if (paddingInDp >= 0) {
            mInteractionPadding = paddingInDp;
            mHalfPadding = paddingInDp >> 1;
        }
    }

    public void setDisableClear(boolean disable) {
        if (disableClear == disable)
            return;

        disableClear = disable;
        if (isPwdInputType && mBitmap != null) {
            int padding = getCompoundDrawablePadding();
            if (disable) {
                padding -= mBitmap.getWidth() + mInteractionPadding;
            } else {
                padding += mBitmap.getWidth() + mInteractionPadding;
            }
            setCompoundDrawablePadding(padding);
        }
    }

    public void setTogglePwdDrawableEnable(boolean enable) {
        if (togglePwdDrawableEnable == enable)
            return;

        togglePwdDrawableEnable = enable;
        dealWithInputTypes(false);
    }

    public void setDisableEmoji(boolean disableEmoji) {
        if (this.disableEmoji == disableEmoji)
            return;

        this.disableEmoji = disableEmoji;

        InputFilter[] oldFilters = getFilters();
        InputFilter[] newFilters;
        if (disableEmoji) {
            newFilters = new InputFilter[oldFilters.length + 1];
            newFilters[oldFilters.length] = new EmojiExcludeFilter();
            System.arraycopy(oldFilters, 0, newFilters, 0, oldFilters.length);
        } else {
            List<InputFilter> list = new ArrayList<>();
            for (InputFilter filter : oldFilters) {
                if (!(filter instanceof EmojiExcludeFilter))
                    list.add(filter);
            }
            newFilters = new InputFilter[list.size()];
            list.toArray(newFilters);
        }
        setFilters(newFilters);
    }

    public void setOnXTextChangeListener(OnXTextChangeListener listener) {
        this.mXTextChangeListener = listener;
    }

    public void setOnXFocusChangeListener(OnXFocusChangeListener listener) {
        mXFocusChangeListener = listener;
    }

    public void setOnClearListener(OnClearListener listener) {
        mOnClearListener = listener;
    }

    /**
     * OnXTextChangeListener is to XEditText what OnTextChangeListener is to EditText.
     */
    public interface OnXTextChangeListener {

        void beforeTextChanged(CharSequence s, int start, int count, int after);

        void onTextChanged(CharSequence s, int start, int before, int count);

        void afterTextChanged(Editable s);
    }

    /**
     * OnXFocusChangeListener is to XEditText what OnFocusChangeListener is to EditText.
     */
    public interface OnXFocusChangeListener {
        void onFocusChange(View v, boolean hasFocus);
    }

    /**
     * Interface definition for a callback to be invoked when the clear drawable is clicked.
     */
    public interface OnClearListener {
        void onClear();
    }

    // =================================== APIs end ========================================

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
     * Disable emoji and other special symbol input.
     */
    private static class EmojiExcludeFilter implements InputFilter {

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