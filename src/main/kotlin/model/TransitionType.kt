package main.kotlin.model

enum class TransitionType {
    GUI_CLICK_BUTTON,
    SWIPE,
    SCROLL,
    GUI_RANDOM,
    GUI_INPUT_TEXT,
    GUI_INPUT_NUMBER,
    CONTEXT_INTERNET_ON,
    CONTEXT_INTERNET_OFF,
    CONTEXT_LOCATION_ON,
    CONTEXT_LOCATION_OFF,
    ROTATE_LANDSCAPE,
    ROTATE_PORTRAIT,
    BUTTON_BACK,
    FIRST_INTERACTION,
    FINISH_EXECUTION;

    companion object {
        fun getUserTypeTransition(): Set<TransitionType> {
            val userTypes = HashSet<TransitionType>()
            userTypes + GUI_CLICK_BUTTON
            return userTypes
        }

        fun getScrollTransitions(): Set<TransitionType> {
            val userTypes = HashSet<TransitionType>()
            userTypes + SCROLL
            userTypes + SWIPE
            return userTypes
        }
    }
}