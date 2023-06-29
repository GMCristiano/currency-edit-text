![Maven Central](https://img.shields.io/maven-central/v/io.github.gmcristiano/currency-edit-text?versionPrefix=1.0.1)

# CurrencyEditText

Android EditText for currency values. It supports grouping and decimal separators from different cultures.

<img src="docs/example.gif" width="180" height="400"/>

### Usage

```kotlin
dependencies {
  implementation 'io.github.gmcristiano:currency-edit-text:1.0.1'
}
```

```xml
<io.github.gmcristiano.currencyedittext.CurrencyEditText
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:digitsBeforeZero="13"
    app:digitsAfterZero="4" />
```

# License

```
Copyright 2023 Cristiano Correia

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```