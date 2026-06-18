package com.yourname.emojimosaic

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.yourname.emojimosaic.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mosaicGenerator: MosaicGenerator
    private var currentMosaic: String = ""

    // Image picker launcher
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mosaicGenerator = MosaicGenerator(this)

        // Load KD-Tree in background when app starts
        CoroutineScope(Dispatchers.IO).launch {
            mosaicGenerator.loadKdTree()
            withContext(Dispatchers.Main) {
                binding.tvStatus.text = "Ready! Pick an image."
            }
        }

        // Pick image button
        binding.btnPickImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Save button
        binding.btnSave.setOnClickListener {
            saveMosaic()
        }
    }

    // Process the selected image
    private fun processImage(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Generating mosaic..."
        binding.btnSave.visibility = View.GONE
        binding.tvMosaic.text = ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load bitmap from URI
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        binding.tvStatus.text = "Failed to load image."
                        binding.progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                // Generate mosaic
                val mosaic = mosaicGenerator.generateMosaic(bitmap)
                currentMosaic = mosaic

                withContext(Dispatchers.Main) {
                    binding.tvMosaic.text = mosaic
                    binding.tvStatus.text = "Mosaic ready! ✅"
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "Error: ${e.message}"
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // Save mosaic as image to gallery
    private fun saveMosaic() {
        if (currentMosaic.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Render the TextView to a bitmap
                val view = binding.tvMosaic
                val bitmap = Bitmap.createBitmap(
                    view.width, view.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                view.draw(canvas)

                // Save to MediaStore (gallery)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "mosaic_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EmojiMosaic")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Saved to Gallery! 🎉", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
