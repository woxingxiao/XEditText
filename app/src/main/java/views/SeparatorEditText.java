package views;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import example.xw.separatoredittext.R;

/**
 * 可按自己想要的格式自动分割显示的EditText
 * 默认手机格式：xxx xxxx xxxx
 * 也可自定义任意格式，如信用卡格式：xxxx-xxxx-xxxx-xxxx 或 xxxx xxxx xxxx xxxx
 * Created by XW on 2015/9/4.
 */
public class SeparatorEditText extends EditText {

    private static final String SPACE = " ";
    private static final int[] DEFAULT_PATTERN = new int[]{3, 4, 4};

    private OnTextChangeListener listener;
    private TextWatcher mTextWatcher;
    private int preLength;
    private int currLength;
    private Drawable clearDrawable;
    private boolean hasFocused;
    private int[] pattern; // 模板
    private int[] intervals; // 根据模板控制分隔符的插入位置
    private String separator = SPACE; // 默认使用空格分割
    private int maxLength; // 根据模板自动计算最大输入长度，超出输入无效。切勿在xml中设置maxLength属性

    public SeparatorEditText(Context context) {
        super(context);
        init();
    }

    public SeparatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SeparatorEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 如果设置 inputType="number" 的话是没法插入空格的，所以强行转为inputType="phone"
        if (getInputType() == InputType.TYPE_CLASS_NUMBER)
            setInputType(InputType.TYPE_CLASS_PHONE);
        setPattern(DEFAULT_PATTERN);

        mTextWatcher = new MyTextWatcher();
        this.addTextChangedListener(mTextWatcher);
        clearDrawable = getCompoundDrawables()[2];
        if (clearDrawable == null) // 如未设置则采用默认
            clearDrawable = getResources().getDrawable(R.drawable.icon_clear);
        if (clearDrawable != null)
            clearDrawable.setBounds(0, 0, clearDrawable.getIntrinsicWidth(), clearDrawable.getIntrinsicHeight());

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                hasFocused = hasFocus;
                initClearMark();
            }
        });
    }

    /**
     * 监听右侧清除图标点击事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (hasFocused && clearDrawable != null && event.getAction() == MotionEvent.ACTION_UP) {
            Rect rect = clearDrawable.getBounds();
            int height = rect.height();
            int distance = (getHeight() - height) / 2;
            boolean isAreaX = event.getX() > (getWidth() - getTotalPaddingRight()) &&
                    event.getX() < (getWidth() - getPaddingRight());
            boolean isAreaY = event.getY() > distance && event.getY() < (distance + height);
            if (isAreaX && isAreaY) {
                setError(null);
                setText("");
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
     * 自定义输入框最右边删除图标
     */
    public void setClearDrawable(int resId) {
        clearDrawable = getResources().getDrawable(resId);
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

    // =========================== MyTextWatcher ================================
    private class MyTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            preLength = s.length();
            if (listener != null)
                listener.beforeTextChanged(s, start, count, after);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            currLength = s.length();

            initClearMark();

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

            if (listener != null)
                listener.onTextChanged(s, start, before, count);
        }


        @Override
        public void afterTextChanged(Editable s) {

            if (listener != null)
                listener.afterTextChanged(s);
        }
    }

    private void initClearMark() {
        if (!hasFocused || currLength == 0) {
            setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                    null, getCompoundDrawables()[3]);
        } else {
            setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1],
                    clearDrawable, getCompoundDrawables()[3]);
        }
    }

    public void setOnTextChangeListener(OnTextChangeListener listener) {
        this.listener = listener;
    }

    public interface OnTextChangeListener {

        void beforeTextChanged(CharSequence s, int start, int count, int after);

        void onTextChanged(CharSequence s, int start, int before, int count);

        void afterTextChanged(Editable s);
    }

}
