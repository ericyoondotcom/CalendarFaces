package com.yoonicode.calendarfaces.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yoonicode.calendarfaces.R
import com.yoonicode.calendarfaces.databinding.ActivityPermissionsRequestBinding

class PermissionsRequestActivity : AppCompatActivity() {
    private var binding: ActivityPermissionsRequestBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions_request)

        binding = ActivityPermissionsRequestBinding.inflate(layoutInflater)
        startGrantFlow()
    }

    private fun startGrantFlow() {
        Log.d("F", "Start Grant Flow called")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CALENDAR),
            1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(applicationContext, "Permissions obtained!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(applicationContext, "App will not function without calendar permission", Toast.LENGTH_LONG).show()
        }
    }
}
