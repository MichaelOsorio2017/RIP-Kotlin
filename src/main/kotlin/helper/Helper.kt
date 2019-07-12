package helper

import java.io.BufferedWriter
import helper.EmulatorHelper.simpleTryCatch
import main.kotlin.helper.SingletonHolder
import java.io.File
import java.io.FileWriter

class Helper(){
    companion object: SingletonHolder<Helper,String>({ folderName ->
        simpleTryCatch { Helper.out = BufferedWriter(FileWriter("$folderName${File.separator}./log.log"))}
        Helper()
    }){
        var out: BufferedWriter? = null
        fun logMessage(command: String, parameters: String, errorMessage: String){
            out?.use {
                simpleTryCatch {
                    if (errorMessage != null && errorMessage == ""){
                        it.write("COMMAND: $command ; PARAMETERS ${parameters.toString()} \n")
                    }else{
                        it.write("ERROR $command")
                        it.write("COMMAND: $command ; PARAMETERS ${parameters.toString()} \n")
                        println("COMMAND: $command ; PARAMETERS ${parameters.toString()} \n")
                    }
                    it.flush()
                }
            }
        }

        fun deleteFile(file: String){
            File(file).delete()
        }

        fun closeStream(){
            simpleTryCatch { out?.close() }
        }
    }
}
