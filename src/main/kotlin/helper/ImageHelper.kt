package helper

import com.intellij.util.ui.UIUtil
import helper.EmulatorHelper.getScreenSize
import main.kotlin.model.State
import main.kotlin.model.Transition
import main.kotlin.model.TransitionType
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.io.File
import java.lang.Exception
import javax.imageio.ImageIO

fun compareImage(fileA: File, fileB: File): Float{
    var percentage: Float = 0F
     try{
        val dbA = ImageIO.read(fileA).let { it.data.dataBuffer }
        val dbB = ImageIO.read(fileB).let { it.data.dataBuffer }
        val sizeA = dbA.size
        val sizeB = dbB.size
        var count = 0
         if(sizeA == sizeB){
            for(i in 0..sizeA){
                if (dbA.getElem(i) == dbB.getElem(i)){
                    count++
                }
            }
             percentage = ((count*100)/sizeA).toFloat()
         }else{
             println("Both the images are not of same size")
         }
    }catch (e: Exception){
        println("Failed to compare image files...")
    }
    return percentage
}

fun takeTransitionScreenshot(stateTransition: Transition, transitionId: Int): String{
    println("$stateTransition")
    val actual = stateTransition.origin
    val screen = File(actual!!.screenShot)
    var img: BufferedImage? = null
    if(TransitionType.getUserTypeTransition().contains(stateTransition.type)){
        val toHighlight =stateTransition.originElement
        val p1 = toHighlight?.point1
        val p2 = toHighlight?.point2
        val width = p2!!.first() - p1!!.first()
        val height = p2[1] - p1[1]
        val g2d = ImageIO.read(screen).let {
            img = it
            it.createGraphics()
        }
        val dash1 = FloatArray(1,{20.toFloat()})
        val dashed = BasicStroke(5f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,10.0f,
            dash1,0.0f)
        g2d.stroke = dashed
        val alpha = 127
        val myColour = Color(245,187,5,alpha)
        with(g2d){
            color = myColour
            fillOval(p1[0],p1[1],width,height)
            color = Color.RED
            drawOval(p1[0],p1[1],width,height)
            dispose()
        }
    }else if(TransitionType.getScrollTransitions().contains(stateTransition.type)){
        val toHighlight = stateTransition.originElement
        val p1 = toHighlight?.point1
        val p2 = toHighlight?.point2

        val tapX = p1!!.first()
        val tapX2 = p2!!.first()*3/2

        val tapY = p1.component2()
        val tapY2 = p2.component2()*3/2
        val g2d = ImageIO.read(screen).let {
            img = it
            it.createGraphics()
        }
        val dash1 = FloatArray(1,{20.toFloat()})
        val dashed =BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            dash1,0.0f)
        g2d.stroke = dashed
        val initial = Point(tapX2,tapY)
        val finalP = if(stateTransition.type == TransitionType.SCROLL) Point(tapX2,tapY) else Point(tapX,tapY2)
        val arrow = createArrowShape(initial, finalP)
        val alpha = 127
        with(g2d){
            color = Color(245,187,5,alpha)
            fill(arrow)
            color = Color.RED
            draw(arrow)
            dispose()
        }
    }else{
        val deviceDimensions = getScreenSize()
        val g2d = UIUtil.createImage(deviceDimensions[0],deviceDimensions[1],BufferedImage.TYPE_INT_RGB).let {
            it.createGraphics()
        }
        with(g2d){
            paint= Color.BLACK
            fillRect(0,0,deviceDimensions[0],deviceDimensions[1])
            paint = Color.WHITE
            font = Font("Serif", Font.BOLD,60)
            val s = stateTransition.type.toString()
            val fm = fontMetrics
            val x = (img?.width?:0 - fm.stringWidth(s))/2
            val y = (img?.height?:0 - fm.height)/2
            drawString(s,x,y)
            dispose()
        }
    }
    val route = screen.parent
    val outputFile = File("$route${File.separator}t$transitionId.png")
    ImageIO.write(img,"png",outputFile)
    return outputFile.path
}

fun createArrowShape(fromPt: Point, toPt: Point): Shape{
    val arrowPolygon = with(Polygon()){
        addPoint(-6,1)
        addPoint(3,1)
        addPoint(3,3)
        addPoint(6,0)
        addPoint(3,-3)
        addPoint(3,-1)
        addPoint(-6,-1)
        this
    }
    val midPoint = midpoint(fromPt,toPt)
    val rotate = Math.atan2((toPt.y-fromPt.y).toDouble(), (toPt.x-fromPt.x).toDouble())
    return with(AffineTransform()){
        translate(midPoint.x/1.0,midPoint.y/1.0)
        val scale = fromPt.distance(toPt)/12.0
        scale(scale,scale)
        rotate(rotate)
        this
    }.createTransformedShape(arrowPolygon)
}

fun midpoint(p1: Point, p2: Point): Point{
    return Point((p1.x+p2.x)/2,(p1.y+p2.y)/2)
}

fun getNodeImagesFromState(state: State): String{
    val screenshot = File(state.screenShot)
    val stateNodesFolder = File(screenshot.parent+File.separator+state.id+File.separator)
    stateNodesFolder.mkdir()
    state.stateNodes.forEachIndexed{ i, it ->
        val img = ImageIO.read(screenshot)
        val w = it.point2!!.first() - it.point1!!.first()
        val h = it.point2!!.component2() - it.point1!!.component2()
        val subimage = img.getSubimage((it.point1 as IntArray).first() , (it.point1 as IntArray).component2(), if(2==0)30 else w, if(h==0)30 else h)
        ImageIO.write(subimage, "png", File("${stateNodesFolder.path+File.separator+state.id}_$i.png"))
    }
    return stateNodesFolder.path
}