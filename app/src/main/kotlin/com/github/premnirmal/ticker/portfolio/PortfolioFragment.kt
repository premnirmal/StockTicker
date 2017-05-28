package com.github.premnirmal.ticker.portfolio

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.CrashLogger
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.components.SimpleSubscriber
import com.github.premnirmal.ticker.events.RefreshEvent
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.StocksAdapter.QuoteClickListener
import com.github.premnirmal.ticker.portfolio.drag_drop.OnStartDragListener
import com.github.premnirmal.ticker.portfolio.drag_drop.SimpleItemTouchHelperCallback
import com.github.premnirmal.ticker.portfolio.search.TickerSelectorActivity
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.ticker.ui.SpacingDecoration
import com.github.premnirmal.tickerwidget.R
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.portfolio_fragment.add_ticker_button
import kotlinx.android.synthetic.main.portfolio_fragment.collapsingToolbarLayout
import kotlinx.android.synthetic.main.portfolio_fragment.fragment_root
import kotlinx.android.synthetic.main.portfolio_fragment.stockList
import kotlinx.android.synthetic.main.portfolio_fragment.subtitle
import kotlinx.android.synthetic.main.portfolio_fragment.swipe_container
import kotlinx.android.synthetic.main.portfolio_fragment.toolbar
import uk.co.chrisjenx.calligraphy.TypefaceUtils
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
open class PortfolioFragment : BaseFragment(), QuoteClickListener, OnStartDragListener {

  companion object {
    private val LIST_INSTANCE_STATE = "LIST_INSTANCE_STATE"
    private val SEQUENTIAL_REQUEST_COUNT = 3
  }

  /**
   * Using this injection holder because in unit tests, we use a mockito subclass of this fragment.
   * Without this holder, dagger is unable to inject dependencies into this class.
   */
  class InjectionHolder {
    @Inject
    lateinit internal var stocksProvider: IStocksProvider

    @Inject
    lateinit internal var bus: RxBus

    @Inject
    lateinit internal var preferences: SharedPreferences

    init {
      Injector.inject(this)
    }
  }

  private val holder = InjectionHolder()
  private var listViewState: Parcelable? = null
  private var attemptingFetch = false
  private var fetchCount = 0
  private val stocksAdapter by lazy {
    StocksAdapter(this as QuoteClickListener, this as OnStartDragListener)
  }
  private var itemTouchHelper: ItemTouchHelper? = null

  override fun onClickQuote(view: View, quote: Quote, position: Int) {
    val popupWindow = PopupMenu(view.context, view)
    popupWindow.menuInflater.inflate(R.menu.stock_menu, popupWindow.menu)
    popupWindow.setOnMenuItemClickListener { menuItem ->
      val itemId = menuItem.itemId
      when (itemId) {
        R.id.positions -> {
          val intent = Intent(activity, EditPositionActivity::class.java)
          intent.putExtra(EditPositionActivity.TICKER, quote.symbol)
          activity.startActivity(intent)
        }
        R.id.remove -> {
          promptRemove(quote, position)
        }
      }
      true
    }
    popupWindow.show()
  }

  override fun onResume() {
    super.onResume()
    bind(holder.bus.forEventType(RefreshEvent::class.java))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { _ ->
          update()
        }
    if (listViewState != null) {
      stockList?.layoutManager?.onRestoreInstanceState(listViewState)
    }
    update()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.portfolio_fragment, container, false)
    return view
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      (toolbar.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
          Tools.getStatusBarHeight(activity)
    }
    collapsingToolbarLayout.setCollapsedTitleTypeface(
        TypefaceUtils.load(activity.assets, "fonts/Ubuntu-Regular.ttf"))
    collapsingToolbarLayout.setExpandedTitleTypeface(
        TypefaceUtils.load(activity.assets, "fonts/Ubuntu-Bold.ttf"))
    stockList.addItemDecoration(
        SpacingDecoration(context.resources.getDimensionPixelSize(R.dimen.list_spacing)))
    val gridLayoutManager = GridLayoutManager(context, 2)
    stockList.layoutManager = gridLayoutManager
    stockList.adapter = stocksAdapter
    val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(stocksAdapter)
    itemTouchHelper = ItemTouchHelper(callback)
    itemTouchHelper?.attachToRecyclerView(stockList)
    swipe_container.setColorSchemeResources(R.color.color_secondary, R.color.spicy_salmon,
        R.color.sea)
    swipe_container.setOnRefreshListener({
      fetch()
    })
    toolbar.inflateMenu(R.menu.menu_paranormal)
    toolbar.setOnMenuItemClickListener { item ->
      val itemId = item.itemId
      if (itemId == R.id.action_settings) {
        val intent = Intent(activity, SettingsActivity::class.java)
        startActivity(intent)
        true
      } else {
        false
      }
    }

    if (savedInstanceState != null) {
      listViewState = savedInstanceState.getParcelable<Parcelable>(LIST_INSTANCE_STATE)
    }

    add_ticker_button.setOnClickListener({ v ->
      val intent = Intent(v.context, TickerSelectorActivity::class.java)
      startActivity(intent)
    })
  }

  internal fun fetch() {
    if (Tools.isNetworkOnline(context)) {
      fetchCount++
      attemptingFetch = true
      bind(holder.stocksProvider.fetch()).subscribe(object : SimpleSubscriber<List<Quote>>() {
        override fun onError(e: Throwable) {
          attemptingFetch = false
          CrashLogger.logException(e)
          swipe_container?.isRefreshing = false
          InAppMessage.showMessage(fragment_root, getString(R.string.refresh_failed))
        }

        override fun onNext(result: List<Quote>) {
          attemptingFetch = false
          swipe_container?.isRefreshing = false
          update()
        }
      })
    } else {
      InAppMessage.showMessage(fragment_root, getString(R.string.no_network_message))
      swipe_container?.isRefreshing = false
    }
  }

  internal fun update() {
    // Don't attempt to make many requests in a row if the stocks don't fetch.
    if (holder.stocksProvider.getStocks().isEmpty() && fetchCount < SEQUENTIAL_REQUEST_COUNT) {
      if (!attemptingFetch) {
        swipe_container?.isRefreshing = true
        fetch()
        return
      }
    }
    fetchCount = 0
    stocksAdapter.refresh(holder.stocksProvider)
    subtitle?.text = getString(R.string.last_fetch, holder.stocksProvider.lastFetched())
  }

  internal fun promptRemove(quote: Quote?, position: Int) {
    if (quote != null) {
      AlertDialog.Builder(activity).setTitle(R.string.remove)
          .setMessage(getString(R.string.remove_prompt, quote.symbol))
          .setPositiveButton(R.string.remove, { dialog, _ ->
            holder.stocksProvider.removeStock(quote.symbol)
            stocksAdapter.remove(quote)
            dialog.dismiss()
          })
          .setNegativeButton(R.string.cancel, { dialog, _ -> dialog.dismiss() })
          .show()
    }
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    super.onSaveInstanceState(outState)
    listViewState = stockList?.layoutManager?.onSaveInstanceState()
    if (listViewState != null) {
      outState?.putParcelable(LIST_INSTANCE_STATE, listViewState)
    }
  }

  override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
    if (Tools.autoSortEnabled()) {
      Tools.enableAutosort(false)
      update()
      InAppMessage.showMessage(fragment_root, getString(R.string.autosort_disabled))
    } else {
      itemTouchHelper?.startDrag(viewHolder)
    }
  }
}