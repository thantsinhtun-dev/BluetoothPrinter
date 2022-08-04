package com.stone.bluetoothprinterdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.izettle.html2bitmap.Html2Bitmap
import com.izettle.html2bitmap.content.WebViewContent
import com.stone.bluetoothprinterdemo.Utils.decodeBitmap
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


private const val PERMISSION_BLUETOOTH = 1
private const val PERMISSION_BLUETOOTH_ADMIN = 2
private const val PERMISSION_BLUETOOTH_CONNECT = 3
private const val PERMISSION_BLUETOOTH_SCAN = 4

class MainActivity : AppCompatActivity() {

    private val applicationUUID = UUID
        .fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var mBluetoothManager: BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothSocket: BluetoothSocket
    private lateinit var mSelectedDevice: BluetoothDevice
    private lateinit var mBluetoothConnectProgressDialog: ProgressDialog
    private lateinit var bitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WebView.enableSlowWholeDocumentDraw()
        val html = "<html><body>" +
                "<p style=\"font-size:25px;\">Hello world!<br/>" +
                "Html bitmap" +
                "HellohjjjhafsjkhjhZKglsrjk " +
                "<br>jvjeihvihhvksjhkdfghw" +
                "<br>sjkheroivthgkjakdsjg" +
                "Hellohjjj hafsjkhjhZKgvlsrjk sjkheroivthgk jakdsjg" +
                "     ellohjjj hafsjkhjhZKgvlsrjk sjkheroi           HellohjjjhafsjkhjhZKglsrjk" +
                "<br>jvjeihvihhvksjhkdfghw<br>" +
                "sjkheroivthgkjakdsjg</p></body><html>"

        thread {
            Html2Bitmap.Builder().setContext(this).setContent(WebViewContent.html(html))
                .build().bitmap?.let {
                    bitmap = it

                    runOnUiThread {
                        img.setImageBitmap(bitmap)
                    }
                }
        }
//        img.setImageBitmap(bitmap)

//        webView.settings.javaScriptEnabled = true
//        webView.loadData("<html><body><h1>HellohjjjhafsjkhjhZKglsrjk <br>jvjeihvihhvksjhkdfghw<br>sjkheroivthgkjakdsjg" ,null,null)
//        webView.webViewClient= object :WebViewClient(){
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//                webView.measure(
//                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
//                    View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED)
//                )
////layout of webview
//                webView.layout(0, 0, webView.measuredWidth, webView.getMeasuredHeight())
//
//                webView.isDrawingCacheEnabled = true
//                webView.buildDrawingCache()
////create Bitmap if measured height and width >0
//                if (webView.measuredWidth> 0 && webView.measuredHeight> 0) {
//                    Log.i("Gooo","${webView.measuredWidth} ${webView.height}")
//                    bitmap = Bitmap.createBitmap(
//                        webView.measuredWidth,
//                        webView.measuredHeight, Bitmap.Config.ARGB_8888
//                    )
//                    bitmap.let {
//                        Canvas(bitmap).apply {
//                            drawBitmap(it, 0f, bitmap.height.toFloat(), Paint())
//                            webView.draw(this)
//                        }
//                    }
//
//                    img.setImageBitmap(bitmap)
//                }
////                else null
//// Draw bitmap on canvas
//
//

//        }



    }
    fun getBitmap(
        w: WebView,
        containerWidth: Int,
        containerHeight: Int,
        baseURL: String?,
        content: String?
    ): Bitmap? {
        val signal = CountDownLatch(1)
        val b = Bitmap.createBitmap(containerWidth, containerHeight, Bitmap.Config.ARGB_8888)
        val ready = AtomicBoolean(false)
        w.post {
            w.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    ready.set(true)
                }
            }
            w.setPictureListener { view, picture ->
                if (ready.get()) {
                    val c = Canvas(b)
                    view.draw(c)
                    w.setPictureListener(null)
                    signal.countDown()
                }
            }
//            w.layout(0, 0, rect.width(), rect.height())
            w.loadDataWithBaseURL(baseURL, content!!, "text/html", "UTF-8", null)
        }
        try {
            signal.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return b
    }



    private val registerForBluetoothEnable = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            getDevicesList()
        } else
            Toast.makeText(
                applicationContext,
                "Need bluetooth to connect the printer!!",
                Toast.LENGTH_SHORT
            ).show()
    }
    fun initGSv0Command(bytesByLine: Int, bitmapHeight: Int): ByteArray {
        val xH = bytesByLine / 256
        val xL = bytesByLine - xH * 256
        val yH = bitmapHeight / 256
        val yL = bitmapHeight - yH * 256
        val imageBytes = ByteArray(8 + bytesByLine * bitmapHeight)
        imageBytes[0] = 29
        imageBytes[1] = 118
        imageBytes[2] = 48
        imageBytes[3] = 0
        imageBytes[4] = xL.toByte()
        imageBytes[5] = xH.toByte()
        imageBytes[6] = yL.toByte()
        imageBytes[7] = yH.toByte()
        return imageBytes
    }
    fun bitmapToBytess(bitmap: Bitmap): ByteArray? {
        val bitmapWidth = bitmap.width
        Log.i("Gooow", bitmapWidth.toString() + "")
        val bitmapHeight = bitmap.height
        Log.i("Goooh", bitmapHeight.toString() + "")
        val bytesByLine = Math.ceil((bitmapWidth.toFloat() / 8.0f).toDouble()).toInt()
        val imageBytes: ByteArray = initGSv0Command(bytesByLine, bitmapHeight)
        var i = 8
        var greyscaleCoefficientInit = 0
        val gradientStep = 6
        val colorLevelStep = 765.0 / (15 * gradientStep + gradientStep - 1).toDouble()
        for (posY in 0 until bitmapHeight) {
            var greyscaleCoefficient = greyscaleCoefficientInit
            val greyscaleLine = posY % gradientStep
            var j = 0
            while (j < bitmapWidth) {
                var b = 0
                for (k in 0..7) {
                    val posX = j + k
                    if (posX < bitmapWidth) {
                        val color = bitmap.getPixel(posX, posY)
                        val red = color shr 16 and 255
                        val green = color shr 8 and 255
                        val blue = color and 255
                        if ((red + green + blue).toDouble() < (greyscaleCoefficient * gradientStep + greyscaleLine).toDouble() * colorLevelStep) {
                            b = b or (1 shl 7 - k)
                        }
                        greyscaleCoefficient += 5
                        if (greyscaleCoefficient > 15) {
                            greyscaleCoefficient -= 16
                        }
                    }
                }
                imageBytes[i++] = b.toByte()
                j += 8
            }
            greyscaleCoefficientInit += 2
            if (greyscaleCoefficientInit > 15) {
                greyscaleCoefficientInit = 0
            }
        }
        return imageBytes
    }
    fun print(v: View) {

        if (this::mBluetoothSocket.isInitialized) {

            thread(name = "print", start = true) {
                try {
                    val printer = mBluetoothSocket.outputStream
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
//                    val SELECT_BIT_IMAGE_MODE = byteArrayOf(0x1B, 0x2A, 33)
//                    printer.write(SELECT_BIT_IMAGE_MODE)
                    printer.write(bitmapToBytess(bitmap))
                    val command = decodeBitmap(bitmap)
                    printer.write(command)
                } catch (e: Exception) {
                    Log.i("Goooo", e.toString())
                }
            }


        } else
            Toast.makeText(
                applicationContext,
                "Please connect a printer first!",
                Toast.LENGTH_SHORT
            ).show()
    }


    private fun closeSocket(nOpenSocket: BluetoothSocket) {
        try {
            nOpenSocket.close()
            Log.d("Goo", "SocketClosed")
        } catch (ex: IOException) {
            Log.d("Goo", "CouldNotCloseSocket")
        }
    }


    @SuppressLint("MissingPermission")
    private fun preparePrinter() {
//        mBluetoothSocket=mSelectedDevice.createRfcommSocketToServiceRecord(applicationUUID)
        mBluetoothConnectProgressDialog = ProgressDialog.show(
            this,
            "Connecting...", mSelectedDevice.name + " : "
                    + mSelectedDevice.address, true, false
        )
        thread(name = "connect", start = true) {
            try {
                mBluetoothSocket = mSelectedDevice
                    .createRfcommSocketToServiceRecord(applicationUUID)
                mBluetoothAdapter.cancelDiscovery()
                mBluetoothSocket.connect()
                mBluetoothConnectProgressDialog.dismiss()

            } catch (eConnectException: IOException) {
                Log.d("Gooo", "CouldNotConnectToSocket", eConnectException)
                closeSocket(mBluetoothSocket)
            }
        }
    }

    fun connect(view: View) {
        if (checkPermission()) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            mBluetoothAdapter = mBluetoothManager.adapter
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE
                )
                registerForBluetoothEnable.launch(enableBtIntent)
            } else {
                val deviceList = getDevicesList()
                if (deviceList.isNotEmpty()) {
                    val items = arrayOfNulls<String>(deviceList.size)
                    deviceList.forEachIndexed { index, item ->
                        items[index] = (item.name)
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Select Printer")
                        .setItems(items) { _, i ->
                            mSelectedDevice = deviceList[i]
                            Toast.makeText(applicationContext, "${items[i]}", Toast.LENGTH_SHORT)
                                .show()
                            val button = findViewById<Button>(R.id.connect)
                            button.text = items[i]
                            preparePrinter()
//                            Thread(this).start()
                        }
                        .setCancelable(false)
                        .show()


                } else {
                    Toast.makeText(applicationContext, "No Devices Found!!", Toast.LENGTH_SHORT)
                        .show()
                }


            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDevicesList(): List<BluetoothDevice> {
        val pairedDevices = mBluetoothAdapter.bondedDevices
        val devicesList: MutableList<BluetoothDevice> = mutableListOf()
        if (pairedDevices.size > 0) {
            pairedDevices.forEach { device ->
                devicesList.add(device)
            }
        }
        return devicesList

    }

    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                PERMISSION_BLUETOOTH
            )
            Log.i("Goooo", "denined")
//      ActivityCompat.requestPermissions(this);
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                PERMISSION_BLUETOOTH_ADMIN
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                PERMISSION_BLUETOOTH_CONNECT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                PERMISSION_BLUETOOTH_SCAN
            )
        } else {
            return true
        }
        return false
    }


}