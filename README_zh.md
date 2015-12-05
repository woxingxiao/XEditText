##我的特点
- **自带清除功能图标，点击清除输入内容**
- **输入时手机号时自动分割：138 0000 0000，提高用户体验，轻松实现；**
- **支持自定义分割符号和分割模板，如分割银行卡号：6000-0000-0000-0000-000；**
- **支持禁止Emoji表情符号输入；**
- **`drawableRight`自定义，点击监听，配合PopupWindow等进行输入提示；**
- **支持仿iOS输入框风格**
***
##怎么玩儿
###Gradle
```groovy
dependencies{
    compile 'com.xw.repo:xedittext:1.0.1@aar'
}
```

![demo3](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo3.gif) ![demo4](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo4.gif)

![demo4](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo4.gif) ![demo5](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo5.gif)
***
###Attributes
attr | format | describe
-------- | ---|---
x_separator|String|分隔符，默认是一个空格
x_disableEmoji|boolean|禁止Emoji输入, 默认可以输入
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
setDisableEmoji(boolean disableEmoji)|true, 设置禁止Emoji输入
setCustomizeMarkerEnable(boolean customizeMarkerEnable)|设置是否自定义Marker
setOnMarkerClickListener(OnMarkerClickListener markerClickListener)|自定义Marker的点击监听
setShowMarkerTime(ShowMarkerTime showMarkerTime)|设置显示Marker的时间
setiOSStyleEnable(boolean iOSStyleEnable)|设置是否使用iOS风格
