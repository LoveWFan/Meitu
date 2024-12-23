package com.fabio.meitu

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.fabio.meitu.databinding.ActivityMainBinding
import com.fabio.meitu.interactiveseg.interactivesegmentation.InteractiveSegMainActivity

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        activityMainBinding.imageSeg.setOnClickListener {
            startActivity(Intent(this, InteractiveSegMainActivity::class.java))
        }

        activityMainBinding.imageGen.setOnClickListener {

        }
    }
}