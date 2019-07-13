package main.kotlin.model
import com.intellij.internal.statistic.eventLog.initStateEventTrackers
import com.sun.jna.StringArray
import main.kotlin.model.AndroidNodeProperty.*

import org.w3c.dom.Node

data class AndroidNode(val state: State,var interacted: Boolean? = false, var clickeable: Boolean = false,
                       var centralPoint: IntArray? = null, var point1: IntArray? = null, var point2: IntArray? = null,
                       var pClass:String? = null, var enabled: Boolean = false, var resourceID:String = "",
                       var text:String= "", var name: String= "",var xPath:String = "",var index:String ="",
                       var scrollable: Boolean= false, var type: Domain.Type? = null) {

    companion object{
        const val TRUE = "true"
        const val FALSE = "false"
    }

    constructor(state: State,domNode: Node):this(state){
        loadAttributesFromDom(domNode)
        val classes = pClass?.split("\\.")
        xPath = "${domNode.attributes.getNamedItem("index")}_${if(pClass != "")classes!!.last()else ""}" +
                "${if(resourceID != "") "/$resourceID" else ""}"
        var temp = domNode.parentNode
        while (temp.nodeName != "hierarchy"){
            val teemp = temp.attributes
            val classess = teemp.getNamedItem("class").nodeValue.split("\\.")
            val indexx = teemp.getNamedItem("index").nodeValue
            xPath = "${indexx}_${classess.last()}|$xPath"
            temp = temp.parentNode
        }
    }

    fun loadAttributesFromDom(domNode: Node){
        val attributes = domNode.attributes
        name = domNode.nodeName
        for(i in 0..attributes.length){
            val attribute = attributes.item(i)
            val attributeValue = attribute.nodeValue
            val androidNodeProperty = AndroidNodeProperty.fromName(attribute.nodeName)

            androidNodeProperty?.let {
                when(it){
                    CLICKABLE -> clickeable = (attributeValue == TRUE)
                    BOUNDS -> loadBounds(attributeValue)
                    CLASS -> pClass = attributeValue
                    ENABLED -> enabled =true
                    RESOURCE_ID -> resourceID = attributeValue
                    TEXT -> text = attributeValue
                    INDEX -> index = attributeValue
                    SCROLLABLE -> scrollable = attributeValue.toBoolean()
                }
            }?: println("IMPORTAN: Property ${attribute.nodeName} is not included in RIP")

        }
    }

    private fun loadBounds(text: String){
        val bounds = text.replace("][","/").replace("[","")
                    .replace("]","") +"/0"
        val coords = bounds.split("/")
        val coord1 = coords[0]
        val coord2 = coords[1]
        val points1 = coord1.split(",")
        val points2 = coord2.split(",")
        val x1 = points1[0].toInt()
        val x2 = points2[0].toInt()
        val y1 = points1[1].toInt()
        val y2 = points2[1].toInt()
        point1 = intArrayOf(x1,y1)
        point2 = intArrayOf(x2,y2)
        centralPoint = intArrayOf((x1+x2)/2,(y1+y2)/2)
    }
    fun isAButton():Boolean{
        return if (pClass == "android.widged.Button"){
             true
        }else{pClass!!.toLowerCase().contains("button")}
    }

    fun isDomainAttribute():Boolean{
       return when(pClass){
            "android.widget.CheckBox" -> {
                type = Domain.Type.BOOLEAN
                true
            }
            "android.widget.EditText" -> {
                type = Domain.Type.STRING
                true
            }
            "android.widget.Button" -> {
                type = Domain.Type.BUTTON
                true
            }
            "android.widget.RadioGroup" ->{
                type = Domain.Type.LIST
                true
            }
            else -> false
        }
    }

    fun isEditableText():Boolean{
        return if(pClass == "android.widget.EditText"){
            true
        }else{pClass!!.toLowerCase().contains("EditText")}
    }

    fun getCentral1X():Int = centralPoint!![0]
    fun getCentral1Y(): Int = centralPoint!![1]
}