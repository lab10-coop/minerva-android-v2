package minerva.android.widget

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.profile_image_menu_layout.*
import minerva.android.R

class ProfileImageDialog(private val fragment: Fragment) : Dialog(fragment.requireContext(), R.style.CardDialog) {

    var imageUri: Uri? = null


    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>) {
        if (isPermissionGranted(permissions)) preparePhoto(requestCode)
        else onPermissionDenied()
    }

    private fun preparePhoto(request: Int) {
        when (request) {
            TAKE_PHOTO_REQUEST -> choosePhoto()
            PICK_IMAGE_REQUEST -> takePhoto()
        }
    }

    private fun takePhoto() {
        ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, IMAGE_TITLE)
            imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, this)
        }
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            fragment.startActivityForResult(this, PICK_IMAGE_REQUEST)
        }
    }

    private fun choosePhoto() {
        Intent(Intent.ACTION_PICK).apply {
            type = PICK_TYPE
            fragment.startActivityForResult(this, TAKE_PHOTO_REQUEST)
        }
    }

    private fun initializeMenu() {
        makePhoto.setOnClickListener {
            checkRequestPermissions(PICK_IMAGE_REQUEST)
            dismiss()
        }
        choosePhoto.setOnClickListener {
            checkRequestPermissions(TAKE_PHOTO_REQUEST)
            dismiss()
        }
    }

    private fun checkRequestPermissions(request: Int) {
        when (request) {
            PICK_IMAGE_REQUEST -> checkPermissions(request, TAKE_PHOTO_PERMISSIONS)
            else -> checkPermissions(request, PICK_IMAGE_PERMISSION)
        }
    }

    private fun checkPermissions(request: Int, requiredPermissions: Array<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (isPermissionGranted(requiredPermissions)) preparePhoto(request)
            else fragment.requestPermissions(requiredPermissions, request)
        else preparePhoto(request)
    }

    private fun isPermissionGranted(permissions: Array<String>): Boolean {
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(fragment.requireContext(), it) != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }

    private fun onPermissionDenied() {
        fragment.apply {
            MinervaFlashbar.show(requireActivity(), getString(R.string.permission_error), getString(R.string.permission_error_message))
        }
    }

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setContentView(R.layout.profile_image_menu_layout)
        initializeMenu()
    }

    companion object {
        const val PICK_IMAGE_REQUEST = 13
        const val TAKE_PHOTO_REQUEST = 23
        private const val IMAGE_TITLE = "Profile Image"
        private const val PICK_TYPE = "image/*"
        val TAKE_PHOTO_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val PICK_IMAGE_PERMISSION = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}