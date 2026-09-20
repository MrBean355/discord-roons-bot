package com.github.mrbean355.roons.security

/**
 * Indicates that an endpoint method or controller class requires admin authorization.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AdminOnly
