package com.tcqq.placepicker.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.tcqq.placepicker.R
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber

const val REQUEST_GAODE_MAP_PERMISSION = 1

/**
 * @author Alan Dreamer
 * @since 11/10/2018 Created
 */
class MainActivity : BaseActivity(),
        EasyPermissions.PermissionCallbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setActionBar(toolbar)
        setActionBarTitle(R.string.app_name)
        button.setOnClickListener {
            openPlacePickerForGaodeMap()
        }
    }

    @AfterPermissionGranted(REQUEST_GAODE_MAP_PERMISSION)
    private fun openPlacePickerForGaodeMap() {
        val permissions =
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE)
        if (EasyPermissions.hasPermissions(this, *permissions)) {
            // Already have permission, do the thing
            startActivity(Intent(this, PlacePickerActivity::class.java))
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.permission_access_find_location),
                    REQUEST_GAODE_MAP_PERMISSION, *permissions)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Timber.d("onPermissionsGranted: $requestCode: ${perms.size}")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Timber.d("onPermissionsDenied: $requestCode: ${perms.size}")
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.permissions_required_title)
                    .setMessage(R.string.permissions_required_content)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
    }
}