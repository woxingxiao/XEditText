# XEditText

![image](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo.gif)
![image](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo2.gif)
***
###Attributes
attr | format | describe
-------- | ---|---
x_separator|String|分隔符，默认是一个空格
x_customizeMarkerEnable|boolean|是否自定义Marker
x_showMarkerTime|enum|显示Marker的时间：after_input(default), before_input, always
x_iOSStyleEnable|boolean|是否使用iOS风格
***
###Methods：
方法名     | 描述
-------- | ---
setSeparator(String separator)| 自定义分隔符，默认是一个空格
setHasNoSeparator(boolean hasNoSeparator)| 设置无分隔符，功能同普通EditText
setPattern(int[] pattern) |自定义模板，默认常见手机号分割，即int[]{3,4,4}
setClearDrawable(int resId)| 自定义删除图标的图片资源
setTextToSeparate(CharSequence c)|设置需要自动模板转换的内容
getNonSeparatorText()|获得无分割符的内容
setOnTextChangeListener(OnTextChangeListener listener)|设置输入监听，功能与EditText的addOnTextChangeListener()完全一样
setCustomizeMarkerEnable(boolean customizeMarkerEnable)|设置是否自定义Marker
setOnMarkerClickListener(OnMarkerClickListener markerClickListener)|自定义Marker的点击监听
setShowMarkerTime(ShowMarkerTime showMarkerTime)|设置显示Marker的时间
setiOSStyleEnable(boolean iOSStyleEnable)|设置是否使用iOS风格
