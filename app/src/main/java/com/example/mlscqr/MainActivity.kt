package com.example.mlscqr

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var scanButton: ImageButton
    private lateinit var importButton: Button
    private lateinit var downloadAllButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var database: DatabaseReference
    private lateinit var clearStudentsButton: Button
    private lateinit var clearVerifiedButton: Button
    private lateinit var downloadCseButton: Button
    private lateinit var downloadCseAiButton: Button
    private lateinit var downloadAimlButton: Button
    private lateinit var downloadEceButton: Button
    private lateinit var downloadEeeButton: Button
    private lateinit var downloadCstButton: Button
    private lateinit var downloadCivilButton: Button
    private lateinit var downloadMechButton: Button
    private lateinit var downloadEctButton: Button
    private lateinit var downloadCdsButton: Button

    private val PICK_EXCEL_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById(R.id.btnScan)
        importButton = findViewById(R.id.btnImport)
        downloadAllButton = findViewById(R.id.btnDownloadAll)
        resultTextView = findViewById(R.id.tvResult)
        clearStudentsButton = findViewById(R.id.btnClearStudents)
        clearVerifiedButton = findViewById(R.id.btnClearVerified)
        downloadCseButton = findViewById(R.id.btnDownloadCse)
        downloadCseAiButton = findViewById(R.id.btnDownloadCseai)
        downloadAimlButton = findViewById(R.id.btnDownloadAiml)
        downloadEceButton = findViewById(R.id.btnDownloadEce)
        downloadEeeButton = findViewById(R.id.btnDownloadEee)
        downloadCstButton = findViewById(R.id.btnDownloadCst)
        downloadCivilButton = findViewById(R.id.btnDownloadCivil)
        downloadMechButton = findViewById(R.id.btnDownloadMech)
        downloadEctButton = findViewById(R.id.btnDownloadEct)
        downloadCdsButton = findViewById(R.id.btnDownloadCds)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this, this)

        scanButton.setOnClickListener {
            startQRScanner()
        }

        importButton.setOnClickListener {
            pickExcelFile()
        }

        downloadAllButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudents", "AllVerifiedStudents.xlsx")
        }

        downloadCseButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsCse", "VerifiedStudentsCse.xlsx")
        }

        downloadCseAiButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsCseAi", "VerifiedStudentsCseAi.xlsx")
        }

        downloadAimlButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsAiml", "VerifiedStudentsAiml.xlsx")
        }

        downloadEceButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsEce", "VerifiedStudentsEce.xlsx")
        }

        downloadEeeButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsEee", "VerifiedStudentsEee.xlsx")
        }

        downloadCstButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsCst", "VerifiedStudentsCst.xlsx")
        }

        downloadCivilButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsCivil", "VerifiedStudentsCivil.xlsx")
        }

        downloadMechButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsMech", "VerifiedStudentsMech.xlsx")
        }

        downloadEctButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsEct", "VerifiedStudentsEct.xlsx")
        }

        downloadCdsButton.setOnClickListener {
            downloadVerifiedStudentsAsExcel("verifiedStudentsCds", "VerifiedStudentsCds.xlsx")
        }

        clearStudentsButton.setOnClickListener {
            showConfirmationDialog("Confirm Deletion", "Are you sure you want to delete?") {
                clearStudentsData()
            }
        }
        clearVerifiedButton.setOnClickListener {
            showConfirmationDialog("Confirm Deletion", "Are you sure you want to delete?") {
                clearVerifiedData() }
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a QR Code")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    private fun pickExcelFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        startActivityForResult(intent, PICK_EXCEL_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_EXCEL_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                importDataFromExcel(uri)
            }
        } else if (requestCode == IntentIntegrator.REQUEST_CODE) {
            val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents == null) {
                    resultTextView.text = "Cancelled"
                } else {
                    checkScannedData(result.contents)
                }
            }
        }
    }

    private fun importDataFromExcel(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val totalRows = sheet.physicalNumberOfRows

            for (i in 1 until totalRows) { // Skip the header row
                val row = sheet.getRow(i)
                if (row != null) {
                    var rollNumber = row.getCell(0)?.toString()?.trim()?.uppercase()
                    val name = row.getCell(1)?.toString()?.trim()
                    var branch = row.getCell(2)?.toString()?.trim()

                    if (branch.equals("CAI", ignoreCase = true)) branch = "CSE(AI)"
                    if (branch.equals("Mech", ignoreCase = true)) branch = "Mechanical"

                    if (!rollNumber.isNullOrEmpty() && !name.isNullOrEmpty() && !branch.isNullOrEmpty()) {
                        // Store data in Firebase
                        database.child("students").child(rollNumber).setValue(
                            mapOf(
                                "name" to name,
                                "branch" to branch
                            )
                        ).addOnSuccessListener {
                            Log.d("Firebase", "Data for $rollNumber added successfully")
                        }.addOnFailureListener { e ->
                            Log.e("Firebase", "Error adding data for $rollNumber: ${e.message}")
                        }
                    } else {
                        Log.w("Excel", "Row $i contains invalid data and was skipped")
                    }
                }
            }
            Toast.makeText(this, "Data imported successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to import data: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("ExcelImport", "Error importing Excel data", e)
        }
    }

    private fun checkScannedData(rollNumber: String) {
        val normalizedRollNumber = rollNumber.trim().lowercase()
        database.child("students").get().addOnSuccessListener { snapshot ->
            var matched = false
            var branchNode = "verifiedStudents"  // Default node
            val verifiedStudentsMap = TreeMap<String, Map<String, String>>()
            val verifiedBranchMap = TreeMap<String, Map<String, String>>()

            // Step 1: Retrieve existing verified students data
            database.child("verifiedStudents").get().addOnSuccessListener { verifiedSnapshot ->
                verifiedSnapshot.children.forEach { student ->
                    val existingRollNumber = student.key?.lowercase()
                    if (existingRollNumber != null) {
                        val name = student.child("name").value.toString()
                        val branch = student.child("branch").value.toString()
                        verifiedStudentsMap[existingRollNumber] = mapOf("name" to name, "branch" to branch)
                    }
                }

                snapshot.children.forEach { student ->
                    val dbRollNumber = student.key?.lowercase()
                    if (dbRollNumber == normalizedRollNumber) {
                        val name = student.child("name").value.toString()
                        val branch = student.child("branch").value.toString()

                        val message = "Welcome $name from $branch"
                        resultTextView.text = message
                        speakOut(message)

                        branchNode = when (branch) {
                            "CSE(AI)" -> "verifiedStudentsCseAi"
                            "AIML" -> "verifiedStudentsAiml"
                            "CSE" -> "verifiedStudentsCse"
                            "ECE" -> "verifiedStudentsEce"
                            "EEE" -> "verifiedStudentsEee"
                            "CST" -> "verifiedStudentsCst"
                            "Civil" -> "verifiedStudentsCivil"
                            "Mech" -> "verifiedStudentsMech"
                            "ECT" -> "verifiedStudentsEct"
                            "CSE(DS)", "CDS" -> "verifiedStudentsCds"
                            else -> "verifiedStudents"
                        }

                        val studentData = mapOf(
                            "name" to name,
                            "branch" to branch
                        )

                        // Step 2: Add the new verified student without replacing existing ones
                        verifiedStudentsMap[dbRollNumber!!] = studentData

                        // Step 3: Retrieve existing data for the branch node before updating
                        database.child(branchNode).get().addOnSuccessListener { branchSnapshot ->
                            branchSnapshot.children.forEach { student ->
                                val existingRollNumber = student.key?.uppercase()
                                if (existingRollNumber != null) {
                                    val name = student.child("name").value.toString()
                                    val branch = student.child("branch").value.toString()
                                    verifiedBranchMap[existingRollNumber] = mapOf("name" to name, "branch" to branch)
                                }
                            }

                            // Step 4: Add the new verified student for the branch
                            verifiedBranchMap[dbRollNumber] = studentData

                            // Step 5: Save updated maps to Firebase
                            database.child("verifiedStudents").setValue(verifiedStudentsMap)
                                .addOnSuccessListener {
                                    Log.d("Firebase", "Verified data updated in sorted order in verifiedStudents")
                                }.addOnFailureListener { e ->
                                    Log.e("Firebase", "Error updating verifiedStudents: ${e.message}")
                                }

                            if (branchNode.isNotEmpty() && branchNode != "verifiedStudents") {
                                database.child(branchNode).setValue(verifiedBranchMap)
                                    .addOnSuccessListener {
                                        Log.d("Firebase", "Verified data updated in sorted order in $branchNode")
                                    }.addOnFailureListener { e ->
                                        Log.e("Firebase", "Error updating $branchNode: ${e.message}")
                                    }
                            }
                        }

                        matched = true
                        return@forEach
                    }
                }

                if (!matched) {
                    resultTextView.text = "No data found for roll number $rollNumber"
                    speakOut("No data found")
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to retrieve verified students: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to check data: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun downloadVerifiedStudentsAsExcel(branchNode: String, fileName: String) {
        database.child(branchNode).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Verified Students")

                // Add header row
                val header = sheet.createRow(0)
                header.createCell(0).setCellValue("Roll Number")
                header.createCell(1).setCellValue("Name")
                header.createCell(2).setCellValue("Branch")

                // Add data rows
                var rowIndex = 1
                snapshot.children.forEach { student ->
                    val row = sheet.createRow(rowIndex++)
                    row.createCell(0).setCellValue(student.key?.uppercase())
                    row.createCell(1).setCellValue(student.child("name").value.toString())
                    row.createCell(2).setCellValue(student.child("branch").value.toString())
                }

                // Save the workbook to local storage
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                FileOutputStream(file).use { outputStream ->
                    workbook.write(outputStream)
                }
                Toast.makeText(this, "Excel file saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "No verified students found!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to download data: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearStudentsData() {
        database.child("students").removeValue().addOnSuccessListener {
            Toast.makeText(this, "Students data cleared successfully!", Toast.LENGTH_SHORT).show()
            Log.d("Firebase", "Students data cleared.")
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to clear students data: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("Firebase", "Error clearing students data: ${e.message}")
        }
    }

    private fun clearVerifiedData() {
        val branches = listOf(
            "verifiedStudentsCseAi", "verifiedStudentsAiml", "verifiedStudentsCse", "verifiedStudentsEce",
            "verifiedStudentsEee", "verifiedStudentsCst", "verifiedStudentsCivil", "verifiedStudentsMech",
            "verifiedStudentsEct", "verifiedStudentsCds","verifiedStudents"
        )

        branches.forEach { branch ->
            database.child(branch).removeValue().addOnSuccessListener {
                Log.d("Firebase", "$branch data cleared.")
            }.addOnFailureListener { e ->
                Log.e("Firebase", "Error clearing $branch data: ${e.message}")
            }
        }

        Toast.makeText(this, "Verified users data cleared successfully!", Toast.LENGTH_SHORT).show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.ENGLISH
        }
    }

    private fun speakOut(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}