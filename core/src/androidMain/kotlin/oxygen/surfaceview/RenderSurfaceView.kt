package oxygen.surfaceview

import android.content.Context
import android.graphics.PixelFormat
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import oxygen.bridge.*
import oxygen.util.*

interface Renderer {
  fun onSurfaceCreated(surface: Surface)

  fun onSurfaceChanged(surface: Surface, width: Int, height: Int)

  fun onSurfaceDestroyed()
}

open class RenderSurfaceView
@JvmOverloads
constructor(
    context: Context,
    val resolutionStrategy: ResolutionStrategy,
    var renderer: Renderer? = null,
) : SurfaceView(context), SurfaceHolder.Callback {

  val currentSurface: Surface?
    get() = holder.surface

  init {
    holder.addCallback(this)
    holder.setFormat(PixelFormat.RGBA_8888)
    setFocusable(true)
    setFocusableInTouchMode(true)
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    renderer?.onSurfaceCreated(holder.surface!!)
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    renderer?.onSurfaceChanged(holder.surface!!, width, height)
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    renderer?.onSurfaceDestroyed()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val dim = resolutionStrategy.calcMeasures(widthMeasureSpec, heightMeasureSpec)
    setMeasuredDimension(dim.width, dim.height)
  }
}

class JvmRenderer(val bridge: OxygenBridge) : Renderer {
  override fun onSurfaceCreated(surface: Surface) {
    Log.info("[OxygenLauncher]On surface create")
    bridge.onSurfaceCreated(surface as Object)
  }

  override fun onSurfaceChanged(surface: Surface, width: Int, height: Int) {
    Log.info("[OxygenLauncher]On surface changed $width x $height")
    bridge.onSurfaceChanged(surface as Object, width, height)
  }

  override fun onSurfaceDestroyed() {
    Log.info("[OxygenLauncher]On surface destroyed")
    bridge.onSurfaceDestroyed()
  }
}
