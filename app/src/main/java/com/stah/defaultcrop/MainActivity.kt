package com.stah.defaultcrop

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import permissions.dispatcher.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.ComponentName

import android.content.pm.ResolveInfo
import android.os.Parcelable

@RuntimePermissions
class MainActivity : AppCompatActivity() {
    var file: File? = null
    var uri: Uri? = null
    lateinit var currentPhotoPath: String

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun onShowCameraButtonClick() {

        openCameraWithPermissionCheck()
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForCamera(request: PermissionRequest) {
        showRationaleDialog(request)
    }

    private fun showRationaleDialog(request: PermissionRequest) {
        AlertDialog.Builder(this)
            .setPositiveButton("許可") { _, _ -> request.proceed() }
            .setNegativeButton("拒否") { _, _ -> request.cancel() }
            .setCancelable(false)
            .setMessage("カメラの権限が必要です")
            .show()
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraDenied() {
        Toast.makeText(this, "permission_camera_denied", Toast.LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    fun onCameraNeverAskAgain() {
        Toast.makeText(this, "もう質問しない", Toast.LENGTH_SHORT).show()
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                // Save a file: path for use with ACTION_VIEW intents
                currentPhotoPath = absolutePath
            }
            if (Build.VERSION.SDK_INT > 23) {
                uri =
                    FileProvider.getUriForFile(
                        this,
                        applicationContext.packageName + ".fileprovider",
                        file!!
                    )

            } else {
                uri = Uri.fromFile(file)
            }
            grantUriPermission(
                "com.android.camera", uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            intent.putExtra("return-data", true)
            startActivityForResult(intent, Companion.REQUEST_CODE_TAKE_IMAGE)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(
            Intent.createChooser(intent, "Select image from your gallery"),
            Companion.REQUEST_CODE_CHOOSE_IMAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            onShowCameraButtonClick()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            Log.d("crop", "result ok req:$requestCode")
            when (requestCode) {
                Companion.REQUEST_CODE_TAKE_IMAGE -> {
                    cropImage()
                }
                Companion.REQUEST_CODE_CHOOSE_IMAGE -> {
                    if (data != null) {
                        uri = data.data
                        cropImage()
                    }
                }
                Companion.REQUEST_CODE_CROP_AVATAR -> {
                    if (data != null) {
                        val bundle = data.extras
                        val bitmap = bundle!!.getParcelable<Bitmap>("data")
                        val ivAvatar = findViewById<ImageView>(R.id.imageView)
                        ivAvatar.setImageBitmap(bitmap)
                    }
                }
            }
        } else {
            Log.d("crop", "result ng req:$requestCode")
        }
    }

    private fun cropImage() {
        val intent = Intent("com.android.camera.action.CROP")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        intent.type = "image/*"

        val list: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        val size = list.size
        if (size == 0) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivityForResult(intent, Companion.REQUEST_CODE_CROP_AVATAR)
            return
        } else {
            intent.setDataAndType(uri, "image/*")
            intent.putExtra("outputX", 180)
            intent.putExtra("outputY", 180)
            intent.putExtra("aspectX", 3)
            intent.putExtra("aspectY", 3)
            intent.putExtra("scale", true)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            if (size == 1) {
                val i = Intent(intent)
                val res = list[0]
                i.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
                startActivityForResult(intent, Companion.REQUEST_CODE_CROP_AVATAR)
            } else {
                val i = Intent(intent)
                i.putExtra(Intent.EXTRA_INITIAL_INTENTS, list.toTypedArray<Parcelable>())
                startActivityForResult(intent, Companion.REQUEST_CODE_CROP_AVATAR)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_CROP_AVATAR = 5
        private const val REQUEST_CODE_CHOOSE_IMAGE = 4
        private const val REQUEST_CODE_TAKE_IMAGE = 3
    }
}








