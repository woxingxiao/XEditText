[**中文说明**](https://github.com/woxingxiao/XEditText/blob/master/README_zh.md)

##What can I do ?
- Deleting function is available. Click the `drawableRight` icon to clear all contents.
- Insert **separator** automatically during inputting. You can customize the **separator** whatever you want (`""`,  `-`, etc.). But you have to set **pattern**, which is kind of a rule you are going to separate the contents.
- Can disable **Emoji** input easily. You don't need to exclude the **Emoji** by youself in codes anymore. <img src="https://s.tylingsoft.com/emoji-icons/stuck_out_tongue_winking_eye.png" width="18"/><img src="https://s.tylingsoft.com/emoji-icons/stuck_out_tongue_winking_eye.png" width="18"/><img src="https://s.tylingsoft.com/emoji-icons/stuck_out_tongue_winking_eye.png" width="18"/>
- `drawableRight` icon, which be called **Marker**, can also be customized. When you do that, for example, you can turn it as an input tips option with a `PopUpWindow` by listening to the **Marker**'s `onMarkerClickListener`.
- iOS style is available. `drawableLeft` and `hint` are both at the center of `EditText` when it has not be focused.

***

##How to use ?

###Gradle
```groovy
dependencies{
    compile 'com.xw.repo:xedittext:1.0.2@aar'
}
```

![demo3](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo3.gif) ![demo4](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo4.gif)

![demo4](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo4.gif) ![demo5](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo5.gif)
***
###Attributes
attr | format | describe
-------- | ---|---
x_separator|String|**separator**, insert automatically during inputting. `""` by default.
x_disableEmoji|boolean|disable **Emoji** or not, `false` by default.
x_customizeMarkerEnable|boolean|customize **Marker** or not, `false` by default.
x_showMarkerTime|enum|set when **Marker** shows, 3 options: `after_input`(by default), `before_input`, `always`
x_iOSStyleEnable|boolean|enable **iOS style** or not, `false` by default.
***
###Methods：
name     | describe
-------- | ---
setSeparator(String separator)| what **separator** you want to set.
setHasNoSeparator(boolean hasNoSeparator)| set none **separator** or not, if set `true`, **separator** equals `""`.
setPattern(int[] pattern) |**pattern** is a kind of rules that you want to separate the contents, for example, credit card input: **separator** = `"-"`, **pattern** = `int[]{4,4,4,4}`, result = xxxx-xxxx-xxxx-xxxx.
setClearDrawable(int resId)|set your `drawableResId` to replace the defalut clear icon.
setTextToSeparate(CharSequence c)|set normal strings to `EditText`, then show separated strings according to  **separator** and **pattern** you've set already.
getNonSeparatorText()|get none **separator**s contents, no matter you've set **separator** or not.
setOnTextChangeListener(OnTextChangeListener listener)|the same as `EditText`'s addOnTextChangeListener() method.
setDisableEmoji(boolean disableEmoji)|disable **Emoji** or not.
setCustomizeMarkerEnable(boolean customizeMarkerEnable)|customize **Marker** or not.
setOnMarkerClickListener(OnMarkerClickListener markerClickListener)|listen to **Marker**'s `onTouch` event.
setShowMarkerTime(ShowMarkerTime showMarkerTime)|set when the **Marker** shows.
setiOSStyleEnable(boolean iOSStyleEnable)|enable **iOS style** or not.