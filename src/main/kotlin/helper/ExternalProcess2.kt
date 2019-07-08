package helper.ExternalProcess2

import main.kotlin.RipException
import org.apache.commons.io.IOUtils

/**
 * Constant to indicate the encoding type
 */
const val ENCODING_TYPE = "UTF-8"

/**
 * function that helps to execute a process in an emulator
 */
fun executeProcess2(commands: List<String>, commandName: String,onSuccesMessage: String?
                    ,onErrorMessage: String?):List<String>{
    val answer = mutableListOf<String>()
    val pb =ProcessBuilder(commands)
    println("-> $commandName")
    println(commands.toString())
    val spb =pb.start()
    val output = IOUtils.toString(spb.inputStream, ENCODING_TYPE)
    answer.add(output)
    if(!output.startsWith("<?xml")){
        println(output)
    }

    val error = IOUtils.toString(spb.errorStream, ENCODING_TYPE)
    answer.add(error)
    println(error)
    answer.add(commandName)
    println("- - - - - - - - - - - - - - - - - - - - - - - - ")
    //Helper.getInstance("./").logMessage(commandName, Arra)
    if(!error.equals("")){
        throw RipException(error)
    }
    if(!output.startsWith("<?xml>")){
        if (output.startsWith("adb: error") || output.contains("error") || output.contains("Failure")
            || output.contains("Error")) {
            throw RipException(error)
        }
    }
    return answer
}
