/*
 * Copyright 2010 Mario Zechner (contact@badlogicgames.com), Nathan Sweet (admin@esotericsoftware.com), Dave Clayton (contact@redskyforge.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package oxygen.surfaceview

import android.view.View
import kotlin.math.roundToInt

/**
 * Will manipulate the GLSurfaceView. Gravity is always center. The width and height of the View
 * will be determined by the classes implementing [ResolutionStrategy].
 *
 * @author christoph widulle
 */
interface ResolutionStrategy {
  fun calcMeasures(widthMeasureSpec: Int, heightMeasureSpec: Int): MeasuredDimension

  class MeasuredDimension(val width: Int, val height: Int)
}

/**
 * This [ResolutionStrategy] will stretch the GLSurfaceView to full screen. FillResolutionStrategy
 * is the default [ResolutionStrategy] if none is specified.
 *
 * @author christoph widulle
 */
class FillResolutionStrategy : ResolutionStrategy {

  override fun calcMeasures(
      widthMeasureSpec: Int,
      heightMeasureSpec: Int,
  ): ResolutionStrategy.MeasuredDimension {
    val width = View.MeasureSpec.getSize(widthMeasureSpec)
    val height = View.MeasureSpec.getSize(heightMeasureSpec)

    return ResolutionStrategy.MeasuredDimension(width, height)
  }
}

/**
 * This [ResolutionStrategy] will place the GLSurfaceView with the given height and width in the
 * center the screen.
 *
 * @author christoph widulle
 */
class FixedResolutionStrategy(private val width: Int, private val height: Int) :
    ResolutionStrategy {

  override fun calcMeasures(
      widthMeasureSpec: Int,
      heightMeasureSpec: Int,
  ): ResolutionStrategy.MeasuredDimension {
    return ResolutionStrategy.MeasuredDimension(width, height)
  }
}

/**
 * This [ResolutionStrategy] will maintain a given aspect ratio and stretch the GLSurfaceView to the
 * maximum available screen size.
 *
 * @author christoph widulle
 */
open class RatioResolutionStrategy : ResolutionStrategy {
  private val ratio: Float

  constructor(ratio: Float) {
    this.ratio = ratio
  }

  constructor(width: Float, height: Float) {
    this.ratio = width / height
  }

  override fun calcMeasures(
      widthMeasureSpec: Int,
      heightMeasureSpec: Int,
  ): ResolutionStrategy.MeasuredDimension {
    val specWidth = View.MeasureSpec.getSize(widthMeasureSpec)
    val specHeight = View.MeasureSpec.getSize(heightMeasureSpec)

    val desiredRatio = ratio
    val realRatio = specWidth.toFloat() / specHeight

    val width: Int
    val height: Int
    if (realRatio < desiredRatio) {
      width = specWidth
      height = (width / desiredRatio).roundToInt()
    } else {
      height = specHeight
      width = (height * desiredRatio).roundToInt()
    }

    return ResolutionStrategy.MeasuredDimension(width, height)
  }
}
