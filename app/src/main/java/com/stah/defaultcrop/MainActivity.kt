package com.stah.defaultcrop

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import permissions.dispatcher.*
import java.io.File

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

    var file: File? = null
    var uri: Uri? = null

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 自動生成された権限ハンドリング用のコードに処理を委譲するための extension function を呼び出す。
        onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun onShowCameraButtonClick() {

        openCameraWithPermissionCheck()
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForCamera(request: PermissionRequest) {
        showRationaleDialog(request)
        //Toast.makeText(this, "permission_camera_rationale", Toast.LENGTH_SHORT).show()
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
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
        /*テスト
        file = File(
            Environment.getExternalStorageDirectory(),
            "file" + System.currentTimeMillis().toString() + ".jpg"
        )

         */

        file = File(
            Environment.getExternalStorageDirectory(),
            "tmp_" + System.currentTimeMillis().toString() + ".jpg"
        )

        if (Build.VERSION.SDK_INT > 23) {
            uri =
                FileProvider.getUriForFile(this, "com.example.homefolder.example.provider", file!!);
        } else {
            uri = Uri.fromFile(file);
        }
        //uri = Uri.fromFile(file)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        intent.putExtra("return-data", true)
        startActivityForResult(intent, REQUEST_CODE_TAKE_IMAGE)
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
        //openCamera()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
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
        cropIntent.putExtra("scaleUpIfNeeded", true)
        cropIntent.putExtra("return-data", true)
        startActivityForResult(cropIntent, REQUEST_CODE_CROP_AVATAR)
    }

    private fun photoDialog() {

        val imgOptions = arrayOf<CharSequence>("Take Photo", "Select from Gallery")

        val builder = AlertDialog.Builder(this);
        builder.setTitle("Choose Image Options");
        //DialogInterface dialogInterface, int i)
        builder.setItems(imgOptions) { p0, i ->
            if (imgOptions[i] == "Take Photo") {
                try {
                    val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    //val file =  File(Environment.getExternalStorageDirectory(), "tmp_" + String.toLong(System.currentTimeMillis()));
                    //Log.d("MainActivity", "File Saved in:\t" + file.getAbsolutePath().toString());

                    file = File(
                        Environment.getExternalStorageDirectory(),
                        "tmp_" + System.currentTimeMillis().toString() + ".jpg"
                    )

                    var uri: Uri? = null
                    if (Build.VERSION.SDK_INT > 23) {
                        uri = FileProvider.getUriForFile(
                            this,
                            "com.example.homefolder.example.provider",
                            file!!
                        );
                    } else {
                        uri = Uri.fromFile(file);
                    }

                    camIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    camIntent.putExtra("return-data", "true");
                    startActivityForResult(camIntent, CAM_CODE);

                } catch (ex: ActivityNotFoundException) {
                    ex.printStackTrace();
                }
            } else if (imgOptions[i].equals("Select from Gallery")) {
                val galIntent =
                    Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(galIntent, "Open With"), GAL_CODE);
            }
        }
    }
}








