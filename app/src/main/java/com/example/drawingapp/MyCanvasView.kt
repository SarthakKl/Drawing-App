package com.example.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat

class MyCanvasView(context: Context,attrs: AttributeSet): View(context,attrs) {
    private var extraCanvas:Canvas?=null
    private var extraBitmap: Bitmap?=null
    private var brushSize=0.0f
    private var canvasPaint: Paint?=null
    private var drawPaint:Paint?=null
    private var color=Color.BLACK
    private var drawPath:CustomPath?=null
    private val allPaths=ArrayList<CustomPath>()
    private val undoPaths=ArrayList<CustomPath>()

    init{
        setUpDrawing()
    }
    private fun setUpDrawing(){
        drawPaint=Paint()
        drawPath=CustomPath(color,brushSize)
        drawPaint!!.color=color
        drawPaint!!.style=Paint.Style.STROKE
        drawPaint!!.strokeJoin=Paint.Join.ROUND
        drawPaint!!.strokeCap=Paint.Cap.ROUND
        canvasPaint=Paint(Paint.DITHER_FLAG)
    }
    internal inner class CustomPath(var color:Int,var brushThickness:Float): Path() {

    }
    fun setSizeForBrush(newSize:Float){
        brushSize=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        // Size can be different for different screen size. So the above implementation will set the brush
        // size accordingly to dp(density independent pixels)
    }
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap!!,0f,0f,canvasPaint)

        for(paths in allPaths){
            drawPaint!!.color=paths.color
            drawPaint!!.strokeWidth=paths.brushThickness
            canvas.drawPath(paths,drawPaint!!)
        }
        if(!drawPath!!.isEmpty){
            drawPaint!!.color=drawPath!!.color
            drawPaint!!.strokeWidth=drawPath!!.brushThickness
            canvas.drawPath(drawPath!!,drawPaint!!)
        }

    }
    fun undo(){
        if(allPaths.size>0)
            undoPaths.add(allPaths.removeAt(allPaths.size-1))
        invalidate()
    }
    fun setColor(newColor:String){
        color=Color.parseColor(newColor)
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchx=event?.x
        val touchy=event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                // Action down means we will draw and action up means we will release our touch
                //NOTE
                // We have declared the color and brushThickness in the customPath() class
                // so we can have different colors and brushThickness for every path we draw.
                // And the global color and brushSize denotes the current color and brushThickness
                // which is being used.
                drawPath!!.color=color
                drawPath!!.brushThickness=brushSize

                drawPath!!.reset()

                if(touchx!=null){
                    if(touchy!=null){
                        drawPath!!.moveTo(touchx,touchy)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchx != null) {
                    if (touchy != null) {
                        drawPath!!.lineTo(touchx,touchy)
                    }
                }
            }
            MotionEvent.ACTION_UP ->{
                MainActivity.imageSaved=false
                allPaths.add(drawPath!!)
                drawPath=CustomPath(color,brushSize)
            }
            else -> return false

        }

        invalidate()
        return true
    }
}