package io.github.landerlyoung.sonydafaisgood

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.Choreographer
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.inputMethodManager
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileOutputStream


class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { takeSnapShotAndShare() }
    }

    private fun takeSnapShotAndShare() {
        inputMethodManager.hideSoftInputFromInputMethod(smile_of_kaz.windowToken, 0)

        main_title.isCursorVisible = false
        sub_title.isCursorVisible = false
        // fore redraw
        smile_of_kaz.invalidate()

        Choreographer.getInstance().postFrameCallback {
            val snapShot = takeSnapShot(smile_of_kaz)

            snapShot?.let { bitmap ->
                doAsync {
                    val save = File(externalCacheDir, "kaz_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(save).use {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                    }

                    deleteFileWithout(externalCacheDir, save)

                    runOnUiThread {
                        main_title.isCursorVisible = true
                        sub_title.isCursorVisible = true

                        shareImage(save)
                    }
                }
            } ?: toast(R.string.failed_to_save_pic)
        }
    }

    private fun deleteFileWithout(externalCacheDir: File?, save: File) {
        externalCacheDir?.listFiles()?.forEach {
            if (it != save) {
                it.delete()
            }
        }
    }

    private fun shareImage(save: File) {
        val intent = Intent(Intent.ACTION_SEND)
        val uri = Uri.parse("file://${save.absoluteFile}")
        intent.type = "image/jpeg"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(intent, getString(R.string.smile_by_app)))
    }

    private fun takeSnapShot(view: View): Bitmap? {
        try {
            val oldLayerType = view.layerType
            view.buildDrawingCache(true)
            var bitmap = view.getDrawingCache(true)

            if (bitmap == null && view.width > 0 && view.height > 0) {
                bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.save()

                // do draw
                canvas.translate(-view.scrollY.toFloat(), -view.scrollY.toFloat())
                view.draw(canvas)
                canvas.restore()
            }

            view.setLayerType(oldLayerType, null)
            return bitmap
        } catch (oom: OutOfMemoryError) {
            return null
        }
    }
}
