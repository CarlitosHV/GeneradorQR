package com.hardbug.escanerqr.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.database.AppDatabase
import com.hardbug.escanerqr.models.ImageCode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var rvRecentCodes: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvRecentTitle: TextView
    private lateinit var adapter: QrCodeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        rvRecentCodes = view.findViewById(R.id.rvRecentCodes)
        emptyState = view.findViewById(R.id.emptyState)
        tvRecentTitle = view.findViewById(R.id.tvRecentTitle)
        
        setupRecyclerView()
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

    private fun showQrDetail(item: ImageCode) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_detail, null)
        val ivDetailQr: ImageView = dialogView.findViewById(R.id.ivDetailQr)
        val tvDetailName: TextView = dialogView.findViewById(R.id.tvDetailName)
        val btnDetailShare: MaterialButton = dialogView.findViewById(R.id.btnDetailShare)
        val btnDetailClose: MaterialButton = dialogView.findViewById(R.id.btnDetailClose)

        tvDetailName.text = item.name
        try {
            ivDetailQr.setImageURI(Uri.parse(item.urlPath))
        } catch (e: Exception) {
            ivDetailQr.setImageResource(R.drawable.baseline_qr_code_24)
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
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, Uri.parse(item.urlPath))
                type = "image/png"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code)))
        } catch (e: Exception) {
            e.printStackTrace()
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
                if (codes.isEmpty()) {
                    rvRecentCodes.visibility = View.GONE
                    tvRecentTitle.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    rvRecentCodes.visibility = View.VISIBLE
                    tvRecentTitle.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                    adapter.submitList(codes)
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