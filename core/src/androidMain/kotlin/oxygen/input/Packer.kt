package oxygen.input

import android.view.*

object MotionEventPacker {

  // [action, actionIndex, pointerCount, historySize, source, flags,
  // edgeFlags, metaState, buttonState, deviceId, downTime, eventTime]
  private const val HEADER_SIZE = 12

  // [pointerId, toolType]
  private const val POINTER_PROPS_PER_POINTER = 2

  // x, y, pressure, touchMajor
  private const val AXIS_PER_POINTER = 4

  /**
   * 打包 int 数据 布局：[action, actionIndex, pointerCount, historySize(固定0), source, flags, edgeFlags,
   * metaState, buttonState, deviceId, downTime, eventTime, pointer0_id, pointer0_toolType,
   * pointer1_id, pointer1_toolType, ...]
   */
  fun packIntData(event: MotionEvent): IntArray {
    val pointerCount = event.pointerCount

    val intData = IntArray(HEADER_SIZE + pointerCount * POINTER_PROPS_PER_POINTER)
    var pos = 0

    intData[pos++] = event.actionMasked
    intData[pos++] = event.actionIndex
    intData[pos++] = pointerCount
    intData[pos++] = 0 // historySize，固定为 0
    intData[pos++] = event.source
    intData[pos++] = event.flags
    intData[pos++] = event.edgeFlags
    intData[pos++] = event.metaState
    intData[pos++] = event.buttonState
    intData[pos++] = event.deviceId
    intData[pos++] = event.downTime.toInt()
    intData[pos++] = event.eventTime.toInt()

    for (i in 0 until pointerCount) {
      intData[pos++] = event.getPointerId(i)
      intData[pos++] = event.getToolType(i)
    }

    return intData
  }

  /**
   * 打包 float 数据（仅当前帧） 布局：[pointer0_x, pointer0_y, pointer0_pressure, pointer0_touchMajor,
   * pointer1_x, pointer1_y, pointer1_pressure, pointer1_touchMajor, ...]
   */
  fun packFloatData(event: MotionEvent): FloatArray {
    val pointerCount = event.pointerCount

    val floatData = FloatArray(pointerCount * AXIS_PER_POINTER)
    var pos = 0

    for (p in 0 until pointerCount) {
      floatData[pos++] = event.getX(p)
      floatData[pos++] = event.getY(p)
      floatData[pos++] = event.getPressure(p)
      floatData[pos++] = event.getTouchMajor(p)
    }

    return floatData
  }
}

object KeyEventPacker {

  // [action, keyCode, repeatCount, metaState, flags, scanCode, deviceId, source, downTime,
  // eventTime, unicodeChar]
  private const val DATA_SIZE = 11

  /** 打包 KeyEvent 核心数据 */
  fun packIntData(event: KeyEvent): IntArray {
    val data = IntArray(DATA_SIZE)
    var pos = 0

    data[pos++] = event.action // ACTION_DOWN / ACTION_UP / ACTION_MULTIPLE
    data[pos++] = event.keyCode // 按键码，如 KEYCODE_ENTER
    data[pos++] = event.repeatCount // 重复次数（长按）
    data[pos++] = event.metaState // 修饰键状态 (Ctrl/Alt/Shift)
    data[pos++] = event.flags // 标志位
    data[pos++] = event.scanCode // 硬件扫描码
    data[pos++] = event.deviceId // 输入设备ID
    data[pos++] = event.source // 输入源
    data[pos++] = event.downTime.toInt() // 按下时间
    data[pos++] = event.eventTime.toInt() // 事件时间
    data[pos++] = event.unicodeChar // 可打印字符的 Unicode 码点

    return data
  }
}
