package com.github.premnirmal.ticker.portfolio

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.support.design.widget.CoordinatorLayout
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
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.ticker.portfolio.drag_drop.RearrangeActivity
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.ticker.ui.SpacingDecoration
import com.github.premnirmal.tickerwidget.R
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
open class PortfolioFragment : BaseFragment() {

  companion object {
    private val LIST_INSTANCE_STATE = "LIST_INSTANCE_STATE"
    private val NO_NETWORK_THROTTLE_INTERVAL = 1000L
  }

  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  @Inject
  lateinit internal var bus: RxBus

  private val handler = Handler(Looper.getMainLooper())
  private var listViewState: Parcelable? = null

  private val stocksAdapter by lazy {
    StocksAdapter(stocksProvider,
        object : StocksAdapter.OnStockClickListener {
          override fun onClick(view: View, stock: Stock, position: Int) {
            val popupWindow = PopupMenu(view.context, view)
            popupWindow.menuInflater.inflate(R.menu.stock_menu, popupWindow.menu)
            if (stock.isIndex) {
              popupWindow.menu.findItem(R.id.graph).isEnabled = false
              popupWindow.menu.findItem(R.id.positions).isEnabled = false
            } else {
              popupWindow.menu.findItem(R.id.graph).isEnabled = true
              popupWindow.menu.findItem(R.id.positions).isEnabled = true
            }
            bind(RxPopupMenu.itemClicks(popupWindow)).subscribe(
                object : SimpleSubscriber<MenuItem>() {
                  override fun onNext(menuItem: MenuItem) {
                    val itemId = menuItem.itemId
                    when (itemId) {
                      R.id.graph -> {
                        val intent = Intent(activity, GraphActivity::class.java)
                        intent.putExtra(GraphActivity.GRAPH_DATA, stock)
                        activity.startActivity(intent)
                      }
                      R.id.positions -> {
                        val intent = Intent(activity, EditPositionActivity::class.java)
                        intent.putExtra(EditPositionActivity.TICKER, stock?.symbol)
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
        })
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.inject(this)
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
      (toolbar.layoutParams as ViewGroup.MarginLayoutParams).topMargin = Tools.getStatusBarHeight()
    }
    stockList.addItemDecoration(
        SpacingDecoration(context.resources.getDimensionPixelSize(R.dimen.list_spacing)))
    stockList.layoutManager = GridLayoutManager(context, 2)
    swipe_container.setColorSchemeResources(R.color.color_secondary, R.color.spicy_salmon,
        R.color.sea)
    swipe_container.setOnRefreshListener({
      bind(stocksProvider.fetch()).subscribe(object : SimpleSubscriber<List<Stock>>() {
        override fun onError(e: Throwable) {
          CrashLogger.logException(e)
          swipe_container.isRefreshing = false
          InAppMessage.showMessage(fragment_root, getString(R.string.refresh_failed))
        }

        override fun onNext(t: List<Stock>) {
          swipe_container.isRefreshing = false
          update()
        }
      })
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
    bind(bus.forEventType(NoNetworkEvent::class.java))
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

    val params = add_ticker_button.layoutParams as CoordinatorLayout.LayoutParams
    params.behavior = FABBehaviour()
    add_ticker_button.layoutParams = params
    add_ticker_button.setOnClickListener({ v ->
      val intent = Intent(v.context, TickerSelectorActivity::class.java)
      startActivity(intent)
    })
  }

  internal fun update() {
    val activity = activity
    if (activity != null) {
      if (stocksProvider.getStocks().isEmpty()) {
        handler.postDelayed({ update() }, 600)
      }

      if (stockList != null) {
        stocksAdapter.refresh(stocksProvider)
        stockList.adapter = stocksAdapter
        subtitle.text = "Last Fetch: ${stocksProvider.lastFetched()}"
      }
    }
  }

  internal fun promptRemove(stock: Stock?, position: Int) {
    if (stock != null) {
      AlertDialog.Builder(activity).setTitle("Remove")
          .setMessage("Are you sure you want to remove ${stock.symbol} from your portfolio?")
          .setPositiveButton("Remove", { dialog, which ->
            stocksProvider.removeStock(stock.symbol)
            val index = stocksAdapter.remove(stock)
            if (index >= 0) {
              stocksAdapter.notifyItemRemoved(index)
            }
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