package io.github.gmcristiano.currencyedittext

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*

class CurrencyEditText(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {

  private var locale = Locale.getDefault()
  private var digitsBeforeZero = Int.MAX_VALUE
  private var digitsAfterZero = 2
  private var groupingSeparator = ','
  private var decimalSeparator = '.'
  private var defaultText: String = ""
  private var numberFilterRegex: String = ""
  private val numericListeners: MutableList<NumericValueWatcher> = ArrayList()
  private var previousText = ""
  private var previousSelectionEnd = 0

  //region TextWatcher
  private val mTextWatcher: TextWatcher = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
      previousText = text.toString()
      previousSelectionEnd = selectionEnd
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable) {
      var textChanged = s.toString()

      if (textChanged.isEmpty()) {
        handleNumericValueCleared()
        return
      }

      // Deal with clear number with groupingSeparator before cursor.
      if (textChanged.count { it == groupingSeparator } < previousText.count { it == groupingSeparator } &&
        textChanged.length == previousText.length - 1 &&
        textChanged.length >= selectionEnd && // hammer concurrency? - component sometimes has selectionEnd above
        selectionEnd > 0 && // hammer - component sometimes has negative value
        previousText.substring(selectionEnd, selectionEnd+1).firstOrNull() == groupingSeparator // allow only backspace
      ) {
        textChanged = textChanged.removeRange(selectionEnd - 1, selectionEnd)
      }

      // If user presses Non DECIMAL_SEPARATOR, convert it to correct DECIMAL_SEPARATOR
      if (!Character.isDigit(textChanged[textChanged.length - 1]) &&
        !textChanged.endsWith(decimalSeparator.toString()) &&
        (textChanged.endsWith(",") || textChanged.endsWith("."))
      ) {
        textChanged = textChanged.substring(0, textChanged.length - 1) + decimalSeparator
      }

      // Limit decimal digits.
      // valid decimal number should not have thousand separators after a decimal separators
      // valid decimal number should not have more than 2 decimal separators
      if (digitLimitBeforeZeroReached(textChanged) ||
        digitLimitAfterZeroReached(textChanged) ||
        hasGroupingSeparatorAfterDecimalSeparator(textChanged) ||
        countMatches(textChanged, decimalSeparator.toString()) > 1
      ) {
        setTextInternal(previousText) // cancel change and revert to previous input
        setSelection(previousSelectionEnd)
        return
      }

      // If only decimal separator is inputted, add a zero to the left of it
      if (textChanged == decimalSeparator.toString()) {
        textChanged = "0$textChanged"
      }

      val textChangedFormatted = format(textChanged)
      setTextInternal(textChangedFormatted)
      val offSetSelection = text.toString().length - previousText.length
      setSelection(maxOf(previousSelectionEnd + offSetSelection, 0))
      handleNumericValueChanged()
    }
  }
  //endregion

  init {
    context.theme?.obtainStyledAttributes(
      attrs, R.styleable.CurrencyEditText,
      0, 0,
    )?.apply {

      try {
        digitsBeforeZero = getInteger(R.styleable.CurrencyEditText_digitsBeforeZero, digitsBeforeZero)
        digitsAfterZero = getInteger(R.styleable.CurrencyEditText_digitsAfterZero, digitsAfterZero)
      } finally {
        recycle()
      }
    }

    inputType = InputType.TYPE_CLASS_PHONE
    reload()
    addTextChangedListener(mTextWatcher)
  }

  fun setLocale(locale: Locale) {
    this.locale = locale
    reload()
  }

  fun setDigitsBeforeZero(digitsBeforeZero: Int) {
    this.digitsBeforeZero = digitsBeforeZero
  }

  fun setDigitsAfterZero(digitsAfterZero: Int) {
    this.digitsAfterZero = digitsAfterZero
  }

  private fun reload() {
    val symbols = DecimalFormatSymbols(locale)
    groupingSeparator = symbols.groupingSeparator
    decimalSeparator = symbols.decimalSeparator
    numberFilterRegex = "[^\\d\\$decimalSeparator]"
  }

  private fun handleNumericValueCleared() {
    for (listener in numericListeners) {
      listener.onCleared()
    }
  }

  private fun handleNumericValueChanged() {
    for (listener in numericListeners) {
      listener.onChanged(numericValue)
    }
  }

  fun addNumericValueChangedListener(watcher: NumericValueWatcher) {
    numericListeners.add(watcher)
  }

  fun removeAllNumericValueChangedListeners() {
    while (numericListeners.isNotEmpty()) {
      numericListeners.removeAt(0)
    }
  }

  fun setDefaultNumericValue(defaultNumericValue: Double, defaultNumericFormat: String) {
    defaultText = String.format(defaultNumericFormat, defaultNumericValue)
    setTextInternal(defaultText)
  }

  fun clear() {
    setTextInternal(defaultText)
    handleNumericValueChanged()
  }

  private val numericValue: Double
    get() {
      return try {
        val format = NumberFormat.getInstance(locale)
        val parse = format.parse(text.toString())
        parse!!.toDouble()
      } catch (e: Exception) {
        Double.NaN
      }
    }

  private fun format(original: String): String? {
    val parts = splitOriginalText(original)
    var number: String? = parts[0].replace(numberFilterRegex.toRegex(), "")
      .replaceFirst("^0+(?!$)".toRegex(), "")
    number = reverse(reverse(number)!!.replace("(.{3})".toRegex(), "$1$groupingSeparator"))
    number = removeStart(number, groupingSeparator.toString())
    if (parts.size > 1) {
      parts[1] = parts[1].replace(numberFilterRegex.toRegex(), "")
      number += decimalSeparator + parts[1]
    }
    if (number.equals("0")) {
      return ""
    }
    return number
  }

  private fun splitOriginalText(original: String): Array<String> {
    return original.split(("\\$decimalSeparator").toRegex()).toTypedArray()
  }

  private fun setTextInternal(text: String?) {
    removeTextChangedListener(mTextWatcher)
    setText(text)
    addTextChangedListener(mTextWatcher)
  }

  private fun reverse(original: String?): String? {
    return if (original == null || original.length <= 1) {
      original
    } else {
      original.reversed()
    }
  }

  private fun removeStart(str: String?, remove: String): String? {
    if (TextUtils.isEmpty(str)) {
      return str
    }
    return if (str!!.startsWith(remove)) {
      str.substring(remove.length)
    } else {
      str
    }
  }

  private fun countMatches(str: String, sub: String): Int {
    if (TextUtils.isEmpty(str)) {
      return 0
    }
    val lastIndex = str.lastIndexOf(sub)
    return if (lastIndex < 0) {
      0
    } else {
      1 + countMatches(str.substring(0, lastIndex), sub)
    }
  }

  private fun hasGroupingSeparatorAfterDecimalSeparator(text: String): Boolean {
    // Return true if thousand separator (.) comes after a decimal separator. (,)
    if (text.contains("$groupingSeparator") && text.contains("$decimalSeparator")) {
      val firstIndexOfDecimal = text.indexOf(decimalSeparator)
      val lastIndexOfGrouping = text.lastIndexOf(groupingSeparator)
      if (firstIndexOfDecimal < lastIndexOfGrouping) {
        return true
      }
    }
    return false
  }

  private fun digitLimitBeforeZeroReached(text: String): Boolean {
    // Return true if limit is reached
    var part = text
    if (text.contains("$decimalSeparator")) {
      // Dot is special character in regex, so we have to treat it specially.
      val parts = splitOriginalText(text)
      if (parts.size > 1) {
        part = parts[0]
      }
    }
    part = part.replace(groupingSeparator.toString(), "")
    if (part.length > digitsBeforeZero) {
      return true
    }
    return false
  }

  private fun digitLimitAfterZeroReached(text: String): Boolean {
    // Return true if limit is reached
    if (text.contains("$decimalSeparator")) {
      // Dot is special character in regex, so we have to treat it specially.
      val parts = splitOriginalText(text)
      if (parts.size > 1) {
        val lastPart = parts[parts.size - 1]
        if (lastPart.length > digitsAfterZero) {
          return true
        }
      }
    }
    return false
  }

  interface NumericValueWatcher {
    fun onChanged(newValue: Double)
    fun onCleared()
  }
}
