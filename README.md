# SeparatorEditText
EditText输入电话号码、银行卡号自动添加空格分割，自定义分割模式，增加删除图标

![image](https://github.com/woxingxiao/SeparatorEditText/blob/master/demo.gif)

**SeparatorEditText**　的主要方法：

方法名     | 描述
-------- | ---
setSeparator(String separator)| 自定义分隔符，默认是一个空格
setPattern(int[] pattern) |自定义模板，默认常见手机号分割，即int[]{3,4,4} 
setClearDrawable(int resId)| 自定义删除图标的图片资源
setTextToSeparate(CharSequence c)|设置需要自动模板转换的内容
getNonSeparatorText()|获得无分割符的内容
setOnTextChangeListener(OnTextChangeListener listener)|设置输入监听，功能与EditText的addOnTextChangeListener()完全一样
