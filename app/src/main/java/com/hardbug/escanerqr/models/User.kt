package com.hardbug.escanerqr.models

import java.util.Date

class User {
    private var userId: Int = 0
    private var firstName: String = ""
    private var secondName: String? = ""
    private var paternalSurname: String = ""
    private var maternalSurname: String? = ""
    private var birthDate: Date = Date()
    private var password: String = ""

    fun getFullName(): String {
        return buildString {
            append(firstName)
            secondName?.let { append(" $it") }
            append(" $paternalSurname")
            maternalSurname?.let { append(" $it") }
        }
    }
}