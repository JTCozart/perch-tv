package com.jtcozart.perch.tapo

/**
 * The camera turned the Tapo password down when hashed with [method]. Retrying cannot help, and repeated failures can
 * lock the camera out for a while.
 */
class TapoPasswordRefusedException(val method: TapoPasswordHash.Method) : TapoStreamException("camera refused the Tapo password")
