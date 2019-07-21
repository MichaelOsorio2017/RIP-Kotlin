package main.kotlin.main

import helper.EmulatorHelper.*
import helper.Helper
import helper.getNodeImagesFromState
import helper.takeTransitionScreenshot
import main.kotlin.model.AndroidNode
import main.kotlin.model.State
import main.kotlin.model.Transition
import main.kotlin.model.TransitionType
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.collections.ArrayList
import main.kotlin.model.TransitionType.*

class RIPRR(configFilePath: String): RIPBase(configFilePath){
    var scriptPath = ""
    var oldStatesTable: Hashtable<String, State>? = null
    var oldStates: ArrayList<State>? = null
    var oldTransitions: ArrayList<Transition>? = null
    var transToBeExecAN: AndroidNode? = null

    override fun preProcess(preProcArgs: JSONObject) {
        scriptPath = preProcArgs.get("scriptPath").toString()
        oldStatesTable = Hashtable()
        this.oldStates = arrayListOf()
        oldTransitions = ArrayList()

        val jsonParser = JSONParser()
        FileReader(scriptPath).use {
            val obj = jsonParser.parse(it) as JSONObject
            val amountStates = Math.toIntExact(obj.get(AMOUNT_STATES) as Long)
            val amountTransitions = Math.toIntExact(obj.get(AMOUNT_TRANSITIONS) as Long)
            println("AMOUNT STATES: $amountStates")
            println("AMOUNT TRANSITIONS: $amountTransitions")
            val states = obj.get(STATES) as JSONObject

            for(i in 0 .. amountStates){
                val tempState = State(hybrid = hybridApp, contextualChanges = contextualExploration)
                val currentState = states.get("${i+1}") as JSONObject
                with(tempState){
                    rawXML = currentState.get("rawXML") as String
                    val raw = rawXML
                    activityName = currentState.get("activityName") as String
                    id = Math.toIntExact(currentState.get("id") as Long)
                    parsedXML = loadXMLFromString(raw!!)
                    oldStates?.add(this)
                    oldStatesTable?.put(rawXML,this)
                }
            }
            val transitions = obj.get((TRANSITIONS)) as JSONObject

            for(i in 1..amountTransitions){
                val currentTransition = transitions.get("$i") as JSONObject
                val originState = Math.toIntExact(currentTransition.get("stState") as Long)
                val tType = TransitionType.valueOf(currentTransition.get("tranType") as String)
                val destState = Math.toIntExact(currentTransition.get("dsState") as Long)
                val tempTransition = Transition(oldStates!!.get(originState-1) , type = tType)
                tempTransition.destination = oldStates!!.get(destState-1)
                if(currentTransition.containsKey("androidNode")){
                    val androidNode = currentTransition.get("androidNode") as JSONObject
                    with(androidNode){
                        val resourceID = get("resourceID") as String
                        val xpath = get("xpath") as String
                        val text = get("text") as String
                        tempTransition.originElement = oldStates!!.get(originState-1).getAndroidNode(resourceID,xpath,text)
                    }
                }
                oldTransitions?.add(tempTransition)
            }
            oldTransitions?.forEach {
                println("${it.origin?.id} - ${it.destination?.id} - ${it.type.name}")
            }
        }
    }

    override fun explore(previousState : State, executedTransition: Transition) {
        println("NEW STATE EXPLORATION STARTED")
        currentState = State(hybrid = hybridApp, contextualChanges = contextualExploration)
        simpleTryCatch {
            ifKeyboardHideKeyboard()
            isEventIdle()
            currentState?.run {
                id = sequentialNumber
                val rawXML = getCurrentViewHierarchy()
                val parsedXML = loadXMLFromString(rawXML)
                val screenShot = takeAndPullScreenshot("$id", folderName)
                this.rawXML = rawXML
                this.parsedXML = parsedXML
                //Conditions for find a new state
                rippingOutsideApp = isRippingOutSideApp(parsedXML)
                val foundState = findStateGraph(this)
                val sameState = compareScreenShotWithExisting(screenShot)
                if (foundState != null || sameState != null || rippingOutsideApp) {
                    println(
                        "State Already Exists ${
                        if (foundState != null) {
                            currentState = foundState
                            Helper.deleteFile(screenShot)
                            "Found state in graph"
                        } else if (sameState != null) {
                            println("SAME STATE FOUND BY IMAGE COMPARISON")
                            Helper.deleteFile(sameState.screenShot!!)
                            val newScreen = File(screenShot)
                            newScreen.renameTo(File(sameState.screenShot))
                            currentState = sameState
                            "Found state by images"
                        } else {
                            Helper.deleteFile(screenShot)
                            currentState = previousState
                            "Ripping outside the app"
                        }
                        }"
                    )
                } else {
                    //New State found
                    val activity = getCurrentFocus()
                    takeAndPullXMLSnapshot("${id}", folderName)
                    println("Current ST: $id")
                    activityName = activity
                    statesTable?.put(rawXML, this)
                    states?.add(this)
                    this.screenShot = screenShot
                    this.retrieveContext(packageName)
                    getNodeImagesFromState(this)
                }

            }
            if(!rippingOutsideApp){
                if(currentState?.hasRemainingTransition()!!){
                    previousState.addPossibleTransition(executedTransition)
                }
                executedTransition.destination = currentState
                executedTransition.origin = previousState
                currentState?.addInboundTransition(executedTransition)
                previousState.addOutboundTransition(executedTransition)
                transitions?.add(executedTransition)
            }
            if(oldTransitions?.size == 0){
                println("OLD TRANSITIONS EMPTY")
                return
            }
            val transToBeExec = oldTransitions?.first()
            transToBeExecAN = transToBeExec?.originElement
            oldTransitions?.forEachIndexed{ i, it ->
                println("${i+1}: ${it.origin?.id} - ${it.destination?.id} - ${it.type.name}")
            }
            if(transToBeExec!!.origin!!.id != currentState!!.id){
                println("SALIENDO DE EJECUCIÓN. ESTADO DE INICIO != AL ACTUAL")
                println("${transToBeExec.origin!!.id} - ${currentState!!.id}")
                return
            }else{
                var tempTrans = currentState?.popTransition()
                var tempTransAN = tempTrans?.originElement
                if(tempTrans?.type != BUTTON_BACK && transToBeExec.type != BUTTON_BACK){
                    while(!(tempTrans?.type != transToBeExec.type)
                        || tempTransAN?.resourceID != transToBeExecAN?.resourceID
                        || tempTransAN?.xPath != transToBeExecAN?.xPath){
                        executeTransition(tempTrans!!)
                        executedIterations++
                        ifKeyboardHideKeyboard()
                        isEventIdle()
                        tempTrans = currentState?.popTransition()
                        tempTransAN = tempTrans?.originElement
                    }
                }else{
                    while(tempTrans?.type != transToBeExec.type){
                        executeTransition(tempTrans!!)
                        executedIterations++
                        ifKeyboardHideKeyboard()
                        isEventIdle()
                        tempTrans = currentState?.popTransition()
                        tempTransAN = tempTrans?.originElement
                    }
                }
                executeTransition(tempTrans!!)
                oldTransitions?.removeAt(0)
                executedIterations++
                ifKeyboardHideKeyboard()
                isEventIdle()
                tempTrans.screenShot = takeTransitionScreenshot(tempTrans!!, transitions!!.size)
                explore(currentState!!,tempTrans)
            }
        }
    }
}