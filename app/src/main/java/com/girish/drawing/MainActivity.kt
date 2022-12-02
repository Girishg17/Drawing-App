package com.girish.drawing

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.security.Permission


class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView?=null
    private var mImageButtonCurrentPaint:ImageButton?=null
    var customProgressDialog:Dialog?=null
    private var eraser:ImageButton?=null
    val openGalleryLauncher:ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
                if(result.resultCode== RESULT_OK && result.data !=null ){

                    val imageback=findViewById<ImageView>(R.id.iv_background)
                    imageback.setImageURI(result.data?.data)

                }

        }
    val  requestPermission:ActivityResultLauncher<Array<String>> =
      registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
          permissions->
            permissions.entries.forEach{
                val permissionName=it.key
                val isGranted=it.value
                if(isGranted){
                    Toast.makeText(this@MainActivity,"permission granted for storage",Toast.LENGTH_LONG).show()
                    val pickintent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                      openGalleryLauncher.launch(pickintent)
                }
                else{
                    if(permissionName== Manifest.permission.READ_EXTERNAL_STORAGE ){
                        Toast.makeText(this@MainActivity,"OOPS deneid permission for storage",Toast.LENGTH_LONG).show()
                    }
                }
            }
      }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        drawingView=findViewById(R.id.drawing_view)
        eraser=findViewById(R.id.eraser)
        eraser!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.palette_pressed)
        )

        drawingView?.setSizeBrush(20.toFloat())
        val linearLayoutPaintColors=findViewById<LinearLayout>(R.id.ll_paint_color)
        mImageButtonCurrentPaint=linearLayoutPaintColors[2] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.palette_pressed)
        )
        val ib_brush:ImageButton=findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener(){
            showBrushSizechoose()
        }
        val ib_undo:ImageButton=findViewById(R.id.ib_undo)
        ib_undo.setOnClickListener(){
                  drawingView?.onClickUndo()
        }
        val ib_redo:ImageButton=findViewById(R.id.ib_redo)
        ib_redo.setOnClickListener(){
            drawingView?.onClickRedo()
        }
        val ib_save:ImageButton=findViewById(R.id.ib_save)
        ib_save.setOnClickListener{
            if(isReadStoragePermission()){
                showProgressDialogue()
                lifecycleScope.launch{
                    val flDrawingView:FrameLayout=findViewById(R.id.fl_drawing_view_container)

                    saveBitmapFile(getbitMapFromView(flDrawingView))
                }
            }
        }
        val ibGallery=findViewById<ImageButton>(R.id.ib_gallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }
    }
private fun showBrushSizechoose(){
    val brushDialogue=Dialog(this)
    brushDialogue.setContentView(R.layout.dialogue_brush_size)
    brushDialogue.setTitle("Brush Size")
    val smallbtn:ImageButton=brushDialogue.findViewById(R.id.ib_small_brush)
    smallbtn.setOnClickListener(){
        drawingView?.setSizeBrush(10.toFloat())
        brushDialogue.dismiss()
    }
    val medium:ImageButton=brushDialogue.findViewById(R.id.ib_medium_brush)
    medium.setOnClickListener(){
        drawingView?.setSizeBrush(20.toFloat())
        brushDialogue.dismiss()
    }
    val large:ImageButton=brushDialogue.findViewById(R.id.ib_large_brush)
    large.setOnClickListener(){
        drawingView?.setSizeBrush(30.toFloat())
        brushDialogue.dismiss()
    }
    brushDialogue.show()
}
    fun paintClicked(view:View){
if(view !=mImageButtonCurrentPaint) {

    val imagebutn=view as ImageButton
    val colorTag=imagebutn.tag.toString()
    drawingView?.setColor(colorTag)
    imagebutn.setImageDrawable(
        ContextCompat.getDrawable(this,R.drawable.palette_pressed)
    )
    mImageButtonCurrentPaint!!.setImageDrawable(
        ContextCompat.getDrawable(this,R.drawable.palette_normal)
    )
  mImageButtonCurrentPaint=view
}
}
    private fun isReadStoragePermission():Boolean{
        val result=ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
    return result==PackageManager.PERMISSION_GRANTED
    }
    private fun requestStoragePermission(){
           if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
               showRationaleDialogue("Kids Drawing App","kids drawing app"+"needs to Acess Your External Storage")
           }
        else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                // TODO: add writing external storage permission
            ))
           }
    }
    private fun getbitMapFromView(view:View):Bitmap{

        val returnBitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(returnBitmap)
        val bgDrawable=view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnBitmap

    }
    private suspend fun saveBitmapFile(mBitmap:Bitmap?):String{
        var result=""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){
                 try {
                     val bytes=ByteArrayOutputStream()
                     mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                     val f= File(externalCacheDir?.absoluteFile.toString()
                        + File.separator+"KidsDrawingApp_" + System.currentTimeMillis()/1000 + ".png"
                     )
                     val fo=FileOutputStream(f)
                     fo.write(bytes.toByteArray())
                     fo.close()
                     result=f.absolutePath
                     runOnUiThread(){
                         cancelProgressDialog()
                         if(result.isNotEmpty()){
                             Toast.makeText(this@MainActivity,"file saved succesfully:$result",Toast.LENGTH_LONG).show()
                         shareImage(result)
                         }
                         else{
                             Toast.makeText(this@MainActivity,"something went wrong",Toast.LENGTH_LONG).show()
                         }
                     }

                 }
                 catch (e:Exception){
                     result=""
                     e.printStackTrace()
                 }
            }
        }
return result
    }
    private fun showProgressDialogue(){
        customProgressDialog=Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialogue_custom_progress)
        customProgressDialog?.show()
    }
    private fun cancelProgressDialog(){
        if(customProgressDialog!=null){
            customProgressDialog?.dismiss()
            customProgressDialog=null
        }
    }
    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path,uri->
              val shareIntent=Intent()
            shareIntent.action=Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type="image/png"
            startActivity(Intent.createChooser(shareIntent,"share"))
        }
    }
    private fun showRationaleDialogue(
        title:String,
        message:String,
    ){
        val builder:AlertDialog.Builder=AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("cancel"){
                dialog,_ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
    }
