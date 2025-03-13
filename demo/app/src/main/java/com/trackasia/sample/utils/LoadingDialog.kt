package com.trackasia.sample.utils
import android.app.ProgressDialog
import android.content.Context

class LoadingDialog(context: Context) {
    private val progressDialog: ProgressDialog = ProgressDialog(context)

    init {
        progressDialog.setCancelable(false)
        progressDialog.setMessage("Loading...")
    }

    fun show() {
        progressDialog.show()
    }

    fun dismiss() {
        progressDialog.dismiss()
    }
}