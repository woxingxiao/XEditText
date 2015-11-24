package com.xw.repo.xedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

/**
 * 可按自己想要的格式自动分割显示的EditText
 * 默认手机格式：xxx xxxx xxxx
 * 也可自定义任意格式，如信用卡格式：xxxx-xxxx-xxxx-xxxx 或 xxxx xxxx xxxx xxxx
 * 使用pattern时无需在xml中设置maxLength属性，若需要设置时应注意加上分隔符的数量
 * Created by woxingxiao on 2015/9/4.
 */
public class XEditText extends EditText {

    private static final String SPACE = " ";
    private static final int[] DEFAULT_PATTERN = new int[]{3, 4, 4};

    private OnTextChangeListener mTextChangeListener;
    private OnMarkerClickListener mMarkerClickListener;
    private TextWatcher mTextWatcher;
    private int preLength;
    private int currLength;
    private Drawable mRightMarkerDrawable;
    private Drawable mLeftDrawable;
    private boolean hasFocused;
    private int[] pattern; // 模板
    private int[] intervals; // 根据模板控制分隔符的插入位置
    private String separator; //分割符，默认使用空格分割
    // 根据模板自动计算最大输入长度，超出输入无效。使用pattern时无需在xml中设置maxLength属性，若需要设置时应注意加上分隔符的数量
    private int maxLength;
    private boolean hasNoSeparator; // 设置为true时功能同EditText
    private boolean customizeMarkerEnable; // 自定义右侧Marker点击选项使能
    private ShowMarkerTime mShowMarkerTime; // 自定义选项后选项显示的时间，默认输入后显示
    private Paint mTextPaint;
    private Rect mRect;
    private Rect mTextRect;
    private Bitmap mBitmap;
    private Paint mBitPaint;
    private boolean iOSStyleEnable; // 仿iOS风格，目前需要结合shape.xml的方式设置外边框
    private boolean iOSFrameHide;
    private CharSequence mHintCharSeq;

    public XEditText(Context context) {
        this(context, null);
    }

    public XEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle); // Attention !
    }

    public XEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XEditText, defStyleAttr, 0);

        separator = a.getString(R.styleable.XEditText_x_separator);
        if (separator == null) separator = SPACE;
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
        a.recycle();

        init();
    }

    private void init() {
        // 如果设置 inputType="number" 的话是没法插入空格的，所以强行转为inputType="phone"
        if (getInputType() == InputType.TYPE_CLASS_NUMBER)
            setInputType(InputType.TYPE_CLASS_PHONE);
        setPattern(DEFAULT_PATTERN);

        mTextWatcher = new MyTextWatcher();
        this.addTextChangedListener(mTextWatcher);
        mRightMarkerDrawable = getCompoundDrawables()[2];
        if (customizeMarkerEnable && mRightMarkerDrawable != null) { // 如果自定义Marker，暂时不显示rightDrawable
            setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                    null, getCompoundDrawables()[3]);
        }
        if (mRightMarkerDrawable == null) { // 如未设置则采用默认
            mRightMarkerDrawable = getResources().getDrawable(R.drawable.icon_clear);
            if (mRightMarkerDrawable != null)
                mRightMarkerDrawable.setBounds(0, 0, mRightMarkerDrawable.getIntrinsicWidth(), mRightMarkerDrawable.getIntrinsicHeight());
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
            if (iOSFrameHide) return;

            if (mHintCharSeq != null) {
                Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
                int textCenterY = (mRect.bottom + mRect.top - fontMetrics.bottom - fontMetrics.top) / 2;
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
     * 监听右侧Marker图标点击事件
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
                    if (mMarkerClickListener != null)
                        mMarkerClickListener.onMarkerClick(event.getRawX(), event.getRawY());
                } else {
                    setError(null);
                    setText("");
                }
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 自定义分隔符
     */
    public void setSeparator(@NonNull String separator) {
        if (separator == null) {
            throw new IllegalArgumentException("separator can't be null !");
        }
        this.separator = separator;
    }

    /**
     * 自定义分割模板
     *
     * @param pattern 每一段的字符个数的数组
     */
    public void setPattern(@NonNull int[] pattern) {
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
                count++;
        }
        maxLength = intervals[intervals.length - 1];
    }

    /**
     * 自定义输入框最右边Marker图标
     */
    public void setRightMarkerDrawable(int resId) {
        mRightMarkerDrawable = getResources().getDrawable(resId);
    }

    /**
     * 输入待转换格式的字符串
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
     * 获得除去分割符的输入框内容
     */
    public String getNonSeparatorText() {
        return getText().toString().replaceAll(separator, "");
    }

    /**
     * 是否自定义Marker
     */
    public void setCustomizeMarkerEnable(boolean customizeMarkerEnable) {
        this.customizeMarkerEnable = customizeMarkerEnable;
        if (customizeMarkerEnable && mRightMarkerDrawable != null) { // 如果自定义Marker，暂时不显示rightDrawable
            setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                    null, getCompoundDrawables()[3]);
        }
    }

    /**
     * Marker在什么时间显示
     *
     * @param showMarkerTime BEFORE_INPUT：没有输入内容时显示；
     *                       AFTER_INPUT：有输入内容后显示；
     *                       ALWAYS：（获得焦点后）一直显示
     */
    public void setShowMarkerTime(ShowMarkerTime showMarkerTime) {
        mShowMarkerTime = showMarkerTime;
    }

    /**
     * @return 是否有分割符
     */
    public boolean hasNoSeparator() {
        return hasNoSeparator;
    }

    /**
     * @param hasNoSeparator true设置无分隔符模式，功能同EditText
     */
    public void setHasNoSeparator(boolean hasNoSeparator) {
        this.hasNoSeparator = hasNoSeparator;
        if (hasNoSeparator) separator = "";
    }

    /**
     * @param iOSStyleEnable true:开启仿iOS风格编辑框模式
     */
    public void setiOSStyleEnable(boolean iOSStyleEnable) {
        this.iOSStyleEnable = iOSStyleEnable;
        initiOSObjects();
        invalidate();
    }

    /**
     * 设置OnTextChangeListener，同EditText.addOnTextChangeListener()
     */
    public void setOnTextChangeListener(OnTextChangeListener listener) {
        this.mTextChangeListener = listener;
    }

    /**
     * 设置OnMarkerClickListener，Marker被点击的监听
     */
    public void setOnMarkerClickListener(OnMarkerClickListener markerClickListener) {
        mMarkerClickListener = markerClickListener;
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
            currLength = s.length();
            if (hasNoSeparator) maxLength = currLength;

            markerFocusChangeLogic();

            if (currLength > maxLength) {
                getText().delete(currLength - 1, currLength);
                return;
            }

            for (int i = 0; i < pattern.length; i++) {
                if (currLength == intervals[i]) {
                    if (currLength > preLength) { // 正在输入
                        if (currLength < maxLength) {
                            removeTextChangedListener(mTextWatcher);
                            mTextWatcher = null;
                            getText().insert(currLength, separator);
                        }
                    } else if (preLength <= maxLength) { // 正在删除
                        removeTextChangedListener(mTextWatcher);
                        mTextWatcher = null;
                        getText().delete(currLength - 1, currLength);
                    }

                    if (mTextWatcher == null) {
                        mTextWatcher = new MyTextWatcher();
                        addTextChangedListener(mTextWatcher);
                    }

                    break;
                }
            }

            if (mTextChangeListener != null)
                mTextChangeListener.onTextChanged(s, start, before, count);
        }


        @Override
        public void afterTextChanged(Editable s) {

            if (mTextChangeListener != null)
                mTextChangeListener.afterTextChanged(s);
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
        if (!iOSStyleEnable) return;
        if (hasFocused) {
            if (mLeftDrawable != null)
                setCompoundDrawables(mLeftDrawable, getCompoundDrawables()[1],
                        getCompoundDrawables()[2], getCompoundDrawables()[3]);
            if (mHintCharSeq != null)
                setHint(mHintCharSeq);
            iOSFrameHide = true;
            invalidate();
        } else {
            if (currLength == 0) { // 编辑框无内容恢复居中状态
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
         * @param x 被点击点相对于屏幕的x坐标
         * @param y 被点击点相对于屏幕的y坐标
         */
        void onMarkerClick(float x, float y);
    }

    public enum ShowMarkerTime {
        BEFORE_INPUT,
        AFTER_INPUT,
        ALWAYS
    }

}