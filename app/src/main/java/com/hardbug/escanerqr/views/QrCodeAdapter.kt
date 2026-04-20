package com.hardbug.escanerqr.views

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
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
        private val ivFavoriteStatus: ImageView = itemView.findViewById(R.id.ivFavoriteStatus)
        private val itemContainer: MaterialCardView = itemView.findViewById(R.id.itemContainer)
        private val cardImage: MaterialCardView = itemView.findViewById(R.id.cardImage)
        private val tvOriginLegend: TextView = itemView.findViewById(R.id.tvOriginLegend)

        fun bind(item: ImageCode) {
            tvQrName.text = item.name

            if (item.metaData == "CAMERA" || item.metaData == "GALLERY" || item.metaData == "SCANNED") {
                cardImage.visibility = View.GONE
                tvOriginLegend.visibility = View.VISIBLE
                tvOriginLegend.text = when(item.metaData) {
                    "CAMERA" -> itemView.context.getString(R.string.scanned_from_camera)
                    "GALLERY" -> itemView.context.getString(R.string.scanned_from_gallery)
                    else -> itemView.context.getString(R.string.Scan)
                }

                val params = tvQrName.layoutParams as ViewGroup.MarginLayoutParams
                params.marginStart = 0
                tvQrName.layoutParams = params
            } else {
                cardImage.visibility = View.VISIBLE
                tvOriginLegend.visibility = View.GONE
                
                val params = tvQrName.layoutParams as ViewGroup.MarginLayoutParams
                params.marginStart = itemView.context.resources.getDimensionPixelSize(R.dimen.card_margin)
                tvQrName.layoutParams = params

                ivQrCode.setImageDrawable(null)
                try {
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

            if (item.isFavorite) {
                ivFavoriteStatus.visibility = View.VISIBLE
                ivFavoriteStatus.setImageResource(R.drawable.ic_action_favorite)
                
                val favoriteBg = MaterialColors.getColor(itemContainer.context, com.google.android.material.R.attr.colorTertiaryContainer, Color.LTGRAY)
                val favoriteStroke = MaterialColors.getColor(itemContainer.context, com.google.android.material.R.attr.colorTertiary, Color.BLUE)
                
                itemContainer.setCardBackgroundColor(favoriteBg)
                itemContainer.strokeWidth = 4
                itemContainer.strokeColor = favoriteStroke
            } else {
                ivFavoriteStatus.visibility = View.GONE
                
                val defaultBg = MaterialColors.getColor(itemContainer.context, com.google.android.material.R.attr.colorSurface, Color.WHITE)
                val defaultStroke = MaterialColors.getColor(itemContainer.context, com.google.android.material.R.attr.colorOutlineVariant, Color.GRAY)

                itemContainer.setCardBackgroundColor(defaultBg)
                itemContainer.strokeWidth = 2
                itemContainer.strokeColor = defaultStroke
            }

            itemView.setOnClickListener { onItemClick(item) }
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
