package com.google.mediapipe.examples.llminference

import android.os.Debug
import kotlin.math.roundToInt

class MemoryMonitor {
    companion object {
        private const val MB = 1024 * 1024.0

        fun getMemoryInfo(): MemoryInfo {
            val runtime = Runtime.getRuntime()
            val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / MB
            val maxHeapSizeInMB = runtime.maxMemory() / MB
            val nativeHeapInMB = Debug.getNativeHeapAllocatedSize() / MB

            return MemoryInfo(
                usedMemInMB.roundToInt(),
                maxHeapSizeInMB.roundToInt(),
                nativeHeapInMB.roundToInt()
            )
        }
    }

    data class MemoryInfo(
        val usedMemoryMB: Int,
        val maxHeapSizeMB: Int,
        val nativeHeapMB: Int
    ) {
        override fun toString(): String {
            return "Used: ${usedMemoryMB}MB / Max: ${maxHeapSizeMB}MB\nNative: ${nativeHeapMB}MB"
        }
    }
}