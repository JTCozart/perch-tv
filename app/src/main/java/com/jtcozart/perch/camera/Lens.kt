package com.jtcozart.perch.camera

import kotlinx.serialization.Serializable

/** Dual-lens cameras publish their second (telephoto) lens on separate stream paths. */
@Serializable
enum class Lens { Wide, Telephoto }
