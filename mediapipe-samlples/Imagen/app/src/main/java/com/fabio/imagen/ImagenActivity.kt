package com.fabio.imagen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fabio.imagen.databinding.ActivityMainBinding
import com.fabio.imagen.diffusion.DiffusionActivity
import com.fabio.imagen.loraweights.LoRAWeightActivity


class ImagenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDiffusion.setOnClickListener {
            startActivity(Intent(this, DiffusionActivity::class.java))
        }


        binding.btnLoRA.setOnClickListener {
            startActivity(Intent(this, LoRAWeightActivity::class.java))
        }
    }

}