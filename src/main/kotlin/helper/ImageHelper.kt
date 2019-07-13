package helper

import main.kotlin.model.Transition
import main.kotlin.model.TransitionType
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
    val screen = File(actual.screenShot)
    if(TransitionType.getUserTypeTransition().contains(stateTransition.type)){

    }else
}