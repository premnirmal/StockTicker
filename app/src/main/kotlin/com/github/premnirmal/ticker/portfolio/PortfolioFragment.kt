package com.github.premnirmal.ticker.portfolio

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.github.premnirmal.ticker.BaseFragment
import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.InAppMessage
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.RxBus
import com.github.premnirmal.ticker.SimpleSubscriber
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.events.NoNetworkEvent
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Stock
import com.github.premnirmal.ticker.portfolio.StocksAdapter.OnStockClickListener
import com.github.premnirmal.ticker.portfolio.drag_drop.RearrangeActivity
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.string
import com.jakewharton.rxbinding.widget.RxPopupMenu
import kotlinx.android.synthetic.main.portfolio_fragment.add_ticker_button
import kotlinx.android.synthetic.main.portfolio_fragment.fragment_root
import kotlinx.android.synthetic.main.portfolio_fragment.stockList
import kotlinx.android.synthetic.main.portfolio_fragment.subtitle
import kotlinx.android.synthetic.main.portfolio_fragment.swipe_container
import kotlinx.android.synthetic.main.portfolio_fragment.toolbar
import rx.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
open class PortfolioFragment : BaseFragment(), OnStockClickListener {

  companion object {
    private val LIST_INSTANCE_STATE = "LIST_INSTANCE_STATE"
    private val NO_NETWORK_THROTTLE_INTERVAL = 1000L
  }

  /**
   * Using this injection holder because in unit tests, we use a mockito subclass of this fragment.
   * Without this holder, dagger is unable to inject depedencies into this class.
   */
  class InjectionHolder {
    @Inject
    lateinit internal var stocksProvider: IStocksProvider

    @Inject
    lateinit internal var bus: RxBus

    init {
      Injector.inject(this)
    }
  }

  private val holder = InjectionHolder()
  private var listViewState: Parcelable? = null
  private var attemptingFetch = false
  private val stocksAdapter by lazy {
    StocksAdapter(holder.stocksProvider, this as OnStockClickListener)
  }

  override fun onClick(view: View, stock: Stock, position: Int) {
    val popupWindow = PopupMenu(view.context, view)
    popupWindow.menuInflater.inflate(R.menu.stock_menu, popupWindow.menu)
    if (stock.isIndex()) {
      popupWindow.menu.findItem(R.id.graph).isEnabled = false
      popupWindow.menu.findItem(R.id.positions).isEnabled = false
    } else {
      popupWindow.menu.findItem(R.id.graph).isEnabled = true
      popupWindow.menu.findItem(R.id.positions).isEnabled = true
    }
    bind(RxPopupMenu.itemClicks(popupWindow))
        .subscribe(object : SimpleSubscriber<MenuItem>() {
          override fun onNext(result: MenuItem) {
            val itemId = result.itemId
            when (itemId) {
              R.id.graph -> {
                val intent = Intent(activity, GraphActivity::class.java)
                intent.putExtra(GraphActivity.GRAPH_DATA, stock)
                activity.startActivity(intent)
              }
              R.id.positions -> {
                val intent = Intent(activity, EditPositionActivity::class.java)
                intent.putExtra(EditPositionActivity.TICKER, stock.symbol)
                activity.startActivity(intent)
              }
              R.id.remove -> {
                promptRemove(stock, position)
              }
            }
          }
        })
    popupWindow.show()
  }

  override fun onResume() {
    super.onResume()
    update()
    if (listViewState != null) {
      stockList?.layoutManager?.onRestoreInstanceState(listViewState)
    }
    val rearrangeItem = toolbar.menu.findItem(R.id.action_rearrange)
    rearrangeItem.isEnabled = true
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.portfolio_fragment, null)
    return view
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      (toolbar.layoutParams as ViewGroup.MarginLayoutParams).topMargin = Tools.getStatusBarHeight(
          activity)
    }
    val gridLayoutManager = GridLayoutManager(context, 2)
    stockList.layoutManager = gridLayoutManager
    stockList.addItemDecoration(
        PortfolioSpacingDecoration(context.resources.getDimensionPixelSize(R.dimen.list_spacing),
            gridLayoutManager))
    stockList.adapter = stocksAdapter
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
      } else if (itemId == R.id.action_rearrange) {
        startActivity(Intent(activity, RearrangeActivity::class.java))
        true
      } else {
        false
      }
    }
    bind(holder.bus.forEventType(NoNetworkEvent::class.java))
        .throttleLast(NO_NETWORK_THROTTLE_INTERVAL, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { event ->
          noNetwork(event as NoNetworkEvent)
          swipe_container.isRefreshing = false
        }

    if (!Tools.isNetworkOnline(context.applicationContext)) {
      noNetwork(NoNetworkEvent())
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
    attemptingFetch = true
    bind(holder.stocksProvider.fetch()).subscribe(object : SimpleSubscriber<List<Stock>>() {
      override fun onError(e: Throwable) {
        attemptingFetch = false
        CrashLogger.logException(e)
        swipe_container.isRefreshing = false
        InAppMessage.showMessage(fragment_root, getString(string.refresh_failed))
      }

      override fun onNext(result: List<Stock>) {
        attemptingFetch = false
        swipe_container.isRefreshing = false
        update()
      }
    })
  }

  internal fun update() {
    if (activity != null) {
      if (holder.stocksProvider.getStocks().isEmpty()) {
        if (!attemptingFetch) {
          fetch()
          return
        }
      }
      if (stockList != null) {
        stocksAdapter.refresh(holder.stocksProvider)
        subtitle.text = "Last Fetch: ${holder.stocksProvider.lastFetched()}"
      }
    }
  }

  internal fun promptRemove(stock: Stock?, position: Int) {
    if (stock != null) {
      AlertDialog.Builder(activity).setTitle("Remove")
          .setMessage("Are you sure you want to remove ${stock.symbol} from your portfolio?")
          .setPositiveButton("Remove", { dialog, which ->
            holder.stocksProvider.removeStock(stock.symbol)
            stocksAdapter.remove(stock)
            dialog.dismiss()
          })
          .setNegativeButton("Cancel", { dialog, which -> dialog.dismiss() })
          .show()
    }
  }

  internal fun noNetwork(event: NoNetworkEvent) {
    InAppMessage.showMessage(fragment_root, getString(R.string.no_network_message))
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    super.onSaveInstanceState(outState)
    listViewState = stockList?.layoutManager?.onSaveInstanceState()
    if (listViewState != null) {
      outState?.putParcelable(LIST_INSTANCE_STATE, listViewState)
    }
  }
}