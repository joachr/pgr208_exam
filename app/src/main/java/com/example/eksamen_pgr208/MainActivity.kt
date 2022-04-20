package com.example.eksamen_pgr208

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.androidnetworking.AndroidNetworking
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.util.FileUriUtils
import com.github.dhaval2404.imagepicker.util.FileUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.eksamen_pgr208.data.api.ImageModelResult
import com.example.eksamen_pgr208.data.api.ApiServices
import com.example.eksamen_pgr208.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private var imageButton : ImageButton? = null
    private var imageFromCameraOrGallery : ImageView? = null
    private var btnUpload : Button? = null
    private var tvIntroStepOne : TextView? = null
    private var tvIntroStepTwo : TextView? = null
    private var uploadProgressbar : ProgressBar? = null
    var liveDataUploadImage : MutableLiveData<String> = MutableLiveData<String>()
    var liveDataGetImages : MutableLiveData<ImageModelResult> = MutableLiveData<ImageModelResult>()

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val okHttpClient = OkHttpClient().newBuilder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        AndroidNetworking.initialize(this@MainActivity, okHttpClient)

        // Controlling Fragments
        /*val navView: BottomNavigationView = binding.bottomNavigationView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        navView.setupWithNavController(navController)*/

        // getting xml components

        imageFromCameraOrGallery = binding.addedImageFromEitherCameraOrMemory
        btnUpload = binding.btnUpload
        uploadProgressbar = binding.uploadProgressBar
        tvIntroStepOne = binding.tvIntro
        tvIntroStepTwo = binding.tvIntroNext
        imageButton = binding.ibButton

        // hiding elements
        btnUpload?.visibility = View.GONE
        uploadProgressbar?.visibility = View.GONE
        tvIntroStepOne?.visibility = View.VISIBLE
        tvIntroStepTwo?.visibility = View.GONE

        // Get bottom navigation shadow be gone
        val nav : BottomNavigationView = binding.bottomNavigationView
        nav.selectedItemId = R.id.home
        nav.background = null

        imageButton!!.setOnClickListener {
            showCameraAndGalleryDialog()
        }

        nav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.home -> {
                    true
                }
                R.id.saved -> {
                    startActivity(Intent(this, SavedActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }



        liveDataGetImages.observe(this){ item ->

            val imagesArray = Intent(this, ResultActivity::class.java)
            imagesArray.putExtra("images", item)
            startActivity(imagesArray)
        }



    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?,) {
        super.onActivityResult(requestCode, resultCode, data)
        imageChooser(resultCode, data)
    }

    private fun imageChooser(resultCode: Int, data: Intent?) {
        when (resultCode) {
            RESULT_OK -> {
                btnUpload?.visibility = View.VISIBLE
                tvIntroStepOne?.visibility = View.GONE
                tvIntroStepTwo?.visibility = View.VISIBLE

                val uri: Uri = data?.data!!
                val filePath = FileUriUtils.getRealPath(this, uri)
                val fileName = FileUtil.getDocumentFile(this, uri)?.name

                println("filePath: $filePath")
                println("fileName: $fileName")
                println("uri: $uri")

                Glide.with(this)
                    .load(filePath)
                    .transform(RoundedCorners(50))
                    .into(imageFromCameraOrGallery!!)


                try {
                    btnUpload?.setOnClickListener {
                        ApiServices.uploadImage(this@MainActivity, filePath!!)
                        ApiServices.getImages(this@MainActivity)
                        uploadProgressbar?.visibility = View.VISIBLE

                        Toast.makeText(
                            this@MainActivity,
                            "Please wait, searching for similar images...",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Toast.makeText(this, "Image: $fileName chosen", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Exception thrown when trying to choose image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Override
     fun showCameraAndGalleryDialog() {
        // shows dialog (modal) to prompt the user to either choose camera or gallery
        val camOrGallDialog = Dialog(this)
        camOrGallDialog.setContentView(R.layout.dialog_camera_or_gallery)
        camOrGallDialog.setTitle("Choose source: ")

        val btnGallery : ImageButton = camOrGallDialog.findViewById(R.id.btn_gallery)
        val btnCamera : ImageButton = camOrGallDialog.findViewById(R.id.btn_camera)

        btnGallery.setOnClickListener {
            ImagePicker.Companion.with(this)
                .galleryOnly()
                .galleryMimeTypes(arrayOf("image/*"))
                .maxResultSize(400, 400)
                .compress(1024)
                .start()
            camOrGallDialog.dismiss()

            println("gallery clicked")
        }

        btnCamera.setOnClickListener {
            ImagePicker.with(this)
                .cameraOnly()
                .maxResultSize(400, 400)
                .compress(1024)
                .start()
            camOrGallDialog.dismiss()
            println("camera clicked")
        }
        camOrGallDialog.show()
    }

}