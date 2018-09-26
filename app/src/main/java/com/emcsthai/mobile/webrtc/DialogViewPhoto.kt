package com.emcsthai.mobile.webrtc

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.emcsthai.mobile.webrtc.model.DrawingPoint


/**
 * Created by nakarin on 8/22/2017 AD.
 */

class DialogViewPhoto : DialogFragment() {

    private val TAG = DialogViewPhoto::class.java.canonicalName

    companion object {

        private const val URL_PATH = "URL_PATH"

        fun newInstance(urlPath: String, cancelable: Boolean): DialogViewPhoto {
            val fragment = DialogViewPhoto()
            val bundle = Bundle()
            bundle.putString(URL_PATH, urlPath)
            fragment.arguments = bundle
            fragment.isCancelable = cancelable
            return fragment
        }
    }

    private var mOnDrawingTouchListener: OnDrawingTouchListener? = null

    private lateinit var imgDrawingView: ImageDrawingView
    private lateinit var fabClose: FloatingActionButton

    private var urlPath: String = ""
    private var cancel: Boolean = true

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.run {
                dialog.window.setLayout(width, height)
                dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            restoreArguments(arguments!!)
        } else {
            restoreInstanceState(savedInstanceState)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)
        dialog.setCancelable(cancel)
        dialog.setCanceledOnTouchOutside(cancel)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_view_photo, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Method from this class
        bindView(view)
        // Method from this class
        setupView()

        imgDrawingView.setOnPaintTouchListener(onPaintTouchListener)
        fabClose.setOnClickListener(onClickListener)
    }

    private fun bindView(v: View) {
        imgDrawingView = v.findViewById(R.id.imgDrawingView)
        fabClose = v.findViewById(R.id.fabClose)
    }

    private fun setupView() {
        Glide.with(context!!)
                .load(urlPath)
                .apply(RequestOptions()
                        .override(1500))
                .into(imgDrawingView)
    }


    fun setDrawingPoint(drawingPoint: DrawingPoint) {
        imgDrawingView.drawFromServer(drawingPoint)
        activity?.runOnUiThread {
            imgDrawingView.invalidate()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(URL_PATH, urlPath)
    }

    private fun restoreInstanceState(bundle: Bundle) {
        urlPath = bundle.getString(URL_PATH)
    }

    private fun restoreArguments(bundle: Bundle) {
        urlPath = bundle.getString(URL_PATH)
    }

    fun setOnDrawingTouchListener(onDrawingTouchListener: OnDrawingTouchListener) {
        mOnDrawingTouchListener = onDrawingTouchListener
    }

    private val onPaintTouchListener = ImageDrawingView.OnPaintTouchListener { startX, startY, moveX, moveY ->
        mOnDrawingTouchListener?.onTouch(startX, startY, moveX, moveY)
    }

    private val onClickListener = View.OnClickListener {
        dialog.dismiss()
    }

    interface OnDrawingTouchListener {
        fun onTouch(startX: Float, startY:Float, moveX: Float, moveY: Float)
    }
}
