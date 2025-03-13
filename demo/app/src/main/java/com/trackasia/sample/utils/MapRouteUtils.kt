package com.trackasia.sample.utils

import com.trackasia.sample.R

class MapRouteUtils() {
    fun getRouterDirection(router: String?): Int {
        return when (router){
            "right" -> R.drawable.ic_arrow_forward
            "left" -> R.drawable.ic_arrow_back
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
            else -> R.drawable.ic_arrow_forward
        }
    }

    fun getNameDirection(router: String?): String {
        return when (router){
            "right" -> "quẹo trái vào "
            "left" -> "quẹo phải vào "
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
            else -> "quẹo phải vào "
        }
    }

}