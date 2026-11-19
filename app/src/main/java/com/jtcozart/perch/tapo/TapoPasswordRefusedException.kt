package com.jtcozart.perch.tapo

/** The camera turned the Tapo password down. Retrying cannot help, and repeated failures can lock the camera out for a while. */
class TapoPasswordRefusedException : TapoStreamException("camera refused the Tapo password")
