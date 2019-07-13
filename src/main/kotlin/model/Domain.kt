package main.kotlin.model

data class Domain (var name: String, var type: Type, var field: String){
    enum class Type{
        BOOLEAN,
        STRING,
        LIST,
        NUMBER,
        BUTTON,
        OTHER,
        PASSWORD,
        DATE
    }
}