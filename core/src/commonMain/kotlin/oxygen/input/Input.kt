package oxygen.input

abstract class Input {
  abstract fun getTextInput(config: TextInputConfig)

  open fun isShowingTextInput(): Boolean = false

  open fun setOnscreenKeyboardVisible(visible: Boolean) {}

  open fun vibrate(milliseconds: Int) {}

  open fun vibrate(pattern: LongArray, repeat: Int) {}

  open fun cancelVibrate() {}
}

data class TextInputConfig(
    val title: String = "",
    val message: String = "",
    val text: String = "",
    val numeric: Boolean = false,
    val multiline: Boolean = true,
    val maxLength: Int = -1,
    val allowEmpty: Boolean = true,
    val onAccepted: (String) -> Unit = {},
    val onCanceled: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)
