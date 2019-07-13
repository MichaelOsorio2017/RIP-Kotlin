package main.kotlin.model
enum class AndroidNodeProperty(name: String, type:String) {
    NAF("NAF","boolean"),
    BOUNDS("bounds","pointsTuple"),
    PACKAGE("bounds","text"),
    TEXT("text", "text"),
    CHECKABLE("checkable", "boolean"),
    CHECKED("checked", "boolean"),
    SELECTED("selected", "boolean"),
    CLICKABLE("clickable", "boolean"),
    CONTENT_DESCRIPTION("content-desc", "text"),
    ENABLED("enabled", "boolean"),
    FOCUSABLE("focusable", "boolean"),
    FOCUSED("focused", "boolean"),
    INDEX("index", "int"),
    PASSWORD("password", "boolean"),
    RESOURCE_ID("resource-id", "text"),
    SCROLLABLE("scrollable", "boolean"),
    //Class of the element: widget or view (eg, android.view.ViewGroup, android.widget.ImageButton)
    CLASS("class", "text"),
    LONG_CLICKABLE("long-clickable", "boolean");
    companion object{
        fun fromName(name:String):AndroidNodeProperty?{
            return values().find { it.name == name }
        }
    }

}