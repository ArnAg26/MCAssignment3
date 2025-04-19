package com.example.matrixcalculator

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var matrixInputsContainer: LinearLayout
    private lateinit var resultTextView: TextView
    private lateinit var operationGroup: RadioGroup

    private val matrixAInputs = mutableListOf<EditText>()
    private val matrixBInputs = mutableListOf<EditText>()

    private var rowsA = 0
    private var colsA = 0
    private var rowsB = 0
    private var colsB = 0

    // Native methods — you'll define these in C++
    external fun addMatrices(a: FloatArray, b: FloatArray, rows: Int, cols: Int): FloatArray
    external fun subtractMatrices(a: FloatArray, b: FloatArray, rows: Int, cols: Int): FloatArray
    external fun multiplyMatrices(a: FloatArray, b: FloatArray, rA: Int, cA: Int, cB: Int): FloatArray
    external fun divideMatrices(a: FloatArray, b: FloatArray, size: Int): FloatArray // assuming square matrices for division

    companion object {
        init {
            System.loadLibrary("matrixops") // Your native library name
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rowsAField = findViewById<EditText>(R.id.rowsA)
        val colsAField = findViewById<EditText>(R.id.colsA)
        val rowsBField = findViewById<EditText>(R.id.rowsB)
        val colsBField = findViewById<EditText>(R.id.colsB)
        val generateBtn = findViewById<Button>(R.id.generateMatricesBtn)
        val calculateBtn = findViewById<Button>(R.id.calculateBtn)
        operationGroup = findViewById(R.id.operationGroup)
        resultTextView = findViewById(R.id.resultTextView)
        matrixInputsContainer = findViewById(R.id.matrixInputsContainer)

        generateBtn.setOnClickListener {
            matrixInputsContainer.removeAllViews()
            matrixAInputs.clear()
            matrixBInputs.clear()

            rowsA = rowsAField.text.toString().toIntOrNull() ?: 0
            colsA = colsAField.text.toString().toIntOrNull() ?: 0
            rowsB = rowsBField.text.toString().toIntOrNull() ?: 0
            colsB = colsBField.text.toString().toIntOrNull() ?: 0

            if (rowsA <= 0 || colsA <= 0 || rowsB <= 0 || colsB <= 0) {
                Toast.makeText(this, "Please enter valid dimensions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addMatrixInputs("Matrix A", rowsA, colsA, matrixAInputs)
            addMatrixInputs("Matrix B", rowsB, colsB, matrixBInputs)
        }

        calculateBtn.setOnClickListener {
            val operation = when (operationGroup.checkedRadioButtonId) {
                R.id.addRadio -> "add"
                R.id.subtractRadio -> "subtract"
                R.id.multiplyRadio -> "multiply"
                R.id.divideRadio -> "divide"
                else -> {
                    Toast.makeText(this, "Please select an operation", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val matrixA = readMatrix(matrixAInputs, rowsA, colsA)
            val matrixB = readMatrix(matrixBInputs, rowsB, colsB)

            if (matrixA == null || matrixB == null) {
                Toast.makeText(this, "Enter valid matrix values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result: FloatArray = try {
                when (operation) {
                    "add" -> {
                        if (rowsA != rowsB || colsA != colsB) {
                            showToast("Add/Subtract requires equal matrix dimensions")
                            return@setOnClickListener
                        }
                        addMatrices(matrixA, matrixB, rowsA, colsA)
                    }

                    "subtract" -> {
                        if (rowsA != rowsB || colsA != colsB) {
                            showToast("Add/Subtract requires equal matrix dimensions")
                            return@setOnClickListener
                        }
                        subtractMatrices(matrixA, matrixB, rowsA, colsA)
                    }

                    "multiply" -> {
                        if (colsA != rowsB) {
                            showToast("Multiplication requires cols(A) == rows(B)")
                            return@setOnClickListener
                        }
                        multiplyMatrices(matrixA, matrixB, rowsA, colsA, colsB)
                    }

                    "divide" -> {
                        if (rowsB != colsB || rowsA != rowsB || colsA != colsB) {
                            showToast("Division requires square, invertible matrix B and A same size")
                            return@setOnClickListener
                        }
                        divideMatrices(matrixA, matrixB, rowsA)
                    }

                    else -> floatArrayOf()
                }
            } catch (e: Exception) {
                showToast("Error performing operation: ${e.message}")
                return@setOnClickListener
            }

            val resultStr = result.toList()
                .chunked(colsB.takeIf { operation == "multiply" } ?: colsA)
                .joinToString("\n") { row -> row.joinToString(" ") { "%.2f".format(it) } }

            resultTextView.text = resultStr
        }
    }

    private fun addMatrixInputs(label: String, rows: Int, cols: Int, list: MutableList<EditText>) {
        val title = TextView(this).apply {
            text = label
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }
        matrixInputsContainer.addView(title)

        for (i in 0 until rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            for (j in 0 until cols) {
                val cell = EditText(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = 4
                    }
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                    gravity = Gravity.CENTER
                    setPadding(8, 4, 8, 4)
                    background = ContextCompat.getDrawable(context, android.R.drawable.editbox_background)
                }
                list.add(cell)
                rowLayout.addView(cell)
            }
            matrixInputsContainer.addView(rowLayout)
        }
    }

    private fun readMatrix(inputList: List<EditText>, rows: Int, cols: Int): FloatArray? {
        return try {
            FloatArray(rows * cols) { i ->
                inputList[i].text.toString().toFloat()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
