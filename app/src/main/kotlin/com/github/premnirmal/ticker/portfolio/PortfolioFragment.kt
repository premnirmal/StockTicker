package com.github.premnirmal.ticker.portfolio

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import com.github.premnirmal.ticker.analytics.ClickEvent
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.ticker.portfolio.StocksAdapter.QuoteClickListener
import com.github.premnirmal.ticker.portfolio.drag_drop.OnStartDragListener
import com.github.premnirmal.ticker.portfolio.drag_drop.SimpleItemTouchHelperCallback
import com.github.premnirmal.ticker.ui.SpacingDecoration
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.fragment_portfolio.stockList
import kotlinx.android.synthetic.main.fragment_portfolio.view_flipper
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class PortfolioFragment : BaseFragment(), ChildFragment, QuoteClickListener, OnStartDragListener {

  interface Parent {
    fun onDragStarted()
    fun onDragEnded()
  }

  companion object {
    private const val LIST_INSTANCE_STATE = "LIST_INSTANCE_STATE"
    private const val KEY_WIDGET_ID = "KEY_WIDGET_ID"

    fun newInstance(widgetId: Int): PortfolioFragment {
      val fragment = PortfolioFragment()
      val args = Bundle()
      args.putInt(KEY_WIDGET_ID, widgetId)
      fragment.arguments = args
      return fragment
    }

    fun newInstance(): PortfolioFragment {
      val fragment = PortfolioFragment()
      val args = Bundle()
      args.putInt(KEY_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
      fragment.arguments = args
      return fragment
    }
  }

  /**
   * Using this injection holder because in unit tests, we use a mockito subclass of this fragment.
   * Without this holder, dagger is unable to inject dependencies into this class.
   */
  class InjectionHolder {

    @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

    init {
      Injector.appComponent.inject(this)
    }
  }

  override val simpleName: String = "PortfolioFragment"
  private lateinit var holder: InjectionHolder
  private val parent: Parent
    get() = parentFragment as Parent
  private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
  private val stocksAdapter by lazy {
    val widgetData = holder.widgetDataProvider.dataForWidgetId(widgetId)
    StocksAdapter(widgetData, this as QuoteClickListener, this as OnStartDragListener)
  }
  private var itemTouchHelper: ItemTouchHelper? = null

  override fun onOpenQuote(
      view: View,
      quote: Quote,
      position: Int
  ) {
    analytics.trackClickEvent(ClickEvent("InstrumentClick"))
    val intent = Intent(view.context, QuoteDetailActivity::class.java)
    intent.putExtra(QuoteDetailActivity.TICKER, quote.symbol)
    startActivity(intent)
  }

  override fun onClickQuoteOptions(
      view: View,
      quote: Quote,
      position: Int
  ) {
    val popupWindow = PopupMenu(view.context, view)
    popupWindow.menuInflater.inflate(R.menu.menu_portfolio, popupWindow.menu)
    popupWindow.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        R.id.remove -> {
          promptRemove(quote)
        }
      }
      true
    }
    popupWindow.show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    holder = InjectionHolder()
    widgetId = requireArguments().getInt(KEY_WIDGET_ID)
  }

  override fun onResume() {
    super.onResume()
    update()
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_portfolio, container, false)
  }

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    stockList.addItemDecoration(
        SpacingDecoration(requireContext().resources.getDimensionPixelSize(R.dimen.list_spacing))
    )
    val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)
    stockList.layoutManager = gridLayoutManager
    stockList.adapter = stocksAdapter
    val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(stocksAdapter)
    itemTouchHelper = ItemTouchHelper(callback)
    itemTouchHelper?.attachToRecyclerView(stockList)

    savedInstanceState?.let { state ->
      val listViewState = state.getParcelable<Parcelable>(LIST_INSTANCE_STATE)
      listViewState?.let { stockList?.layoutManager?.onRestoreInstanceState(it) }
    }
    val widgetData = holder.widgetDataProvider.dataForWidgetId(widgetId)
    if (widgetData.getTickers().isEmpty()) {
      view_flipper.displayedChild = 0
    } else {
      view_flipper.displayedChild = 1
    }
  }

  private fun update() {
    stocksAdapter.refresh()
  }

  private fun promptRemove(quote: Quote?) {
    quote?.let {
      val widgetData = holder.widgetDataProvider.dataForWidgetId(widgetId)
      AlertDialog.Builder(requireContext())
          .setTitle(R.string.remove)
          .setMessage(getString(R.string.remove_prompt, it.symbol))
          .setPositiveButton(R.string.remove) { dialog, _ ->
            widgetData.removeStock(it.symbol)
            stocksAdapter.remove(it)
            holder.widgetDataProvider.broadcastUpdateWidget(widgetId)
            dialog.dismiss()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val listViewState = stockList?.layoutManager?.onSaveInstanceState()
    listViewState?.let {
      outState.putParcelable(LIST_INSTANCE_STATE, it)
    }
  }

  override fun onStartDrag(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
    parent.onDragStarted()
    val widgetData = holder.widgetDataProvider.dataForWidgetId(widgetId)
    if (widgetData.autoSortEnabled()) {
      widgetData.setAutoSort(false)
      update()
      holder.widgetDataProvider.broadcastUpdateWidget(widgetId)
      InAppMessage.showMessage(requireActivity(), R.string.autosort_disabled)
    } else {
      itemTouchHelper?.startDrag(viewHolder)
    }
  }

  override fun onStopDrag() {
    parent.onDragEnded()
  }

  override fun setData(bundle: Bundle) {
  }

  override fun scrollToTop() {
    stockList.smoothScrollToPosition(0)
  }
}