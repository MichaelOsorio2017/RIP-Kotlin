package main.kotlin.model

import java.time.LocalDateTime

data class HybridError(private val currentTime: LocalDateTime = LocalDateTime.now(),
                       private val description: String, private val type: String) {

}