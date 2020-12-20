package com.speedpubread.oceo.speedread

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

class FileSelector : AppCompatActivity() {
    private val TAG = "FileSelector"
    private val EXTERNAL_STORAGE_READ_PERMISSION_REQ = 3
    var activity // set when launchFile initially called. maybe a better way?
            : Activity? = null
    var frag: Fragment? = null
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // file permission request callback
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        Log.d("permissionsReqResult", requestCode.toString())
        when (requestCode) {
            EXTERNAL_STORAGE_READ_PERMISSION_REQ -> {
//                Log.d(TAG, "Read External storage Permission")
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0])
                    launchFileChooser(frag)
                } else {
//                    Log.d("Permission", "File Perm not granted")
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun launchFileChooser(frag: Fragment?) {
            var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
            chooseFile.type = "*/*"
            chooseFile = Intent.createChooser(chooseFile, "Choose a file")
            frag!!.startActivityForResult(chooseFile, 1)
        }

        @JvmStatic
        fun requestReadPermission(activity: Activity?) {
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3)
        }
    }
}