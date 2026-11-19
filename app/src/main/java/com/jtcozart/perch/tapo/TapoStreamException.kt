package com.jtcozart.perch.tapo

import java.io.IOException

/** A Tapo camera could not be reached or refused the stream. An IOException, so the player treats it as a network error. */
open class TapoStreamException(message: String, cause: Throwable? = null) : IOException(message, cause)
