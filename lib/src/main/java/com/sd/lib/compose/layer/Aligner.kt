package com.sd.lib.compose.layer

/**
 * 对齐接口
 */
internal interface Aligner {

   enum class Position {
      /** 顶部开始方向对齐 */
      TopStart,
      /** 顶部中间对齐 */
      TopCenter,
      /** 顶部结束方向对齐 */
      TopEnd,
      /** 顶部对齐，不计算x坐标，默认x坐标为0 */
      Top,

      /** 底部开始方向对齐 */
      BottomStart,
      /** 底部中间对齐 */
      BottomCenter,
      /** 底部结束方向对齐 */
      BottomEnd,
      /** 底部对齐，不计算x坐标，默认x坐标为0 */
      Bottom,

      /** 开始方向顶部对齐 */
      StartTop,
      /** 开始方向中间对齐 */
      StartCenter,
      /** 开始方向底部对齐 */
      StartBottom,
      /** 开始方向对齐，不计算y坐标，默认y坐标为0 */
      Start,

      /** 结束方向顶部对齐 */
      EndTop,
      /** 结束方向中间对齐 */
      EndCenter,
      /** 结束方向底部对齐 */
      EndBottom,
      /** 结束方向对齐，不计算y坐标，默认y坐标为0 */
      End,

      /** 中间对齐 */
      Center,
   }

   data class Input(
      val position: Position,

      // 目标
      val targetX: Int,
      val targetY: Int,
      val targetWidth: Int,
      val targetHeight: Int,

      // 源容器
      val containerX: Int,
      val containerY: Int,
      val containerWidth: Int,
      val containerHeight: Int,

      // 源
      val sourceWidth: Int,
      val sourceHeight: Int,
   )

   data class Result(
      /** 输入参数 */
      val input: Input,

      /** 源相对于源容器的x值 */
      val x: Int,

      /** 源相对于源容器的y值 */
      val y: Int,
   ) {
      /**
       * 源相对于源容器的溢出信息
       */
      val sourceOverflow: Overflow
         get() = Overflow(
            start = -x,
            end = (x + input.sourceWidth) - input.containerWidth,
            top = -y,
            bottom = (y + input.sourceHeight) - input.containerHeight,
         )

      /**
       * 输入的参数相对于源容器的溢出信息
       */
      fun overflow(
         x: Int,
         y: Int,
         width: Int,
         height: Int,
      ): Overflow {
         return Companion.overflow(
            parentX = input.containerX,
            parentY = input.containerY,
            parentWidth = input.containerWidth,
            parentHeight = input.containerHeight,
            childX = x,
            childY = y,
            childWidth = width,
            childHeight = height,
         )
      }
   }

   /**
    * 4条边距离源容器4条边的值。值大于0表示溢出，值小于0表示未溢出。
    */
   data class Overflow(
      val start: Int,
      val end: Int,
      val top: Int,
      val bottom: Int,
   ) {
      /**
       * 4条边溢出容器的总值
       */
      fun totalOverflow(): Int {
         var size = 0
         if (top > 0) size += top
         if (bottom > 0) size += bottom
         if (start > 0) size += start
         if (end > 0) size += end
         return size
      }
   }

   companion object {
      @JvmStatic
      fun overflow(
         parentX: Int,
         parentY: Int,
         parentWidth: Int,
         parentHeight: Int,
         childX: Int,
         childY: Int,
         childWidth: Int,
         childHeight: Int,
      ): Overflow {

         val parentEnd = parentX + parentWidth
         val parentBottom = parentY + parentHeight

         val childEnd = childX + childWidth
         val childBottom = childY + childHeight

         return Overflow(
            start = parentX - childX,
            top = parentY - childY,
            end = childEnd - parentEnd,
            bottom = childBottom - parentBottom,
         )
      }
   }
}

//-------------------- impl --------------------

internal fun Aligner.Input.toResult(): Aligner.Result {
   var x = 0
   var y = 0

   when (this.position) {
      Aligner.Position.TopStart -> {
         y = getYAlignTop() - this.sourceHeight
         x = getXAlignStart()
      }

      Aligner.Position.TopCenter -> {
         y = getYAlignTop() - this.sourceHeight
         x = getXAlignCenter()
      }

      Aligner.Position.TopEnd -> {
         y = getYAlignTop() - this.sourceHeight
         x = getXAlignEnd()
      }

      Aligner.Position.Top -> {
         y = getYAlignTop() - this.sourceHeight
         x = 0
      }

      Aligner.Position.BottomStart -> {
         y = getYAlignBottom() + this.sourceHeight
         x = getXAlignStart()
      }

      Aligner.Position.BottomCenter -> {
         y = getYAlignBottom() + this.sourceHeight
         x = getXAlignCenter()
      }

      Aligner.Position.BottomEnd -> {
         y = getYAlignBottom() + this.sourceHeight
         x = getXAlignEnd()
      }

      Aligner.Position.Bottom -> {
         y = getYAlignBottom() + this.sourceHeight
         x = 0
      }

      Aligner.Position.StartTop -> {
         x = getXAlignStart() - this.sourceWidth
         y = getYAlignTop()
      }

      Aligner.Position.StartCenter -> {
         x = getXAlignStart() - this.sourceWidth
         y = getYAlignCenter()
      }

      Aligner.Position.StartBottom -> {
         x = getXAlignStart() - this.sourceWidth
         y = getYAlignBottom()
      }

      Aligner.Position.Start -> {
         x = getXAlignStart() - this.sourceWidth
         y = 0
      }

      Aligner.Position.EndTop -> {
         x = getXAlignEnd() + this.sourceWidth
         y = getYAlignTop()
      }

      Aligner.Position.EndCenter -> {
         x = getXAlignEnd() + this.sourceWidth
         y = getYAlignCenter()
      }

      Aligner.Position.EndBottom -> {
         x = getXAlignEnd() + this.sourceWidth
         y = getYAlignBottom()
      }

      Aligner.Position.End -> {
         x = getXAlignEnd() + this.sourceWidth
         y = 0
      }

      Aligner.Position.Center -> {
         x = getXAlignCenter()
         y = getYAlignCenter()
      }
   }

   return Aligner.Result(this, x, y)
}

private fun Aligner.Input.getXAlignStart(): Int {
   return this.targetX - this.containerX
}

private fun Aligner.Input.getXAlignEnd(): Int {
   return getXAlignStart() + (this.targetWidth - this.sourceWidth)
}

private fun Aligner.Input.getXAlignCenter(): Int {
   return getXAlignStart() + (this.targetWidth - this.sourceWidth) / 2
}

private fun Aligner.Input.getYAlignTop(): Int {
   return this.targetY - this.containerY
}

private fun Aligner.Input.getYAlignBottom(): Int {
   return getYAlignTop() + (this.targetHeight - this.sourceHeight)
}

private fun Aligner.Input.getYAlignCenter(): Int {
   return getYAlignTop() + (this.targetHeight - this.sourceHeight) / 2
}