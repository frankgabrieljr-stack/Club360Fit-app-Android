package com.club360fit.app.ui.utils

/**
 * Consistent copy for save/submit outcomes (snackbars, etc.).
 */
object SubmitResultMessages {
    const val SUBMITTED_SUCCESS = "Submitted successfully"
    const val SAVED_SUCCESS = "Saved successfully"
    const val LOGGED_SUCCESS = "Logged successfully"
    const val APPROVED_SUCCESS = "Approved successfully"
    const val DECLINED_SUCCESS = "Declined successfully"
    const val UPLOAD_SUCCESS = "Upload successful"
    const val DELETE_SUCCESS = "Deleted successfully"
    /** Schedule session marked complete (admin calendar). */
    const val MARKED_COMPLETE_SUCCESS = "Marked complete"
    const val IMAGE_READ_FAILED = "Unable to read image"

    fun failure(throwable: Throwable): String =
        throwable.message?.takeIf { it.isNotBlank() } ?: "Something went wrong"

    fun failure(message: String?): String =
        message?.takeIf { it.isNotBlank() } ?: "Something went wrong"
}
