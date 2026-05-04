package oxygen.input

import android.app.*
import android.content.*
import android.hardware.*
import android.os.*
import android.text.*
import android.text.InputFilter.*
import android.util.*
import android.view.*
import android.view.View.*
import android.view.inputmethod.*
import android.widget.*
import java.util.*
import oxygen.*
import oxygen.input.*

class AndroidInput(val context: Context, val view: View) :
    Input(), OnKeyListener, OnTouchListener, OnGenericMotionListener {
  private var showingTextInput = false
  var requestFocus = true
  val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

  private val handle = Handler()

  init {
    view.setOnTouchListener(this)
    view.setOnKeyListener(this)
    view.setOnGenericMotionListener(this)
    view.isFocusable = true
    view.isFocusableInTouchMode = true
    view.requestFocus()
  }

  override fun onTouch(view: View, event: MotionEvent): Boolean {
    if (requestFocus && view != null) {
      view.isFocusableInTouchMode = true
      view.requestFocus()
      requestFocus = false
    }
    return Core.bridge.handleTouch(
        MotionEventPacker.packIntData(event),
        MotionEventPacker.packFloatData(event),
    )
  }

  override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
    return Core.bridge.handleKey(keyCode, KeyEventPacker.packIntData(event))
  }

  override fun onGenericMotion(view: View, event: MotionEvent): Boolean {
    return Core.bridge.handleTouch(
        MotionEventPacker.packIntData(event),
        MotionEventPacker.packFloatData(event),
    )
  }

  override fun setOnscreenKeyboardVisible(visible: Boolean) {
    handle.post {
      val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      if (visible) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        manager.showSoftInput(view, 0)
      } else {
        manager.hideSoftInputFromWindow(view.windowToken, 0)
      }
    }
  }

  override fun vibrate(milliseconds: Int) {
    vibrator.vibrate(milliseconds.toLong())
  }

  override fun vibrate(pattern: LongArray, repeat: Int) {
    vibrator.vibrate(pattern, repeat)
  }

  override fun cancelVibrate() {
    vibrator.cancel()
  }

  override fun isShowingTextInput(): Boolean = showingTextInput

  override fun getTextInput(config: TextInputConfig) {
    handle.post {
      val alert = AlertDialog.Builder(context)

      if (config.message.isNotEmpty()) {
        alert.setMessage(config.message)
      }

      val input = EditText(context)
      input.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
      input.setText(config.text)

      if (config.numeric) {
        input.inputType = InputType.TYPE_CLASS_NUMBER
      }

      if (config.maxLength != -1) {
        input.filters = arrayOf<InputFilter>(LengthFilter(config.maxLength))
      }

      if (!config.multiline) {
        input.isSingleLine = true
      }

      try {
        input.setSelection(config.text.length)
      } catch (_: Exception) {}

      if (config.title.isNotEmpty()) {
        alert.setTitle(config.title)
      }

      var dialog: AlertDialog? = null

      alert.setView(input)
      alert.setPositiveButton(android.R.string.ok) { _, _ ->
        handle.post { config.onAccepted(input.text.toString()) }
      }
      alert.setNegativeButton(android.R.string.cancel) { _, _ ->
        handle.post { config.onCanceled() }
      }
      alert.setOnCancelListener { handle.post { config.onCanceled() } }
      alert.setOnDismissListener { showingTextInput = false }

      showingTextInput = true

      dialog = alert.show()

      input.addTextChangedListener(
          object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable) {
              if (!config.allowEmpty) {
                dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                    input.text.toString().trim().isNotEmpty()
              }
            }
          }
      )

      if (!config.allowEmpty) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            input.text.toString().trim().isNotEmpty()
      }

      input.requestFocus()

      dialog.window?.clearFlags(
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
              WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
      )
      dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
  }
}
