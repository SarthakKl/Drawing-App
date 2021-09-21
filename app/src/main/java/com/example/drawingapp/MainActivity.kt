package com.example.drawingapp


import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var savedImageUri: Uri? = null

    companion object {
        private const val STORAGE_REQUEST_CODE = 1
        var imageSaved = false
    }

    private val getImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->

            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data != null) {
                    val image = findViewById<ImageView>(R.id.Image)
                    image.visibility = View.VISIBLE
                    image.setImageURI(result.data!!.data)
                    imageSaved = false
                } else
                    Toast.makeText(
                        this,
                        "ERROR FETCHING IMAGE FROM THE GALLERY",
                        Toast.LENGTH_SHORT
                    )
                        .show()
            } else {
                Toast.makeText(
                    applicationContext,
                    "CANT GET IMAGE FROM THE GALLERY",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun getBitmap(view: View): Bitmap {
        val returnedBitmap: Bitmap =
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val returnedCanvas = Canvas(returnedBitmap)

        val bg = view.background

        if (bg != null) {
            bg.draw(returnedCanvas)  // Background color of the view is drawn onto the canvas
        } else
            returnedCanvas.drawColor(Color.WHITE)

        view.draw(returnedCanvas)  // this method is used to draw all view(including children) onto the canvas

        return returnedBitmap
    }


    private fun shareImage() {
        /*MediaScannerConnection.scanFile(this,arrayOf(savedImagePath!!),null){
            path,uri-> val sharedIntent=Intent()
            sharedIntent.action=Intent.ACTION_SEND
            sharedIntent.putExtra(Intent.EXTRA_STREAM,uri)
            sharedIntent.setType("image/jpeg")

            startActivity(
                Intent.createChooser(sharedIntent,"Share")
            )

        }*/
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, savedImageUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/jpeg"
        startActivity(Intent.createChooser(intent, "Share Image"))
    }

    private fun saveImage(mbitmap: Bitmap?): Boolean/*String*/ { // to run code in Background Thread
        // do async work
        // https://www.youtube.com/watch?v=dEASm7nv7DA
        // Below code is taken from the link provided above

        val displayName = "Drawing" + System.currentTimeMillis() / 1000

        val imageCollection = sdkVersion29andUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpeg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, mbitmap!!.width)
            put(MediaStore.Images.Media.HEIGHT, mbitmap!!.height)
        }

        return try {
            contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                savedImageUri = uri
                contentResolver.openOutputStream(uri).use { outputstream ->
                    if (!mbitmap!!.compress(Bitmap.CompressFormat.JPEG, 95, outputstream))
                        throw IOException("Couldn't save Bitmap")
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
        /*
            For android version below 10, U can also save the image in the cache directory
            of the app in the external storage. As for android version above 10 scoped storage
            is made mandatory, hence you can't access any app specific directories from any other
            app. So you will not be able to share image using this method and also you will not be
            able to see the image in gallery*/

        /*
            savedImagePath=""
            if(mbitmap!=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mbitmap.compress(Bitmap.CompressFormat.JPEG, 95, bytes)

                    val f = File(
                        externalCacheDir!!.absoluteFile.toString() + File.separator
                                + "DrawingApp." + System.currentTimeMillis() / 1000 + ".jpeg"
                    )
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                     savedImagePath= f.absolutePath
                }
                catch (e:Exception){
                    e.printStackTrace()
                }
            }
        return savedImagePath!!*/
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        /* Uncomment this if you want to set the CanvasView programmatically rather than the use
        of activity_main.xml
         */
        /*
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val myCanvasView= MyCanvasView(this)
        window.setDecorFitsSystemWindows(false)
        myCanvasView.contentDescription= getString(R.string.canvasContentDescription)
        setContentView(myCanvasView)*/

        super.onCreate(savedInstanceState)
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        val theCanvas = findViewById<MyCanvasView>(R.id.canvas)
        theCanvas.setSizeForBrush(20.0f)

        val brushSize = findViewById<ImageButton>(R.id.brush)
        brushSize.setOnClickListener {
            brushChangeDialogBox()
        }
        findViewById<ImageButton>(R.id.gallery).setOnClickListener {
            if (checkForPermission()) {
                //Code for opening the gallery
                val galleryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                getImage.launch(galleryIntent)
            } else
                askForPermission()
        }
        findViewById<ImageButton>(R.id.undoBtn).setOnClickListener {
            theCanvas.undo()
        }
        findViewById<ImageButton>(R.id.savebtn).setOnClickListener {
            if (checkForPermission()) {
                if (!imageSaved) {
                    saveImage(getBitmap(findViewById(R.id.FrameLayout)))
                    Toast.makeText(this, "Image successfully saved", Toast.LENGTH_SHORT)
                        .show()
                    imageSaved = true
                } else if (imageSaved)
                    Toast.makeText(this, "Image already saved to Storage", Toast.LENGTH_SHORT)
                        .show()
                else
                    Toast.makeText(this, "Error saving the image", Toast.LENGTH_SHORT).show()
            } else
                askForPermission()
        }

        findViewById<ImageButton>(R.id.share).setOnClickListener {
            if (!imageSaved) {
                saveImage(getBitmap(findViewById(R.id.FrameLayout)))
                imageSaved = true
                shareImage()
            } else {
                shareImage()
            }
        }
        findViewById<ImageButton>(R.id.color_picker).setOnClickListener {
            color_picker()
        }
    }

    private fun color_picker() {
        ColorPickerDialog
            .Builder(this)                        // Pass Activity Instance
            .setTitle("Pick Theme")            // Default "Choose Color"
            .setColorShape(ColorShape.SQAURE)   // Default ColorShape.CIRCLE
            .setDefaultColor(R.color.black)     // Pass Default Color
            .setColorListener { color, colorHex ->
                // Handle Color Selection
                findViewById<MyCanvasView>(R.id.canvas).setColor(colorHex)
            }
            .show()

    }

    private fun checkForPermission(): Boolean {

        val bool: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bool = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            bool = (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED)
        }

        return bool
    }

    private fun askForPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) ||
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            Toast.makeText(
                this,
                "These permissions are required to get image from the gallery",
                Toast.LENGTH_SHORT
            ).show()
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), STORAGE_REQUEST_CODE
        )

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "PERMISSION GRANTED", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "PERMISSION DENIED", Toast.LENGTH_SHORT).show()
        }
    }

    private fun brushChangeDialogBox() {
        val canvas = findViewById<MyCanvasView>(R.id.canvas)
        val dialogBox = Dialog(this)
        dialogBox.setContentView(R.layout.brush_thickness)
        dialogBox.setTitle("Brush Size")

        val smallBtn = dialogBox.findViewById<ImageButton>(R.id.small_brush_thickness)
        smallBtn.setOnClickListener {
            canvas.setSizeForBrush(10.0f)
            dialogBox.dismiss()
        }

        val mediumBtn = dialogBox.findViewById<ImageButton>(R.id.medium_brush_thickness)
        mediumBtn.setOnClickListener {
            canvas.setSizeForBrush(20.0f)
            dialogBox.dismiss()
        }

        val largeBtn = dialogBox.findViewById<ImageButton>(R.id.large_brush_thickness)
        largeBtn.setOnClickListener {
            canvas.setSizeForBrush(30.0f)
            dialogBox.dismiss()
        }
        dialogBox.show()
    }
}

