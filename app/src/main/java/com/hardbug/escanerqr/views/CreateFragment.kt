package com.hardbug.escanerqr.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.hardbug.escanerqr.HomeActivity
import com.hardbug.escanerqr.R

class CreateFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create, container, false)

        val cardQuickQR = view.findViewById<MaterialCardView>(R.id.cardQuickQR)
        val cardAdvanced = view.findViewById<MaterialCardView>(R.id.cardAdvanced)

        cardQuickQR.setOnClickListener {
            (requireActivity() as HomeActivity).replaceFragment(QuickQRFragment(), true)
        }

        cardAdvanced.setOnClickListener {
            (requireActivity() as HomeActivity).replaceFragment(AdvancedCreateFragment(), true)
        }

        return view
    }
}