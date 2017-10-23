package com.soundcloud.android.upsell

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.soundcloud.android.R
import com.soundcloud.android.configuration.FeatureOperations
import com.soundcloud.android.presentation.CellRenderer
import io.reactivex.subjects.PublishSubject

abstract class UpsellItemRenderer<T> internal
constructor(private val featureOperations: FeatureOperations) : CellRenderer<T> {

    private var listener: Listener<T>? = null
    val loadingResult: PublishSubject<UpsellLoadingResult> = PublishSubject.create()

    interface Listener<T> {
        fun onUpsellItemDismissed(position: Int, item: T)
        fun onUpsellItemClicked(context: Context, position: Int, item: T)
        fun onUpsellItemCreated()
    }

    override fun createItemView(parent: ViewGroup): View {
        listener?.onUpsellItemCreated()
        loadingResult.onNext(UpsellLoadingResult.Create)
        return parent
    }

    override fun bindItemView(position: Int, view: View, items: List<T>) {
        bindItemView(position, view, items[position])
    }

    private fun bindItemView(position: Int, view: View, item: T) {
        view.findViewById<TextView>(R.id.title).text = getTitle(view.context)
        view.findViewById<TextView>(R.id.description).text = getDescription(view.context)

        view.isEnabled = false
        view.findViewById<ImageButton>(R.id.close_button).setOnClickListener {
            listener?.onUpsellItemDismissed(position, item)
            loadingResult.onNext(UpsellLoadingResult.Dismiss(position))
        }
        bindActionButton(view, position, item)
    }

    private fun bindActionButton(view: View, position: Int, item: T) {
        val action = view.findViewById<Button>(R.id.action_button)
        setButtonText(view, action)
        action.setOnClickListener {
            listener?.onUpsellItemClicked(view.context, position, item)
            loadingResult.onNext(UpsellLoadingResult.Click(view.context))
        }
    }

    private fun setButtonText(view: View, action: Button) {
        if (featureOperations.isHighTierTrialEligible) {
            action.text = getTrialActionButtonText(view.context, featureOperations.highTierTrialDays)
        } else {
            action.setText(R.string.upsell_upgrade_button)
        }
    }

    fun setListener(listener: Listener<T>) {
        this.listener = listener
    }

    protected abstract fun getTitle(context: Context): String
    protected abstract fun getDescription(context: Context): String
    protected abstract fun getTrialActionButtonText(context: Context, trialDays: Int): String
}

sealed class UpsellLoadingResult {
    data class Dismiss(val position: Int) : UpsellLoadingResult()
    data class Click(val context: Context) : UpsellLoadingResult()
    object Create : UpsellLoadingResult()
}
