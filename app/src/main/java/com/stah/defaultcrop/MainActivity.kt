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

@RuntimePermissions
class MainActivity : AppCompatActivity() {
    private val GAL_CODE = 1
    private val CROP_CODE = 2
    private val CAM_CODE = 3

    //Define constants:
    private val PERMISSION_CAMERA_CODE = 1
    private val PERMISSION_STORE_CODE = 2
    private val REQUEST_CODE_TAKE_IMAGE = 3
    private val REQUEST_CODE_CHOOSE_IMAGE = 4
    private val REQUEST_CODE_CROP_AVATAR = 5
    private val REQUEST_CODE_CROP_COVER = 6

    private val CROP_FROM_CAMERA = 22

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
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            intent.putExtra("return-data", true)
            startActivityForResult(intent, REQUEST_CODE_TAKE_IMAGE)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(
            Intent.createChooser(intent, "Select image from your gallery"),
            REQUEST_CODE_CHOOSE_IMAGE
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
                REQUEST_CODE_TAKE_IMAGE -> {
                    cropImage()
                }
                REQUEST_CODE_CHOOSE_IMAGE -> {
                    if (data != null) {
                        uri = data.data
                        cropImage()
                    }
                }
                REQUEST_CODE_CROP_AVATAR -> {
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
        val cropIntent = Intent("com.android.camera.action.CROP")
        cropIntent.setDataAndType(uri, "image/*")
        cropIntent.putExtra("crop", true)


        cropIntent.putExtra("outputX", 180)
        cropIntent.putExtra("outputY", 180)
        cropIntent.putExtra("aspectX", 3)
        cropIntent.putExtra("aspectY", 3)
        cropIntent.putExtra("scale", true);
        cropIntent.putExtra("scaleUpIfNeeded", true)
        cropIntent.putExtra("return-data", true)

        cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(cropIntent, REQUEST_CODE_CROP_AVATAR)
    }
}








