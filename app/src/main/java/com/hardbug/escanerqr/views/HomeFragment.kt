package com.hardbug.escanerqr.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.database.AppDatabase
import com.hardbug.escanerqr.models.ImageCode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class HomeFragment : Fragment() {

    private lateinit var rvRecentCodes: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvRecentTitle: TextView
    private lateinit var editTextSearch: TextInputEditText
    private lateinit var adapter: QrCodeAdapter
    private lateinit var textInputLayout: com.google.android.material.textfield.TextInputLayout
    
    private var allCodes: List<ImageCode> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        rvRecentCodes = view.findViewById(R.id.rvRecentCodes)
        emptyState = view.findViewById(R.id.emptyState)
        tvRecentTitle = view.findViewById(R.id.tvRecentTitle)
        editTextSearch = view.findViewById(R.id.editTextSearch)
        textInputLayout = view.findViewById(R.id.textInputLayoutSearch)

        textInputLayout.visibility = View.GONE
        
        setupRecyclerView()
        setupSearch()
        observeCodes()
        
        return view
    }

    private fun setupRecyclerView() {
        adapter = QrCodeAdapter(
            onDeleteClick = { item ->
                showDeleteConfirmation(item)
            },
            onItemClick = { item ->
                showQrDetail(item)
            }
        )
        rvRecentCodes.adapter = adapter
    }

    private fun setupSearch() {
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCodes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterCodes(query: String) {
        val filteredList = if (query.isEmpty()) {
            allCodes
        } else {
            allCodes.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filteredList)
        
        if (filteredList.isEmpty() && allCodes.isNotEmpty()) {
            Snackbar.make(requireView(), R.string.no_results, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showQrDetail(item: ImageCode) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_detail, null)
        val ivDetailQr: ImageView = dialogView.findViewById(R.id.ivDetailQr)
        val tvDetailName: TextView = dialogView.findViewById(R.id.tvDetailName)
        val btnDetailShare: MaterialButton = dialogView.findViewById(R.id.btnDetailShare)
        val btnDetailClose: MaterialButton = dialogView.findViewById(R.id.btnDetailClose)

        tvDetailName.text = item.name
        
        if (item.metaData == "SCANNED") {
            ivDetailQr.setImageResource(R.drawable.ic_barcode)
        } else {
            try {
                ivDetailQr.setImageURI(Uri.parse(item.urlPath))
            } catch (e: Exception) {
                ivDetailQr.setImageResource(R.drawable.baseline_qr_code_24)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomMaterialAlertDialog)
            .setView(dialogView)
            .create()

        btnDetailClose.setOnClickListener { dialog.dismiss() }
        
        btnDetailShare.setOnClickListener {
            shareImage(item)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun shareImage(item: ImageCode) {
        if (item.metaData == "SCANNED") {
            // Compartir como texto si es un escaneo
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, item.urlPath)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code)))
        } else {
            // Compartir como archivo usando FileProvider para códigos generados
            try {
                val fileUri = Uri.parse(item.urlPath)
                val fileName = fileUri.lastPathSegment ?: "qr_code.png"
                val file = File(requireContext().filesDir, "codes/$fileName")
                
                if (file.exists()) {
                    val contentUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.hardbug.escanerqr.fileprovider",
                        file
                    )
                    
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        type = "image/png"
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code)))
                } else {
                    Snackbar.make(requireView(), "El archivo no existe", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Snackbar.make(requireView(), "Error al compartir: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation(item: ImageCode) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_message)
            .setNegativeButton(R.string.delete_negative) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.delete_positive) { _, _ ->
                deleteCode(item.imageCodeUuid)
            }
            .show()
    }

    private fun observeCodes() {
        val db = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            db.imageCodeDao().getAllImageCodes().collectLatest { codes ->
                allCodes = codes
                if (codes.isEmpty()) {
                    rvRecentCodes.visibility = View.GONE
                    tvRecentTitle.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                    textInputLayout.visibility = View.GONE
                } else {
                    rvRecentCodes.visibility = View.VISIBLE
                    tvRecentTitle.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                    textInputLayout.visibility = View.VISIBLE
                    adapter.submitList(codes)
                    filterCodes(editTextSearch.text.toString())
                }
            }
        }
    }

    private fun deleteCode(uuid: String) {
        val db = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            db.imageCodeDao().deleteImageCode(uuid)
        }
    }
}