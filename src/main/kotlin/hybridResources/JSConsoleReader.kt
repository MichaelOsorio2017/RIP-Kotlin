package main.kotlin.hybridResources

import helper.EmulatorHelper.simpleTryCatch
import main.kotlin.main.RIPBase
import java.io.BufferedReader
import java.io.InputStreamReader

class JSConsoleReader(): Thread(){

    constructor(mainThread: RIPBase):this(){
        rip = mainThread
    }

    companion object{
        private var isRunning: Boolean = true
        private  var rip: RIPBase? = null
    }

    override fun run(){
        simpleTryCatch {
            readWebViewConsole()
        }
    }

    private fun readWebViewConsole(){
        simpleTryCatch {
            val proc = Runtime.getRuntime().exec("adb logcat chromium:D SystemWebViewClient:D *:S")
            val inputStreamReader = InputStreamReader(proc.inputStream)
            BufferedReader(inputStreamReader).use {
                it.forEachLine {
                    if(isRunning){
                        println(it)
                    }
                }
            }
            proc.waitFor()
        }
    }

    fun kill(){
        isRunning = false
    }
}