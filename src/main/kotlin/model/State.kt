package main.kotlin.model

import helper.EmulatorHelper.*
import main.kotlin.model.TransitionType.*
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.w3c.dom.Document
import java.util.*
import kotlin.collections.ArrayList


data class State(var id: Int? = null, var activityName: String? = null,
                 var rawXML: String? = null, var hybrid: Boolean, var contextualChanges: Boolean,
                 var battery: Int? = null, val stateNodes: MutableList<AndroidNode> = ArrayList(),
                 val possibleTransitions: Deque<Transition> = ArrayDeque(),
                 val outboundTransition: List<Transition> = ArrayList(),
                 val inboundTransitions: List<Transition> = ArrayList(),
                 var hybridErrors: MutableList<HybridError> = ArrayList(),
                 var screenShot: String? = null, var domainModel: MutableList<Domain> = ArrayList(),
                 var wifiStatus: Boolean? = null, var cpu: Double? = null, var memory:Double? = null,
                 var airplane: Boolean? = null) {
    var parsedXML: Document? = null
        set(value){
            field = value
            generatePossibleTransitions()
        }

    fun popTransition(): Transition = possibleTransitions.pop()


    fun addInboundTransition(pTransition: Transition) = inboundTransitions + pTransition


    fun addOutboundTransition(pTransition: Transition)= outboundTransition + pTransition


    private fun generatePossibleTransitions(){
        possibleTransitions.push(Transition(origin = this, type = BUTTON_BACK))
        if(contextualChanges){
            listOf(1,2).forEach{
                possibleTransitions.push(Transition(this, type = CONTEXT_INTERNET_OFF))
            }
            possibleTransitions.push(Transition(this, type = CONTEXT_LOCATION_OFF))
            possibleTransitions.push(Transition(this, type = CONTEXT_LOCATION_ON))
            possibleTransitions.push(Transition(this, type = ROTATE_LANDSCAPE))
            possibleTransitions.push(Transition(this, type = ROTATE_PORTRAIT))
        }

        if(hybrid){
            //Interactions in hybrid applications
        }

        val allNodes = parsedXML?.getElementsByTagName("node")

        for (i in 0 .. allNodes!!.length){
            var currentNode = allNodes.item(i)
            var newAndroidNode = AndroidNode(this, currentNode)
            stateNodes.add(newAndroidNode)
            if(newAndroidNode.isDomainAttribute()){
                loadDomainModel(newAndroidNode)
            }
            with(newAndroidNode){
                if(isAButton() || isClickeable()|| (hybrid and isEnabled())){
                    possibleTransitions.push(if (isEditableText()){
                        Transition(this@State,type = GUI_INPUT_TEXT,originElement = this)
                    }else{
                        Transition(this@State,type = SCROLL, originElement = this)
                    })
                }
            }
            possibleTransitions.push(if (newAndroidNode.isScrollable()){
                Transition(this, type =SWIPE, originElement = newAndroidNode )
            }else{
                Transition(this, type = SCROLL, originElement = newAndroidNode)
            })
        }
    }
    private fun loadDomainModel(androidNode: AndroidNode){
        var field = androidNode.resourceID
        val name = field
        val type = androidNode.type
        if(field == "")field ="BLANK"
        domainModel.add(Domain(name,type!!,field))
    }
    fun addError(error: HybridError){
        hybridErrors.add(error)
    }

    fun hasRemainingTransition(): Boolean = possibleTransitions.isNotEmpty()

    fun getAndroidNode(resourceID: String, xPath: String, text: String): AndroidNode?{
        return stateNodes.find {
            it.xPath ==xPath && it.resourceID == resourceID && it.text == text
        }
    }

    fun getDomainModel(): JSONArray {
        val model = JSONArray()
        domainModel.forEach { attribute ->
            val fields = attribute.field.split("/")
            var field = attribute.field
            if(fields.size>1)field = fields[1]
            with(JSONObject()){
                put("name",attribute.name)
                put("field",field)
                put("type",attribute.type)
                model + this
            }

        }
        return model
    }

    fun retrieveContext(packageName: String){
        battery = showBatteryLevel()
        wifiStatus = isWifiEnabled()
        cpu = showCPUUsage(packageName)
        memory = showMemoryUsage(packageName)
        airplane = isAirplaneModeOn()
    }
}