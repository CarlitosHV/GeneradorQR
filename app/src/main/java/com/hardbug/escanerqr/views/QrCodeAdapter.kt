package com.hardbug.escanerqr.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.models.ImageCode

class QrCodeAdapter(
    private val onDeleteClick: (ImageCode) -> Unit,
    private val onItemClick: (ImageCode) -> Unit
) : ListAdapter<ImageCode, QrCodeAdapter.QrViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QrViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_qr_code, parent, false)
        return QrViewHolder(view)
    }

    override fun onBindViewHolder(holder: QrViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QrViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivQrCode: ImageView = itemView.findViewById(R.id.ivQrCode)
        private val tvQrName: TextView = itemView.findViewById(R.id.tvQrName)
        private val btnDelete: View = itemView.findViewById(R.id.btnDelete)

        fun bind(item: ImageCode) {
            tvQrName.text = item.name

            if (item.metaData == "SCANNED") {
                ivQrCode.setImageResource(R.drawable.ic_barcode)
                ivQrCode.setPadding(12, 12, 12, 12)
                ivQrCode.alpha = 0.7f
            } else {
                ivQrCode.setPadding(0, 0, 0, 0)
                ivQrCode.alpha = 1.0f
                try {
                    ivQrCode.setImageDrawable(null)
                    val uri = item.urlPath.toUri()
                    if (uri.scheme == "file") {
                        ivQrCode.setImageURI(uri)
                    } else {
                        ivQrCode.setImageResource(R.drawable.baseline_qr_code_24)
                    }
                } catch (e: Exception) {
                    ivQrCode.setImageResource(R.drawable.baseline_qr_code_24)
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ImageCode>() {
        override fun areItemsTheSame(oldItem: ImageCode, newItem: ImageCode): Boolean {
            return oldItem.imageCodeUuid == newItem.imageCodeUuid
        }

        override fun areContentsTheSame(oldItem: ImageCode, newItem: ImageCode): Boolean {
            return oldItem == newItem
        }
    }
}