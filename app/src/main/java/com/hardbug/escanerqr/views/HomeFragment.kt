package com.hardbug.escanerqr.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.database.AppDatabase
import com.hardbug.escanerqr.models.ImageCode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class HomeFragment : Fragment() {

    private lateinit var rvRecentCodes: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView
    private lateinit var editTextSearch: TextInputEditText
    private lateinit var adapter: QrCodeAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var textInputLayout: com.google.android.material.textfield.TextInputLayout
    
    private var allCodes: List<ImageCode> = emptyList()
    private var currentTab = 0
    private var hasVibratedAtLimit = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        rvRecentCodes = view.findViewById(R.id.rvRecentCodes)
        emptyState = view.findViewById(R.id.emptyState)
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage)
        editTextSearch = view.findViewById(R.id.editTextSearch)
        textInputLayout = view.findViewById(R.id.textInputLayoutSearch)
        tabLayout = view.findViewById(R.id.tabLayout)

        setupRecyclerView()
        setupTabs()
        setupSearch()
        observeCodes()

        ViewCompat.setOnApplyWindowInsetsListener(rvRecentCodes) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomNavHeight = resources.getDimensionPixelSize(R.dimen.fab_margin) * 4
            v.updatePadding(bottom = insets.bottom + bottomNavHeight)
            windowInsets
        }
        
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

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.5f

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList[position]

                if (direction == ItemTouchHelper.RIGHT) {
                    showDeleteConfirmation(item)
                } else if (direction == ItemTouchHelper.LEFT) {
                    toggleFavorite(item)
                }
                adapter.notifyItemChanged(position)
                hasVibratedAtLimit = false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView.findViewById<View>(R.id.itemContainer)
                    val limit = viewHolder.itemView.width * 0.3f

                    if (abs(dX) >= limit && !hasVibratedAtLimit && isCurrentlyActive) {
                        vibrateAction()
                        hasVibratedAtLimit = true
                    } else if (abs(dX) < limit) {
                        hasVibratedAtLimit = false
                    }

                    val limitedDX = if (dX > 0) min(dX, limit) else max(dX, -limit)
                    getDefaultUIUtil().onDraw(c, recyclerView, itemView, limitedDX, dY, actionState, isCurrentlyActive)
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                val itemView = viewHolder.itemView.findViewById<View>(R.id.itemContainer)
                getDefaultUIUtil().clearView(itemView)
                hasVibratedAtLimit = false
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rvRecentCodes)
    }

    private fun vibrateAction() {
        context?.let { ctx ->
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterCodes(editTextSearch.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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
        var filteredList = if (currentTab == 1) {
            allCodes.filter { it.isFavorite }
        } else {
            allCodes
        }

        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { it.name.contains(query, ignoreCase = true) }
        }

        adapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            rvRecentCodes.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            tvEmptyMessage.text = if (currentTab == 1) getString(R.string.no_favorites_yet) else getString(R.string.no_codes_yet)
        } else {
            rvRecentCodes.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun toggleFavorite(item: ImageCode) {
        val db = AppDatabase.getDatabase(requireContext())
        item.isFavorite = !item.isFavorite
        viewLifecycleOwner.lifecycleScope.launch {
            db.imageCodeDao().updateImageCode(item)
            val message = if (item.isFavorite) R.string.added_to_favorites else R.string.removed_from_favorites
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showQrDetail(item: ImageCode) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_detail, null)
        val ivDetailQr: ImageView = dialogView.findViewById(R.id.ivDetailQr)
        val tvDetailName: TextView = dialogView.findViewById(R.id.tvDetailName)
        val btnDetailShare: MaterialButton = dialogView.findViewById(R.id.btnDetailShare)
        val btnDetailClose: MaterialButton = dialogView.findViewById(R.id.btnDetailClose)

        tvDetailName.text = item.name
        
        val isScan = item.metaData == "CAMERA" || item.metaData == "GALLERY" || item.metaData == "SCANNED"

        if (isScan) {
            ivDetailQr.visibility = View.GONE
            btnDetailShare.text = getString(R.string.copy)
            btnDetailShare.setIconResource(R.drawable.ic_action_content_copy)
        } else {
            ivDetailQr.visibility = View.VISIBLE
            btnDetailShare.text = getString(R.string.share)
            btnDetailShare.setIconResource(R.drawable.ic_share)
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
            if (isScan) {
                copyToClipboard(item.urlPath)
            } else {
                shareImage(item)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scanned Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.text_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareImage(item: ImageCode) {
        try {
            val fileUri = Uri.parse(item.urlPath)
            val file = if (fileUri.scheme == "file") {
                File(fileUri.path!!)
            } else {
                val fileName = fileUri.lastPathSegment ?: "qr_code.png"
                File(requireContext().filesDir, "codes/$fileName")
            }
            
            if (file.exists()) {
                val contentUri = FileProvider.getUriForFile(requireContext(), "com.hardbug.escanerqr.fileprovider", file)
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code)))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showDeleteConfirmation(item: ImageCode) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_message)
            .setNegativeButton(R.string.delete_negative) { dialog, _ ->
                dialog.dismiss()
                observeCodes()
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
                filterCodes(editTextSearch.text.toString())
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