[ ![Download](https://api.bintray.com/packages/woxingxiao/maven/xedittext/images/download.svg) ](https://bintray.com/woxingxiao/maven/xedittext/_latestVersion)

# XEditText
Wrapped common usage of `EditText`.

## Screenshot
***
![demo1](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo1.gif) ![demo2](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo2.gif)

![demo3](https://github.com/woxingxiao/XEditText/blob/master/screenshots/demo3.gif)

## Gradle
```groovy
    dependencies{
        // 'com.xw.repo:xedittext:2.0.0@aar'
        compile 'com.xw.repo:xedittext:${LATEST_VERSION}@aar'
    }
```

***
## Usage
```xml
  <com.xw.repo.XEditText
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:hint="default, just likes EditText"
      app:x_disableClear="true"/>

  <com.xw.repo.XEditText
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:hint="clear drawable"/>

  <com.xw.repo.XEditText
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:hint="default password input"
      android:inputType="textPassword"/>

  <com.xw.repo.XEditText
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:hint="pwd input, custom drawables"
      android:inputType="textPassword"
      app:x_clearDrawable="@mipmap/ic_clear" <!--support svg-->
      app:x_hidePwdDrawable="@mipmap/ic_hide" <!--support svg-->
      app:x_showPwdDrawable="@mipmap/ic_show"/> <!--support svg-->
```
Check the sample for more detail.

## License
```
The MIT License (MIT)

Copyright (c) 2016 woxingxiao

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
