package com.pstor

interface Tagged {
    val tag: String
        get() = this.javaClass.simpleName
}